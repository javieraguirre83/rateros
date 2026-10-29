package rateros.core;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import rateros.entidad.Ratero;
import rateros.entidad.Rol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** When gangs arrive, how big they are, which roles and which weapons. */
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
        if (!Ajustes.activado || player.tickCount % CADA != 0 || player.isSpectator()) return;
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

    /** Size of the gang for this world day. */
    public static int tamano(long dia, RandomSource r) {
        String t = dia >= Ajustes.diaGrande ? Ajustes.tamAvanzado : dia >= Ajustes.diaMedio ? Ajustes.tamMedio : Ajustes.tamInicio;
        int[] rg = Ajustes.rango(t, 2, 3);
        return Math.min(Ajustes.maxBanda, rg[0] + r.nextInt(rg[1] - rg[0] + 1));
    }

    /** Roles of a gang of this size: a boss in big late gangs, a forcer, crossbowmen, the rest thieves. */
    public static List<Rol> roles(int n, long dia, RandomSource r) {
        List<Rol> roles = new ArrayList<>();
        if (Ajustes.rolJefe && n >= 5 && dia >= Ajustes.diaGrande) roles.add(Rol.JEFE);
        if (Ajustes.rolForzador && n >= 3 && roles.size() < n && r.nextBoolean()) roles.add(Rol.FORZADOR);
        if (Ajustes.rolBallestero && n >= 2) {
            int ballesteros = Math.max(1, Math.min(2, n / 3));
            for (int i = 0; i < ballesteros && roles.size() < n; i++) roles.add(Rol.BALLESTERO);
        }
        Rol relleno = Ajustes.rolLadron ? Rol.LADRON : Ajustes.rolBallestero ? Rol.BALLESTERO : Ajustes.rolForzador ? Rol.FORZADOR : Rol.JEFE;
        while (roles.size() < n) roles.add(relleno);
        Collections.shuffle(roles, new java.util.Random(r.nextLong()));
        return roles;
    }

    /** Makes a gang arrive near the player now. size 0 = by world day. Returns how many came. */
    public static int invocar(ServerPlayer player, int size) {
        ServerLevel level = player.level();
        RandomSource r = level.getRandom();
        long dia = dia(level);
        int n = size > 0 ? Math.min(size, 32) : tamano(dia, r);
        BlockPos donde = lugar(level, player.blockPosition(), r);
        if (donde == null) return 0;
        int llegaron = 0;
        for (Rol rol : roles(n, dia, r)) {
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
            equipar(ratero, rol, dia, level, r);
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

    // ---------- weapons and armor, better as days go by ----------

    private static ItemStack arma(Rol rol, long dia, RandomSource r) {
        boolean tarde = Ajustes.mejorarArmas && dia >= Ajustes.diaGrande;
        boolean medio = Ajustes.mejorarArmas && dia >= Ajustes.diaMedio;
        boolean diamante = Ajustes.permitirDiamante && tarde && r.nextFloat() < (rol == Rol.JEFE ? 0.35F : 0.08F);
        Item item = switch (rol) {
            case BALLESTERO -> Items.CROSSBOW;
            case FORZADOR -> diamante ? Items.DIAMOND_AXE : medio || r.nextBoolean() ? Items.IRON_AXE : Items.STONE_AXE;
            case JEFE -> diamante ? Items.DIAMOND_SWORD : Items.IRON_SWORD;
            case LADRON -> diamante ? Items.DIAMOND_SWORD : medio && r.nextBoolean() || r.nextInt(3) == 0 ? Items.IRON_SWORD : Items.STONE_SWORD;
        };
        return new ItemStack(item);
    }

    private static void encantar(ItemStack stack, Rol rol, long dia, ServerLevel level, RandomSource r) {
        if (!Ajustes.permitirEncantamientos || !Ajustes.mejorarArmas || dia < Ajustes.diaMedio) return;
        float prob = dia >= Ajustes.diaGrande ? (rol == Rol.JEFE ? 0.7F : 0.35F) : (rol == Rol.JEFE ? 0.35F : 0.15F);
        if (r.nextFloat() >= prob) return;
        int nivel = dia >= Ajustes.diaGrande ? 10 + r.nextInt(rol == Rol.JEFE ? 21 : 11) : 3 + r.nextInt(8);
        Optional<HolderSet.Named<Enchantment>> mesa = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
        ItemStack encantado = EnchantmentHelper.enchantItem(r, stack, nivel, level.registryAccess(), mesa);
        if (encantado != stack) stack.applyComponents(encantado.getComponents());
    }

    public static void equipar(Ratero ratero, Rol rol, long dia, ServerLevel level, RandomSource r) {
        ItemStack arma = arma(rol, dia, r);
        encantar(arma, rol, dia, level, r);
        ratero.setItemSlot(EquipmentSlot.MAINHAND, arma);
        ratero.setDropChance(EquipmentSlot.MAINHAND, arma.is(Items.DIAMOND_SWORD) || arma.is(Items.DIAMOND_AXE) ? 0.02F : 0.085F);
        if (rol == Rol.JEFE) {
            AttributeInstance vida = ratero.getAttribute(Attributes.MAX_HEALTH);
            if (vida != null) {
                vida.addPermanentModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(Rateros.MODID, "jefe_vida"), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                ratero.setHealth(ratero.getMaxHealth());
            }
            ratero.setItemSlot(EquipmentSlot.CHEST, new ItemStack(dia >= Ajustes.diaGrande * 2 && Ajustes.permitirDiamante ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE));
            ratero.setDropChance(EquipmentSlot.CHEST, 0.0F);
        }
        if (rol == Rol.LADRON) {
            // Thieves are a bit faster
            AttributeInstance vel = ratero.getAttribute(Attributes.MOVEMENT_SPEED);
            if (vel != null) vel.addPermanentModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(Rateros.MODID, "ladron_rapido"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** All bandits around a player. */
    public static List<Ratero> alrededor(ServerPlayer player, double radio) {
        return player.level().getEntitiesOfClass(Ratero.class, new AABB(player.blockPosition()).inflate(radio));
    }

    private Bandas() {}
}
