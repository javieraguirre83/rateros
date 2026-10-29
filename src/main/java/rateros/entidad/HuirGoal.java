package rateros.entidad;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Running away with the loot: away from the nearest player, fast. */
public class HuirGoal extends Goal {
    private final Ratero ratero;
    private int recalcular = 0;

    public HuirGoal(Ratero ratero) {
        this.ratero = ratero;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return ratero.isHuyendo();
    }

    @Override
    public void start() {
        recalcular = 0;
    }

    @Override
    public void tick() {
        if (--recalcular > 0 && !ratero.getNavigation().isDone()) return;
        recalcular = 20;
        Player p = ratero.level().getNearestPlayer(ratero, 64);
        Vec3 desde = p != null ? p.position() : ratero.position().add(ratero.getRandom().nextGaussian(), 0, ratero.getRandom().nextGaussian());
        Vec3 destino = LandRandomPos.getPosAway(ratero, 24, 8, desde);
        if (destino != null) ratero.getNavigation().moveTo(destino.x, destino.y, destino.z, 1.35);
    }
}
