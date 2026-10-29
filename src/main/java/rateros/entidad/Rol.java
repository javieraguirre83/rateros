package rateros.entidad;

import rateros.core.Ajustes;

import java.util.Locale;

/** The four kinds of bandit. */
public enum Rol {
    LADRON("ladron"),
    BALLESTERO("ballestero"),
    FORZADOR("forzador"),
    JEFE("jefe");

    public final String id;

    Rol(String id) {
        this.id = id;
    }

    public String clave() {
        return "rateros.rol." + id;
    }

    /** How many stacks this role carries (with the theft level applied). Integer.MAX_VALUE = no limit. */
    public int carga() {
        int base = switch (this) {
            case LADRON -> Ajustes.cargaLadron;
            case BALLESTERO -> Ajustes.cargaBallestero;
            case FORZADOR -> Ajustes.cargaForzador;
            case JEFE -> Ajustes.cargaJefe;
        };
        double f = Ajustes.factorCarga();
        if (f <= 0) return Integer.MAX_VALUE;
        return Math.max(base > 0 ? 1 : 0, (int) Math.round(base * f));
    }

    /** Chance (0..1) of going for the most valuable thing on each grab. */
    public double probValioso() {
        int base = switch (this) {
            case LADRON -> Ajustes.valiosoLadron;
            case BALLESTERO -> Ajustes.valiosoBallestero;
            case FORZADOR -> Ajustes.valiosoForzador;
            case JEFE -> Ajustes.valiosoJefe;
        };
        return Math.min(1.0, base / 100.0 * Ajustes.factorValioso());
    }

    /** Fighters go after players on sight; thieves only fight back when hit. */
    public boolean pelea() {
        return this != LADRON;
    }

    public static Rol leer(String texto) {
        String t = texto == null ? "" : texto.toLowerCase(Locale.ROOT).replace("ó", "o");
        for (Rol r : values()) if (r.id.equals(t)) return r;
        return LADRON;
    }
}
