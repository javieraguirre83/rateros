package rateros.entidad;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import rateros.core.Ajustes;
import rateros.core.Botin;

/**
 * Breaking through weak blocks (doors, wood, glass...) to reach a place: a chest or the player.
 * Slow and noisy, so you hear them coming. Stone, bricks, obsidian and iron doors stop them.
 */
public class Rompedor {
    /** Mark on the drops of broken blocks, so bandits do not pick up your walls. */
    public static final String NO_TOMAR = "raterosEscombro";

    private final Ratero ratero;
    private BlockPos rompiendo = null;
    private int ticks = 0;

    public Rompedor(Ratero ratero) {
        this.ratero = ratero;
    }

    public boolean rompiendo() {
        return rompiendo != null;
    }

    /** Finds what is in the way to "destino" and starts breaking it. False if the way is blocked by something unbreakable. */
    public boolean intentar(Vec3 destino, BlockPos ignorar) {
        BlockPos estorbo = buscarEstorbo(destino, ignorar);
        if (estorbo == null || !puedeRomper(estorbo)) return false;
        rompiendo = estorbo;
        ticks = 0;
        return true;
    }

    public void cancelar() {
        if (rompiendo != null) ratero.level().destroyBlockProgress(ratero.getId(), rompiendo, -1);
        rompiendo = null;
    }

    /** The first solid block on the way to the destination (looking from the feet and from the eyes). */
    private BlockPos buscarEstorbo(Vec3 fin, BlockPos ignorar) {
        BlockPos mejor = null;
        double mejorD = Double.MAX_VALUE;
        for (Vec3 inicio : new Vec3[]{ratero.position().add(0, 0.5, 0), ratero.getEyePosition()}) {
            Vec3 dir = fin.subtract(inicio);
            double largo = dir.length();
            if (largo < 0.01) continue;
            dir = dir.scale(1.0 / largo);
            for (double t = 0.4; t < Math.min(largo, 4.0); t += 0.3) {
                BlockPos pos = BlockPos.containing(inicio.add(dir.scale(t)));
                if (pos.equals(ignorar)) break;
                BlockState state = ratero.level().getBlockState(pos);
                if (state.getCollisionShape(ratero.level(), pos).isEmpty()) continue;
                if (t < mejorD) {
                    mejorD = t;
                    mejor = pos;
                }
                break;
            }
        }
        return mejor;
    }

    public boolean puedeRomper(BlockPos pos) {
        if (!Ajustes.romperBloques || !(ratero.level() instanceof ServerLevel level)) return false;
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) return false;
        if (level.getBlockEntity(pos) != null) return false;   // never chests, beds, signs...
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0) return false; // bedrock and such
        return Botin.bloqueEnLista(Ajustes.bloquesRompibles, state);
    }

    /** One tick of breaking. Returns true while still busy. */
    public boolean tick() {
        if (rompiendo == null || !(ratero.level() instanceof ServerLevel level)) return false;
        BlockState state = level.getBlockState(rompiendo);
        if (state.getCollisionShape(level, rompiendo).isEmpty() || !puedeRomper(rompiendo)) {
            cancelar();
            return false;
        }
        ratero.getNavigation().stop();
        ratero.getLookControl().setLookAt(Vec3.atCenterOf(rompiendo));
        int total = Math.max(10, Ajustes.segundosRomper * 20 / (ratero.getRol() == Rol.FORZADOR ? 2 : 1));
        ticks++;
        if (ticks % 10 == 0) {
            ratero.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, rompiendo, state.getSoundType().getHitSound(), SoundSource.HOSTILE, 1.0F, 0.8F);
            level.destroyBlockProgress(ratero.getId(), rompiendo, Math.min(9, ticks * 10 / total));
        }
        if (ticks >= total) {
            BlockPos pos = rompiendo;
            level.destroyBlockProgress(ratero.getId(), pos, -1);
            // The block drops as usual (you can pick it up), but bandits will not take it
            for (ItemStack drop : Block.getDrops(state, level, pos, null)) {
                ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                item.setDefaultPickUpDelay();
                item.getPersistentData().putBoolean(NO_TOMAR, true);
                level.addFreshEntity(item);
            }
            level.destroyBlock(pos, false, ratero, 512);
            rompiendo = null;
            return false;
        }
        return true;
    }
}
