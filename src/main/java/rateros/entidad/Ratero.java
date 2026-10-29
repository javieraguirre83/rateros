package rateros.entidad;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import rateros.core.Ajustes;
import rateros.core.Botin;

import java.util.List;

/**
 * A bandit: looks like a pillager in its own clothes, sniffs out worked goods in chests,
 * opens them (lid, sound and trapped-chest redstone included), steals and runs away.
 */
public class Ratero extends Pillager implements ContainerUser {
    private static final EntityDataAccessor<String> ROL = SynchedEntityData.defineId(Ratero.class, EntityDataSerializers.STRING);

    /** What it stole (dropped when it dies, lost if it escapes). */
    private final SimpleContainer botin = new SimpleContainer(54);
    private int tomados = 0;
    private boolean huyendo = false;
    private int ticksHuyendo = 0;
    private int ticksVida = 0;
    /** Container it is opening right now (for the lid animation and trapped chests). */
    BlockPos abiertoEn = null;

    public Ratero(EntityType<? extends Ratero> type, Level level) {
        super(type, level);
        setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder crearAtributos() {
        return Pillager.createAttributes();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROL, Rol.LADRON.id);
    }

    public Rol getRol() {
        return Rol.leer(entityData.get(ROL));
    }

    public void setRol(Rol rol) {
        entityData.set(ROL, rol.id);
        setCustomName(Component.translatable(rol.clave()));
    }

    @Override
    protected void registerGoals() {
        // Our own set of goals (no raids, no patrols, no banners)
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HuirGoal(this));
        goalSelector.addGoal(2, new RangedCrossbowAttackGoal<>(this, 1.0, 8.0F));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            public boolean canUse() {
                return !isHolding(Items.CROSSBOW) && super.canUse();
            }
        });
        goalSelector.addGoal(2, new PersecucionGoal(this));
        goalSelector.addGoal(3, new RobarGoal(this));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 15.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return getRol().pelea() && !huyendo && super.canUse();
            }
        });
    }

    // ---------- loot ----------

    public SimpleContainer botin() {
        return botin;
    }

    public int tomados() {
        return tomados;
    }

    public boolean puedeCargar() {
        return tomados < getRol().carga();
    }

    /** Puts a stolen stack in the sack. */
    public boolean guardar(ItemStack stack) {
        if (stack.isEmpty() || !botin.canAddItem(stack)) return false;
        botin.addItem(stack);
        tomados++;
        if (!puedeCargar() || sacoLleno()) huir();
        return true;
    }

    /** No empty slot left in the sack (matters with the "ruthless" level, which has no limit). */
    public boolean sacoLleno() {
        for (ItemStack s : botin.getItems()) if (s.isEmpty()) return false;
        return true;
    }

    public boolean isHuyendo() {
        return huyendo;
    }

    public void huir() {
        if (huyendo) return;
        huyendo = true;
        ticksHuyendo = 0;
        setTarget(null);
    }

    // Items lying on the ground count as loot too
    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        return !huyendo && puedeCargar() && Botin.interesa(stack) && botin.canAddItem(stack);
    }

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (entity.getPersistentData().getBooleanOr(Rompedor.NO_TOMAR, false)) return;
        if (!wantsToPickUp(level, stack)) return;
        onItemPickup(entity);
        take(entity, stack.getCount());
        guardar(stack.copy());
        entity.discard();
    }

    // ---------- opening containers (ContainerUser) ----------

    @Override
    public boolean hasContainerOpen(ContainerOpenersCounter counter, BlockPos pos) {
        return pos.equals(abiertoEn);
    }

    @Override
    public double getContainerInteractionRange() {
        return 4.0;
    }

    // ---------- every tick: escaping, giving up ----------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) return;
        ticksVida++;
        if (getHealth() < getMaxHealth() * 0.35F && tomados > 0) huir();
        // Nothing to steal for a long time: the gang leaves
        if (!huyendo && ticksVida > 20 * 60 * 5 && getTarget() == null) huir();
        if (huyendo) {
            ticksHuyendo++;
            if (ticksHuyendo % 20 == 0 && (ticksHuyendo > Ajustes.tiempoEscape * 20 || lejosDeTodos())) escapar();
        }
    }

    private boolean lejosDeTodos() {
        double d = Ajustes.distanciaEscape;
        return level().getNearestPlayer(this, d) == null;
    }

    /** Gone, and whatever it carried is lost. */
    public void escapar() {
        if (level() instanceof ServerLevel server && !botin.isEmpty()) {
            int cosas = 0;
            for (ItemStack s : botin.getItems()) cosas += s.getCount();
            Component msg = Component.translatable("rateros.escapo", cosas).withStyle(ChatFormatting.RED);
            for (ServerPlayer p : server.players()) {
                if (p.distanceToSqr(this) < 96 * 96) p.sendSystemMessage(msg);
            }
        }
        botin.clearContent();
        discard();
    }

    /** Removes it giving back what it carried (used by /rateros retirar). */
    public void retirarDevolviendo(ServerLevel level) {
        for (ItemStack s : botin.removeAllItems()) spawnAtLocation(level, s);
        discard();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (ItemStack s : botin.removeAllItems()) spawnAtLocation(level, s);
        if (getRol() == Rol.JEFE) spawnAtLocation(level, new ItemStack(Items.EMERALD, 3 + random.nextInt(6)));
    }

    // ---------- not a normal pillager ----------

    @Override
    public boolean canJoinRaid() {
        return false;
    }

    @Override
    public boolean canJoinPatrol() {
        return false;
    }

    @Override
    public boolean canBeLeader() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    // ---------- save / load ----------

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("Rol", getRol().id);
        out.putInt("Tomados", tomados);
        out.putBoolean("Huyendo", huyendo);
        out.putInt("TicksHuyendo", ticksHuyendo);
        out.putInt("TicksVida", ticksVida);
        botin.storeAsItemList(out.list("Botin", ItemStack.CODEC));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(ROL, Rol.leer(in.getStringOr("Rol", "ladron")).id);
        tomados = in.getIntOr("Tomados", 0);
        huyendo = in.getBooleanOr("Huyendo", false);
        ticksHuyendo = in.getIntOr("TicksHuyendo", 0);
        ticksVida = in.getIntOr("TicksVida", 0);
        botin.fromItemList(in.listOrEmpty("Botin", ItemStack.CODEC));
    }

    /** Everyone nearby that belongs to the same kind (for messages). */
    public static List<Ratero> cerca(Level level, net.minecraft.world.phys.AABB area) {
        return level.getEntitiesOfClass(Ratero.class, area);
    }
}
