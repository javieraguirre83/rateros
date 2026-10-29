package rateros.entidad;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * When a bandit with a melee weapon is chasing you and cannot reach you (you hid in your house),
 * it breaks weak blocks to get in. Crossbowmen just keep shooting.
 */
public class PersecucionGoal extends Goal {
    private final Ratero ratero;
    private final Rompedor rompedor;
    private int ticksSinAvance = 0;
    private double mejorDistancia = Double.MAX_VALUE;
    private int revisar = 0;
    private boolean sinCamino = false;

    public PersecucionGoal(Ratero ratero) {
        this.ratero = ratero;
        this.rompedor = new Rompedor(ratero);
        // No flags: it works alongside the attack goal, only stepping in to break things
        setFlags(EnumSet.noneOf(Flag.class));
    }

    private LivingEntity objetivo() {
        LivingEntity t = ratero.getTarget();
        if (t == null || !t.isAlive() || ratero.isHuyendo() || ratero.isHolding(Items.CROSSBOW)) return null;
        return t;
    }

    @Override
    public boolean canUse() {
        return objetivo() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return objetivo() != null || rompedor.rompiendo();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        ticksSinAvance = 0;
        mejorDistancia = Double.MAX_VALUE;
        revisar = 0;
    }

    @Override
    public void stop() {
        rompedor.cancelar();
    }

    @Override
    public void tick() {
        if (rompedor.tick()) return;
        LivingEntity t = objetivo();
        if (t == null) return;
        if (ratero.distanceToSqr(t) < 2.5 * 2.5) {
            ticksSinAvance = 0;
            return;
        }
        if (--revisar <= 0) {
            revisar = 20;
            Path p = ratero.getNavigation().createPath(t, 0);
            sinCamino = p == null || !p.canReach();
        }
        double d = ratero.distanceToSqr(t);
        if (d < mejorDistancia - 0.25) {
            mejorDistancia = d;
            ticksSinAvance = 0;
        } else {
            ticksSinAvance++;
        }
        boolean atorado = sinCamino && ticksSinAvance > 30 || ratero.horizontalCollision && ticksSinAvance > 40;
        if (atorado) {
            rompedor.intentar(t.position().add(0, 0.9, 0), null);
            ticksSinAvance = 0;
            mejorDistancia = Double.MAX_VALUE;
        }
    }
}
