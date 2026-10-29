package rateros.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Which gang level each player has unlocked. A level opens for good the first time the player has one of
 * its materials (ingots, blocks, gems) in the inventory, the ender chest or the containers around them.
 */
public final class Niveles {
    private static final String CLAVE = "raterosNivel";

    /** The unlocked level (0 = nothing valuable yet). */
    public static int nivel(ServerPlayer player) {
        int n = player.getPersistentData().getIntOr(CLAVE, 0);
        return Math.max(0, Math.min(n, Ajustes.niveles.size() - 1));
    }

    public static Ajustes.Nivel datos(ServerPlayer player) {
        return Ajustes.niveles.get(nivel(player));
    }

    /** Looks at what the player has now and unlocks higher levels (never lowers). */
    public static void actualizar(ServerPlayer player) {
        int actual = player.getPersistentData().getIntOr(CLAVE, 0);
        int maximo = Ajustes.niveles.size() - 1;
        if (actual >= maximo) return;
        int visto = mejorEn(player.getInventory(), actual);
        visto = Math.max(visto, mejorEn(player.getEnderChestInventory(), visto));
        if (visto < maximo && player.level() instanceof ServerLevel level) {
            visto = Math.max(visto, mejorAlrededor(level, player.blockPosition(), visto));
        }
        if (visto > actual) {
            player.getPersistentData().putInt(CLAVE, visto);
            Rateros.LOGGER.info("Rateros: {} desbloqueo el nivel {} ({})", player.getName().getString(), visto, Ajustes.niveles.get(visto).nombre());
        }
    }

    /** Level of the best material in a container, or "desde" if nothing better. */
    private static int mejorEn(Container c, int desde) {
        int mejor = desde;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty()) continue;
            mejor = Math.max(mejor, nivelDe(s, mejor));
        }
        return mejor;
    }

    /** Level an item unlocks (only checks levels above "desde"). */
    private static int nivelDe(ItemStack s, int desde) {
        for (int n = Ajustes.niveles.size() - 1; n > desde; n--) {
            if (Botin.enLista(Ajustes.niveles.get(n).materiales(), s)) return n;
        }
        return desde;
    }

    /** Containers within the sniffing radius (not untouched structure loot). */
    private static int mejorAlrededor(ServerLevel level, BlockPos centro, int desde) {
        int radio = Ajustes.radioOlfato;
        int mejor = desde;
        int cx0 = (centro.getX() - radio) >> 4, cx1 = (centro.getX() + radio) >> 4;
        int cz0 = (centro.getZ() - radio) >> 4, cz1 = (centro.getZ() + radio) >> 4;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof Container c)) continue;
                    if (be instanceof RandomizableContainer rc && rc.getLootTable() != null) continue;
                    if (be.getBlockPos().distSqr(centro) > (double) radio * radio) continue;
                    mejor = Math.max(mejor, mejorEn(c, mejor));
                }
            }
        }
        return mejor;
    }

    private Niveles() {}
}
