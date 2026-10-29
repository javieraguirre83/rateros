package rateros.entidad;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import rateros.core.Ajustes;
import rateros.core.Botin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sniff out the container with the best "worked" goods, walk there (breaking weak blocks in the way),
 * open it and take stacks one by one: the higher the role, the likelier it goes for the valuables.
 */
public class RobarGoal extends Goal {
    private static final TagKey<Structure> ALDEA = TagKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace("village"));

    private final Ratero ratero;
    private BlockPos objetivo;
    private final Set<BlockPos> descartados = new HashSet<>();
    private int esperaBusqueda = 0;
    private int repath = 0;
    private int ticksSinAvance = 0;
    private double mejorDistancia = Double.MAX_VALUE;
    private BlockPos rompiendo = null;
    private int ticksRompiendo = 0;
    private int esperaToma = 0;

    public RobarGoal(Ratero ratero) {
        this.ratero = ratero;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ---------- choosing a target ----------

    @Override
    public boolean canUse() {
        if (ratero.isHuyendo() || !ratero.puedeCargar()) return false;
        if (ratero.getTarget() != null && ratero.getRol().pelea()) return false;
        if (--esperaBusqueda > 0) return false;
        esperaBusqueda = 60 + ratero.getRandom().nextInt(40);
        objetivo = buscar();
        return objetivo != null;
    }

    private static boolean esRobable(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity
                || be instanceof ShulkerBoxBlockEntity || be instanceof AbstractFurnaceBlockEntity;
    }

    private BlockPos buscar() {
        if (!(ratero.level() instanceof ServerLevel level)) return null;
        int radio = Ajustes.radioOlfato;
        BlockPos centro = ratero.blockPosition();
        BlockPos mejor = null;
        double mejorPuntaje = 0;
        int cx0 = (centro.getX() - radio) >> 4, cx1 = (centro.getX() + radio) >> 4;
        int cz0 = (centro.getZ() - radio) >> 4, cz1 = (centro.getZ() + radio) >> 4;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!esRobable(be) || !(be instanceof Container c)) continue;
                    BlockPos pos = be.getBlockPos();
                    if (descartados.contains(pos)) continue;
                    double dist = Math.sqrt(pos.distSqr(centro));
                    if (dist > radio) continue;
                    // Fresh structure loot is not "worked" goods
                    if (be instanceof RandomizableContainer rc && rc.getLootTable() != null) continue;
                    if (!Ajustes.asaltarAldeas && level.structureManager().getStructureWithPieceAt(pos, ALDEA).isValid()) continue;
                    int p = Botin.puntaje(c);
                    if (p <= 0) continue;
                    double puntaje = p / (1.0 + dist / 16.0);
                    if (puntaje > mejorPuntaje) {
                        mejorPuntaje = puntaje;
                        mejor = pos;
                    }
                }
            }
        }
        return mejor;
    }

    // ---------- going there ----------

    @Override
    public void start() {
        repath = 0;
        ticksSinAvance = 0;
        mejorDistancia = Double.MAX_VALUE;
        rompiendo = null;
        esperaToma = 20;
    }

    @Override
    public boolean canContinueToUse() {
        if (objetivo == null || ratero.isHuyendo() || !ratero.puedeCargar()) return false;
        if (ratero.getTarget() != null && ratero.getRol().pelea()) return false;
        return ratero.level().getBlockEntity(objetivo) instanceof Container;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        cerrar();
        if (rompiendo != null) ratero.level().destroyBlockProgress(ratero.getId(), rompiendo, -1);
        rompiendo = null;
        ratero.getNavigation().stop();
        objetivo = null;
    }

    private Vec3 centro() {
        return Vec3.atCenterOf(objetivo);
    }

    private boolean alAlcance() {
        return ratero.distanceToSqr(centro()) < 3.2 * 3.2;
    }

    @Override
    public void tick() {
        if (objetivo == null) return;
        ratero.getLookControl().setLookAt(centro());
        if (alAlcance()) {
            ratero.getNavigation().stop();
            robando();
            return;
        }
        cerrar();
        if (rompiendo != null) {
            romperTick();
            return;
        }
        double d = ratero.distanceToSqr(centro());
        if (d < mejorDistancia - 0.25) {
            mejorDistancia = d;
            ticksSinAvance = 0;
        } else {
            ticksSinAvance++;
        }
        boolean atorado = ticksSinAvance > 40 || ratero.getNavigation().isDone() && ticksSinAvance > 10;
        if (atorado) {
            BlockPos estorbo = buscarEstorbo();
            if (estorbo != null && puedeRomper(estorbo)) {
                rompiendo = estorbo;
                ticksRompiendo = 0;
            } else {
                // A wall they cannot break: forget this container and look for another
                descartados.add(objetivo);
                objetivo = null;
                esperaBusqueda = 0;
            }
            ticksSinAvance = 0;
            mejorDistancia = Double.MAX_VALUE;
            return;
        }
        if (--repath <= 0) {
            repath = 20;
            Path path = ratero.getNavigation().createPath(objetivo, 1);
            ratero.getNavigation().moveTo(path, 1.0);
        }
    }

    // ---------- breaking in ----------

    /** The first solid block between the bandit and the container (at feet and head height). */
    private BlockPos buscarEstorbo() {
        BlockPos mejor = null;
        double mejorD = Double.MAX_VALUE;
        Vec3 fin = centro();
        for (Vec3 inicio : new Vec3[]{ratero.position().add(0, 0.5, 0), ratero.getEyePosition()}) {
            Vec3 dir = fin.subtract(inicio);
            double largo = dir.length();
            if (largo < 0.01) continue;
            dir = dir.scale(1.0 / largo);
            for (double t = 0.4; t < Math.min(largo, 4.0); t += 0.3) {
                BlockPos pos = BlockPos.containing(inicio.add(dir.scale(t)));
                if (pos.equals(objetivo)) break;
                BlockState state = ratero.level().getBlockState(pos);
                if (state.isAir() || state.getCollisionShape(ratero.level(), pos).isEmpty()) continue;
                if (t < mejorD) {
                    mejorD = t;
                    mejor = pos;
                }
                break;
            }
        }
        return mejor;
    }

    private boolean puedeRomper(BlockPos pos) {
        if (!Ajustes.romperBloques || !(ratero.level() instanceof ServerLevel level)) return false;
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) return false;
        BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) != null) return false;
        return Botin.bloqueEnLista(Ajustes.bloquesRompibles, state);
    }

    private void romperTick() {
        BlockState state = ratero.level().getBlockState(rompiendo);
        if (state.isAir() || !puedeRomper(rompiendo)) {
            ratero.level().destroyBlockProgress(ratero.getId(), rompiendo, -1);
            rompiendo = null;
            return;
        }
        ratero.getLookControl().setLookAt(Vec3.atCenterOf(rompiendo));
        int total = Math.max(10, Ajustes.segundosRomper * 20 / (ratero.getRol() == Rol.FORZADOR ? 2 : 1));
        ticksRompiendo++;
        if (ticksRompiendo % 10 == 0) {
            ratero.swing(InteractionHand.MAIN_HAND);
            ratero.level().playSound(null, rompiendo, state.getSoundType().getHitSound(), SoundSource.HOSTILE, 1.0F, 0.8F);
            ratero.level().destroyBlockProgress(ratero.getId(), rompiendo, Math.min(9, ticksRompiendo * 10 / total));
        }
        if (ticksRompiendo >= total) {
            ratero.level().destroyBlockProgress(ratero.getId(), rompiendo, -1);
            ratero.level().destroyBlock(rompiendo, true, ratero, 512);
            rompiendo = null;
            repath = 0;
        }
    }

    // ---------- stealing ----------

    private void abrir(Container c) {
        if (ratero.abiertoEn != null) return;
        ratero.abiertoEn = objetivo;
        c.startOpen(ratero);
        esperaToma = 20;
    }

    private void cerrar() {
        if (ratero.abiertoEn == null) return;
        BlockPos pos = ratero.abiertoEn;
        ratero.abiertoEn = null;
        if (ratero.level().getBlockEntity(pos) instanceof Container c) c.stopOpen(ratero);
    }

    private void robando() {
        if (!(ratero.level().getBlockEntity(objetivo) instanceof Container c)) {
            objetivo = null;
            return;
        }
        abrir(c);
        if (--esperaToma > 0) return;
        esperaToma = 12;
        if (!tomarUno(c)) {
            // Nothing else worth taking here: close and look for the next container
            cerrar();
            descartados.add(objetivo);
            objetivo = null;
            esperaBusqueda = 0;
        }
    }

    /** Takes one stack: with the role's chance it goes for a valuable one, otherwise any worked one. */
    private boolean tomarUno(Container c) {
        if (!ratero.puedeCargar()) return false;
        List<Integer> utiles = new ArrayList<>();
        List<Integer> valiosos = new ArrayList<>();
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (!Botin.interesa(s)) continue;
            utiles.add(i);
            if (Botin.valioso(s)) valiosos.add(i);
        }
        if (utiles.isEmpty()) return false;
        boolean vaPorLoBueno = !valiosos.isEmpty() && ratero.getRandom().nextDouble() < ratero.getRol().probValioso();
        List<Integer> deDonde = vaPorLoBueno ? valiosos : utiles;
        int slot = deDonde.get(ratero.getRandom().nextInt(deDonde.size()));
        ItemStack s = c.getItem(slot);
        if (!ratero.botin().canAddItem(s)) return false;
        ItemStack tomado = c.removeItem(slot, s.getCount());
        c.setChanged();
        if (!ratero.guardar(tomado)) {
            c.setItem(slot, tomado);
            return false;
        }
        ratero.swing(InteractionHand.OFF_HAND);
        ratero.level().playSound(null, objetivo, SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 0.6F, 0.9F + ratero.getRandom().nextFloat() * 0.3F);
        return true;
    }

    /** Forget emptied containers from time to time (they may be refilled). */
    public void olvidar() {
        descartados.clear();
    }
}
