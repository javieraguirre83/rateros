package rateros.entidad;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import rateros.core.Ajustes;

import java.util.EnumSet;

/**
 * When a bandit smells nothing to steal around it, it heads for the bed (respawn point) of the nearest
 * player: that is where your home usually is. Once close, the theft goal smells your chests and takes over.
 */
public class BuscarCasaGoal extends Goal {
    /** Farther than this from the bandit, the bed is not considered (blocks). */
    private static final int MAX_CAMA = 200;
    /** Close enough: from here the chests should be in smelling range. */
    private static final int CERCA = 12;

    private final Ratero ratero;
    private BlockPos casa;
    private int espera = 0;
    private int repath = 0;
    private int sinAvance = 0;
    private double mejor = Double.MAX_VALUE;

    public BuscarCasaGoal(Ratero ratero) {
        this.ratero = ratero;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean libre() {
        if (!Ajustes.buscarCama || ratero.isHuyendo() || !ratero.puedeCargar()) return false;
        return ratero.getTarget() == null || !ratero.getRol().pelea();
    }

    @Override
    public boolean canUse() {
        if (!libre() || --espera > 0) return false;
        espera = 100;
        casa = buscarCama();
        return casa != null && ratero.blockPosition().distSqr(casa) > CERCA * CERCA;
    }

    /** Bed of the nearest player, if it is in this dimension and not too far. */
    private BlockPos buscarCama() {
        ServerPlayer elegido = null;
        double d = Double.MAX_VALUE;
        for (Player p : ratero.level().players()) {
            if (!(p instanceof ServerPlayer sp) || sp.isSpectator()) continue;
            double dp = sp.distanceToSqr(ratero);
            if (dp < d) {
                d = dp;
                elegido = sp;
            }
        }
        if (elegido == null) return null;
        ServerPlayer.RespawnConfig cfg = elegido.getRespawnConfig();
        if (cfg == null || cfg.respawnData() == null) return null;
        if (!cfg.respawnData().dimension().equals(ratero.level().dimension())) return null;
        BlockPos pos = cfg.respawnData().pos();
        return pos.distSqr(ratero.blockPosition()) <= (double) MAX_CAMA * MAX_CAMA ? pos : null;
    }

    @Override
    public void start() {
        repath = 0;
        sinAvance = 0;
        mejor = Double.MAX_VALUE;
    }

    @Override
    public boolean canContinueToUse() {
        return casa != null && libre() && ratero.blockPosition().distSqr(casa) > CERCA * CERCA;
    }

    @Override
    public void tick() {
        double d = ratero.blockPosition().distSqr(casa);
        if (d < mejor - 1) {
            mejor = d;
            sinAvance = 0;
        } else if (++sinAvance > 400) {
            // No way there for 20 seconds: give up for a while
            casa = null;
            espera = 600;
            return;
        }
        if (--repath <= 0 || ratero.getNavigation().isDone()) {
            repath = 40;
            ratero.getNavigation().moveTo(casa.getX() + 0.5, casa.getY(), casa.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        ratero.getNavigation().stop();
    }
}
