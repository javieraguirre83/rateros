package rateros.core;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import rateros.entidad.Ratero;
import rateros.entidad.Rol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** When gangs arrive, how big they are, which roles and which weapons (by the level the player unlocked). */
public final class Bandas {
    private static final String ULTIMA = "raterosUltimaBanda";
    /** How often (ticks) each player is checked. */
    private static final int CADA = 600;

    public static long dia(Level level) {
        return level.getOverworldClockTime() / 24000L;
    }

    /** Chance of a gang per Minecraft day for this world day (grows from the start value to the advanced one). */
    public static double probPorDia(long dia) {
        double t = Math.min(1.0, Math.max(0.0, dia / (double) Ajustes.diaAvanzado));
        return Ajustes.probInicio + (Ajustes.probAvanzada - Ajustes.probInicio) * t;
    }

    /** Called every tick for each player (server side). */
    public static void revisar(ServerPlayer player) {
        if (player.tickCount % CADA != 0 || player.isSpectator()) return;
        // What you have unlocked is tracked even with raids off (it is for good)
        Niveles.actualizar(player);
        if (!Ajustes.activado) return;
        ServerLevel level = player.level();
        if (level.dimension() != Level.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL) return;
        // One gang at a time around each player
        if (!level.getEntitiesOfClass(Ratero.class, player.getBoundingBox().inflate(128)).isEmpty()) return;
        long ahora = level.getGameTime();
        long ultima = player.getPersistentData().getLongOr(ULTIMA, -1L);
        if (ultima >= 0 && ahora - ultima < 6000) return;

        boolean noche = level.isDarkOutside();
        double porDia = probPorDia(dia(level));
        double revisionesPorDia = 24000.0 / CADA;
        double prob = switch (Ajustes.horario) {
            case "noche" -> noche ? porDia / (revisionesPorDia / 2) : 0;
            case "dia" -> noche ? 0 : porDia / (revisionesPorDia / 2);
            default -> {
                // Same daily average, but "multiplicadorNoche" times likelier at night
                double base = porDia / (revisionesPorDia / 2 * (1 + Ajustes.multiplicadorNoche));
                yield noche ? base * Ajustes.multiplicadorNoche : base;
            }
        };
        if (level.getRandom().nextDouble() < prob) invocar(player, 0);
    }

    /** Keeps the per-player data when Minecraft creates the new player after death. */
    public static void copiar(Player de, Player a) {
        CompoundTag viejo = de.getPersistentData(), nuevo = a.getPersistentData();
        for (String k : new String[]{ULTIMA, "raterosNivel"}) {
            if (viejo.contains(k)) nuevo.put(k, viejo.get(k).copy());
        }
    }

    /** Size of the gang for a level. */
    public static int tamano(Ajustes.Nivel nivel, RandomSource r) {
        int[] rg = Ajustes.rango(nivel.tamano(), 2, 2);
        return Math.min(Ajustes.maxBanda, rg[0] + r.nextInt(rg[1] - rg[0] + 1));
    }

    /** Roles of a gang: maybe a boss, maybe a forcer, the crossbowmen of the level, the rest thieves. */
    public static List<Rol> roles(int n, Ajustes.Nivel nivel, RandomSource r) {
        List<Rol> roles = new ArrayList<>();
        if (Ajustes.rolJefe && r.nextInt(100) < nivel.jefe() && roles.size() < n) roles.add(Rol.JEFE);
        if (Ajustes.rolForzador && nivel.forzador() && roles.size() < n) roles.add(Rol.FORZADOR);
        if (Ajustes.rolBallestero) {
            for (int i = 0; i < nivel.ballesteros() && roles.size() < n; i++) roles.add(Rol.BALLESTERO);
        }
        Rol relleno = Ajustes.rolLadron ? Rol.LADRON : Ajustes.rolBallestero ? Rol.BALLESTERO : Ajustes.rolForzador ? Rol.FORZADOR : Rol.JEFE;
        while (roles.size() < n) roles.add(relleno);
        Collections.shuffle(roles, new java.util.Random(r.nextLong()));
        return roles;
    }

    /** Makes a gang arrive near the player now. size 0 = by the unlocked level. Returns how many came. */
    public static int invocar(ServerPlayer player, int size) {
        ServerLevel level = player.level();
        RandomSource r = level.getRandom();
        Niveles.actualizar(player);
        int indice = Niveles.nivel(player);
        // Now and then a gang of a lower level comes instead (any of them, equally likely)
        if (indice > 0 && r.nextInt(100) < Ajustes.probMenor) indice = r.nextInt(indice);
        Ajustes.Nivel nivel = Ajustes.niveles.get(indice);
        int n = size > 0 ? Math.min(size, 32) : tamano(nivel, r);
        BlockPos donde = lugar(level, player.blockPosition(), r);
        if (donde == null) return 0;
        int llegaron = 0;
        for (Rol rol : roles(n, nivel, r)) {
            Ratero ratero = Rateros.RATERO.get().create(level, EntitySpawnReason.EVENT);
            if (ratero == null) continue;
            // Spread them a little around the spot, each one on valid ground
            BlockPos p = donde;
            for (int k = 0; k < 6; k++) {
                BlockPos q = superficie(level, donde.getX() + r.nextInt(5) - 2, donde.getZ() + r.nextInt(5) - 2);
                if (sePuedeParar(level, q)) {
                    p = q;
                    break;
                }
            }
            ratero.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360F, 0F);
            ratero.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.EVENT, null);
            ratero.setPatrolLeader(false);
            ratero.setCanJoinRaid(false);
            ratero.setRol(rol);
            equipar(ratero, rol, nivel, r);
            level.addFreshEntity(ratero);
            llegaron++;
        }
        if (llegaron > 0) {
            player.getPersistentData().putLong(ULTIMA, level.getGameTime());
            if (Ajustes.aviso) {
                level.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.5F, 1.2F);
                player.sendSystemMessage(Component.translatable("rateros.aviso", llegaron).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
        }
        return llegaron;
    }

    /**
     * Can a bandit stand here? The two blocks of its body must be passable (air, grass, flowers, snow layer...)
     * and without water or lava, and the block below must be solid ground.
     */
    public static boolean sePuedeParar(ServerLevel level, BlockPos p) {
        for (BlockPos cuerpo : new BlockPos[]{p, p.above()}) {
            if (!level.getBlockState(cuerpo).getCollisionShape(level, cuerpo).isEmpty()) return false;
            if (!level.getFluidState(cuerpo).isEmpty()) return false;
        }
        BlockPos suelo = p.below();
        return !level.getBlockState(suelo).getCollisionShape(level, suelo).isEmpty() && level.getFluidState(suelo).isEmpty();
    }

    /** Surface position at x, z (on top of the ground, ignoring leaves). */
    private static BlockPos superficie(ServerLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** An open spot 24-40 blocks away, on dry ground, not too far above or below you. */
    private static BlockPos lugar(ServerLevel level, BlockPos cerca, RandomSource r) {
        for (int intento = 0; intento < 32; intento++) {
            double ang = r.nextDouble() * Math.PI * 2;
            int d = 24 + r.nextInt(17);
            int x = cerca.getX() + (int) (Math.cos(ang) * d);
            int z = cerca.getZ() + (int) (Math.sin(ang) * d);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            BlockPos p = superficie(level, x, z);
            if (Math.abs(p.getY() - cerca.getY()) > 24) continue;
            if (sePuedeParar(level, p)) return p;
        }
        return null;
    }

    // ---------- weapons and armor, by the unlocked level (never enchanted) ----------

    private static Item espada(String material) {
        return switch (material) {
            case "madera" -> Items.WOODEN_SWORD;
            case "hierro" -> Items.IRON_SWORD;
            case "diamante" -> Items.DIAMOND_SWORD;
            case "netherita" -> Items.NETHERITE_SWORD;
            default -> Items.STONE_SWORD;
        };
    }

    private static Item hacha(String material) {
        return switch (material) {
            case "madera" -> Items.WOODEN_AXE;
            case "hierro" -> Items.IRON_AXE;
            case "diamante" -> Items.DIAMOND_AXE;
            case "netherita" -> Items.NETHERITE_AXE;
            default -> Items.STONE_AXE;
        };
    }

    private static Item peto(String material) {
        return switch (material) {
            case "diamante" -> Items.DIAMOND_CHESTPLATE;
            case "netherita" -> Items.NETHERITE_CHESTPLATE;
            default -> Items.IRON_CHESTPLATE;
        };
    }

    public static void equipar(Ratero ratero, Rol rol, Ajustes.Nivel nivel, RandomSource r) {
        // The boss is twice as likely to carry the better weapon
        int prob = rol == Rol.JEFE ? Math.min(100, nivel.probMejor() * 2) : nivel.probMejor();
        String material = r.nextInt(100) < prob ? nivel.armaMejor() : nivel.armaBase();
        ItemStack arma = new ItemStack(switch (rol) {
            case BALLESTERO -> Items.CROSSBOW;
            case FORZADOR -> hacha(material);
            default -> espada(material);
        });
        ratero.setItemSlot(EquipmentSlot.MAINHAND, arma);
        boolean caro = material.equals("diamante") || material.equals("netherita");
        ratero.setDropChance(EquipmentSlot.MAINHAND, caro ? 0.02F : 0.085F);
        if (rol == Rol.JEFE) {
            AttributeInstance vida = ratero.getAttribute(Attributes.MAX_HEALTH);
            if (vida != null) {
                vida.addPermanentModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(Rateros.MODID, "jefe_vida"), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                ratero.setHealth(ratero.getMaxHealth());
            }
            ratero.setItemSlot(EquipmentSlot.CHEST, new ItemStack(peto(nivel.armaBase())));
            ratero.setDropChance(EquipmentSlot.CHEST, 0.0F);
        }
    }

    /** All bandits around a player. */
    public static List<Ratero> alrededor(ServerPlayer player, double radio) {
        return player.level().getEntitiesOfClass(Ratero.class, new AABB(player.blockPosition()).inflate(radio));
    }

    private Bandas() {}
}
