package rateros.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** All settings: config/rateros/ajustes.json (also editable with the Z screen and /rateros ajustes). */
public final class Ajustes {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path ARCHIVO = Path.of("config", "rateros", "ajustes.json");

    // ---------- general ----------
    public static boolean activado = true;
    /** "cualquiera", "noche" or "dia". */
    public static String horario = "cualquiera";
    /** With "cualquiera": how many times more likely at night. */
    public static double multiplicadorNoche = 2.0;
    /** Chance of a gang per Minecraft day at the start of the world and in an advanced world (0..1). */
    public static double probInicio = 0.2;
    public static double probAvanzada = 0.5;
    /** World day from which the advanced chance applies (it grows little by little until then). */
    public static int diaAvanzado = 60;
    public static boolean aviso = true;
    /** Rob village and structure chests too (already opened ones; never fresh loot chests). */
    public static boolean asaltarAldeas = true;
    /** Radius (blocks) in which they sniff out loot. */
    public static int radioOlfato = 48;

    // ---------- the gang ----------
    public static int maxBanda = 8;
    public static boolean rolLadron = true, rolBallestero = true, rolForzador = true, rolJefe = true;
    /**
     * Gang levels, from the lowest. A level is unlocked for good the first time you have one of its
     * materials (in your inventory, your ender chest or the containers around you).
     */
    public static List<Nivel> niveles = nivelesDefecto();
    /** % of gangs that come from a random lower level instead of yours (for some variety). */
    public static int probMenor = 30;

    /**
     * One gang level. tamano "3-4"; weapons by material (madera, piedra, hierro, diamante, netherita):
     * armaBase, or armaMejor with probMejor %; how many crossbowmen; whether a forcer comes; % chance of a boss.
     */
    public record Nivel(String nombre, List<String> materiales, String tamano, String armaBase, String armaMejor,
                        int probMejor, int ballesteros, boolean forzador, int jefe) {
        JsonObject aJson() {
            JsonObject j = new JsonObject();
            j.addProperty("nombre", nombre);
            j.add("materiales", lista(materiales));
            j.addProperty("tamano", tamano);
            j.addProperty("armaBase", armaBase);
            j.addProperty("armaMejor", armaMejor);
            j.addProperty("probMejor", probMejor);
            j.addProperty("ballesteros", ballesteros);
            j.addProperty("forzador", forzador);
            j.addProperty("jefe", jefe);
            return j;
        }

        static Nivel leer(JsonObject j, Nivel d) {
            return new Nivel(texto(j, "nombre", d.nombre), leerLista(j, "materiales", d.materiales),
                    texto(j, "tamano", d.tamano), leerArma(texto(j, "armaBase", d.armaBase)),
                    leerArma(texto(j, "armaMejor", d.armaMejor)), entero(j, "probMejor", d.probMejor, 0, 100),
                    entero(j, "ballesteros", d.ballesteros, 0, 32), bool(j, "forzador", d.forzador),
                    entero(j, "jefe", d.jefe, 0, 100));
        }
    }

    public static List<Nivel> nivelesDefecto() {
        return new ArrayList<>(List.of(
                new Nivel("nada", List.of(), "2", "madera", "piedra", 50, 0, false, 0),
                new Nivel("hierro", List.of("minecraft:iron_ingot", "minecraft:iron_block", "minecraft:lapis_lazuli", "minecraft:lapis_block"),
                        "2-3", "piedra", "hierro", 25, 1, false, 0),
                new Nivel("oro", List.of("minecraft:gold_ingot", "minecraft:gold_block", "minecraft:emerald", "minecraft:emerald_block",
                        "minecraft:redstone", "minecraft:redstone_block"), "3-4", "hierro", "hierro", 0, 1, true, 0),
                new Nivel("diamante", List.of("minecraft:diamond", "minecraft:diamond_block"), "4-6", "hierro", "diamante", 20, 1, true, 50),
                new Nivel("netherita", List.of("minecraft:netherite_ingot", "minecraft:netherite_block"), "5-8", "diamante", "netherita", 20, 2, true, 100)));
    }

    public static String leerArma(String texto) {
        String t = texto == null ? "" : texto.toLowerCase(Locale.ROOT);
        return switch (t) {
            case "madera", "wood", "wooden" -> "madera";
            case "hierro", "iron" -> "hierro";
            case "diamante", "diamond" -> "diamante";
            case "netherita", "netherite" -> "netherita";
            default -> "piedra";
        };
    }

    // ---------- the theft ----------
    /** "suave", "normal", "agresivo" or "despiadado". */
    public static String nivelHurto = "normal";
    public static int cargaLadron = 3, cargaBallestero = 1, cargaForzador = 2, cargaJefe = 4;
    public static int valiosoLadron = 10, valiosoBallestero = 10, valiosoForzador = 25, valiosoJefe = 50;
    public static List<String> valiosos = new ArrayList<>(List.of(
            "minecraft:diamond", "minecraft:emerald", "minecraft:netherite_ingot", "minecraft:netherite_scrap",
            "minecraft:diamond_block", "minecraft:emerald_block", "minecraft:netherite_block", "minecraft:gold_block",
            "minecraft:ancient_debris", "minecraft:totem_of_undying", "minecraft:elytra", "minecraft:enchanted_golden_apple",
            "minecraft:nether_star", "minecraft:heart_of_the_sea", "minecraft:trident", "minecraft:beacon", "encantado"));
    public static List<String> nuncaRobar = new ArrayList<>();
    /** Junk they are not interested in (not "worked" goods). */
    public static List<String> ignorar = new ArrayList<>(List.of(
            "minecraft:dirt", "minecraft:grass_block", "minecraft:gravel", "minecraft:sand", "minecraft:red_sand",
            "minecraft:cobblestone", "minecraft:cobbled_deepslate", "minecraft:netherrack", "minecraft:tuff",
            "minecraft:diorite", "minecraft:andesite", "minecraft:granite", "minecraft:rotten_flesh",
            "minecraft:wheat_seeds", "minecraft:stick", "#minecraft:saplings", "minecraft:poisonous_potato"));
    /** A fleeing bandit escapes (with your things) when it is this far (blocks) from every player. */
    public static int distanciaEscape = 96;

    // ---------- breaking in ----------
    public static boolean romperBloques = true;
    public static List<String> bloquesRompibles = new ArrayList<>(List.of(
            "#minecraft:wooden_doors", "#minecraft:wooden_trapdoors", "#minecraft:fence_gates", "#minecraft:planks",
            "#minecraft:logs", "#minecraft:wooden_fences", "#minecraft:wooden_stairs", "#minecraft:wooden_slabs",
            "#minecraft:wool", "#minecraft:leaves", "#minecraft:impermeable", "minecraft:glass_pane",
            "minecraft:dirt", "minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:hay_block"));
    /** Seconds it takes a normal bandit to break one block (the forcer takes half). */
    public static int segundosRomper = 4;

    // ---------- theft level ----------

    /** Multiplier of how much each bandit carries. 0 = no limit. */
    public static double factorCarga() {
        return switch (nivelHurto) {
            case "suave" -> 0.5;
            case "agresivo" -> 2.0;
            case "despiadado" -> 0.0;
            default -> 1.0;
        };
    }

    /** Multiplier of the chance of taking something valuable. */
    public static double factorValioso() {
        return switch (nivelHurto) {
            case "suave" -> 0.5;
            case "agresivo" -> 1.5;
            case "despiadado" -> 2.0;
            default -> 1.0;
        };
    }

    public static String leerNivel(String texto) {
        String t = texto == null ? "" : texto.toLowerCase(Locale.ROOT);
        return switch (t) {
            case "suave", "soft", "gentle" -> "suave";
            case "agresivo", "aggressive" -> "agresivo";
            case "despiadado", "ruthless" -> "despiadado";
            default -> "normal";
        };
    }

    public static String leerHorario(String texto) {
        String t = texto == null ? "" : texto.toLowerCase(Locale.ROOT).replace("í", "i");
        return switch (t) {
            case "noche", "night" -> "noche";
            case "dia", "day" -> "dia";
            default -> "cualquiera";
        };
    }

    /** {min, max} of a size text like "3-5". */
    public static int[] rango(String texto, int min, int max) {
        try {
            String[] p = texto.trim().split("-");
            int a = Integer.parseInt(p[0].trim());
            int b = p.length > 1 ? Integer.parseInt(p[1].trim()) : a;
            return new int[]{Math.max(1, Math.min(a, b)), Math.max(1, Math.max(a, b))};
        } catch (Exception e) {
            return new int[]{min, max};
        }
    }

    // ---------- load / save ----------

    /** Version of the file format: 2 = gangs by unlocked level (1.1.0). */
    private static final int VERSION = 2;
    /** The original values, to go back to them. */
    private static final JsonObject DEFECTO = aJson();

    /** Back to the original values (keeps whether raids are on or off). */
    public static void restaurar() {
        boolean act = activado;
        aplicar(DEFECTO.deepCopy());
        activado = act;
        guardar();
    }

    public static void cargar() {
        try {
            if (Files.exists(ARCHIVO)) {
                try (Reader r = Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
                    JsonObject j = GSON.fromJson(r, JsonObject.class);
                    if (j != null) {
                        aplicar(j);
                        // Files from 1.0.x: the escape distance changed meaning (it is now the only escape rule)
                        if (entero(j, "_version", 1, 1, 1000) < 2 && distanciaEscape == 48) distanciaEscape = 96;
                    }
                }
            }
        } catch (Exception e) {
            Rateros.LOGGER.error("No se pudo leer " + ARCHIVO, e);
        }
        guardar();
    }

    public static void guardar() {
        try {
            Files.createDirectories(ARCHIVO.getParent());
            try (Writer w = Files.newBufferedWriter(ARCHIVO, StandardCharsets.UTF_8)) {
                GSON.toJson(aJson(), w);
            }
        } catch (Exception e) {
            Rateros.LOGGER.error("No se pudo guardar " + ARCHIVO, e);
        }
    }

    public static JsonObject aJson() {
        JsonObject j = new JsonObject();
        j.addProperty("_ayuda", "Ajustes de los rateros. horario: cualquiera, noche o dia. nivelHurto: suave, normal, agresivo o despiadado. "
                + "Las probabilidades de banda van de 0 a 1 (0.2 = 1 de cada 5 dias). valioso*: porcentaje (0-100). "
                + "Listas: ids como minecraft:diamond o etiquetas como #minecraft:planks; en valiosos, \"encantado\" = cualquier objeto encantado.");
        j.addProperty("_niveles", "niveles: del mas bajo al mas alto. Se desbloquea para siempre la primera vez que tienes uno de sus materiales "
                + "(en tu inventario, tu cofre de ender o los cofres cerca de ti). tamano: \"3-4\". Armas: madera, piedra, hierro, diamante o netherita; "
                + "armaMejor sale con probMejor %. ballesteros: cuantos. forzador: si viene uno. jefe: % de que venga.");
        j.addProperty("_version", VERSION);
        j.addProperty("activado", activado);
        j.addProperty("horario", horario);
        j.addProperty("multiplicadorNoche", multiplicadorNoche);
        j.addProperty("probInicio", probInicio);
        j.addProperty("probAvanzada", probAvanzada);
        j.addProperty("diaAvanzado", diaAvanzado);
        j.addProperty("aviso", aviso);
        j.addProperty("asaltarAldeas", asaltarAldeas);
        j.addProperty("radioOlfato", radioOlfato);
        j.addProperty("maxBanda", maxBanda);
        j.addProperty("rolLadron", rolLadron);
        j.addProperty("rolBallestero", rolBallestero);
        j.addProperty("rolForzador", rolForzador);
        j.addProperty("rolJefe", rolJefe);
        j.addProperty("probMenor", probMenor);
        JsonArray ns = new JsonArray();
        niveles.forEach(n -> ns.add(n.aJson()));
        j.add("niveles", ns);
        j.addProperty("nivelHurto", nivelHurto);
        j.addProperty("cargaLadron", cargaLadron);
        j.addProperty("cargaBallestero", cargaBallestero);
        j.addProperty("cargaForzador", cargaForzador);
        j.addProperty("cargaJefe", cargaJefe);
        j.addProperty("valiosoLadron", valiosoLadron);
        j.addProperty("valiosoBallestero", valiosoBallestero);
        j.addProperty("valiosoForzador", valiosoForzador);
        j.addProperty("valiosoJefe", valiosoJefe);
        j.add("valiosos", lista(valiosos));
        j.add("nuncaRobar", lista(nuncaRobar));
        j.add("ignorar", lista(ignorar));
        j.addProperty("distanciaEscape", distanciaEscape);
        j.addProperty("romperBloques", romperBloques);
        j.add("bloquesRompibles", lista(bloquesRompibles));
        j.addProperty("segundosRomper", segundosRomper);
        return j;
    }

    private static JsonArray lista(List<String> l) {
        JsonArray a = new JsonArray();
        l.forEach(a::add);
        return a;
    }

    private static List<String> leerLista(JsonObject j, String clave, List<String> actual) {
        if (!j.has(clave) || !j.get(clave).isJsonArray()) return actual;
        List<String> r = new ArrayList<>();
        for (JsonElement e : j.getAsJsonArray(clave)) {
            String s = e.getAsString().trim();
            if (!s.isEmpty()) r.add(s);
        }
        return r;
    }

    private static int entero(JsonObject j, String k, int actual, int min, int max) {
        try { return j.has(k) ? Math.max(min, Math.min(max, j.get(k).getAsInt())) : actual; } catch (Exception e) { return actual; }
    }

    private static double decimal(JsonObject j, String k, double actual, double min, double max) {
        try { return j.has(k) ? Math.max(min, Math.min(max, j.get(k).getAsDouble())) : actual; } catch (Exception e) { return actual; }
    }

    private static boolean bool(JsonObject j, String k, boolean actual) {
        try { return j.has(k) ? j.get(k).getAsBoolean() : actual; } catch (Exception e) { return actual; }
    }

    private static String texto(JsonObject j, String k, String actual) {
        try { return j.has(k) ? j.get(k).getAsString() : actual; } catch (Exception e) { return actual; }
    }

    /** Applies the values present in a JSON (missing ones keep their value). */
    public static void aplicar(JsonObject j) {
        activado = bool(j, "activado", activado);
        horario = leerHorario(texto(j, "horario", horario));
        multiplicadorNoche = decimal(j, "multiplicadorNoche", multiplicadorNoche, 0.1, 20);
        probInicio = decimal(j, "probInicio", probInicio, 0, 1);
        probAvanzada = decimal(j, "probAvanzada", probAvanzada, 0, 1);
        diaAvanzado = entero(j, "diaAvanzado", diaAvanzado, 1, 100000);
        aviso = bool(j, "aviso", aviso);
        asaltarAldeas = bool(j, "asaltarAldeas", asaltarAldeas);
        radioOlfato = entero(j, "radioOlfato", radioOlfato, 8, 128);
        maxBanda = entero(j, "maxBanda", maxBanda, 1, 32);
        rolLadron = bool(j, "rolLadron", rolLadron);
        rolBallestero = bool(j, "rolBallestero", rolBallestero);
        rolForzador = bool(j, "rolForzador", rolForzador);
        rolJefe = bool(j, "rolJefe", rolJefe);
        probMenor = entero(j, "probMenor", probMenor, 0, 100);
        if (j.has("niveles") && j.get("niveles").isJsonArray()) {
            List<Nivel> def = nivelesDefecto();
            List<Nivel> r = new ArrayList<>();
            JsonArray a = j.getAsJsonArray("niveles");
            for (int i = 0; i < a.size(); i++) {
                if (!a.get(i).isJsonObject()) continue;
                r.add(Nivel.leer(a.get(i).getAsJsonObject(), i < def.size() ? def.get(i) : def.get(def.size() - 1)));
            }
            if (!r.isEmpty()) niveles = r;
        }
        nivelHurto = leerNivel(texto(j, "nivelHurto", nivelHurto));
        cargaLadron = entero(j, "cargaLadron", cargaLadron, 0, 54);
        cargaBallestero = entero(j, "cargaBallestero", cargaBallestero, 0, 54);
        cargaForzador = entero(j, "cargaForzador", cargaForzador, 0, 54);
        cargaJefe = entero(j, "cargaJefe", cargaJefe, 0, 54);
        valiosoLadron = entero(j, "valiosoLadron", valiosoLadron, 0, 100);
        valiosoBallestero = entero(j, "valiosoBallestero", valiosoBallestero, 0, 100);
        valiosoForzador = entero(j, "valiosoForzador", valiosoForzador, 0, 100);
        valiosoJefe = entero(j, "valiosoJefe", valiosoJefe, 0, 100);
        valiosos = leerLista(j, "valiosos", valiosos);
        nuncaRobar = leerLista(j, "nuncaRobar", nuncaRobar);
        ignorar = leerLista(j, "ignorar", ignorar);
        distanciaEscape = entero(j, "distanciaEscape", distanciaEscape, 16, 256);
        romperBloques = bool(j, "romperBloques", romperBloques);
        bloquesRompibles = leerLista(j, "bloquesRompibles", bloquesRompibles);
        segundosRomper = entero(j, "segundosRomper", segundosRomper, 1, 120);
    }

    private Ajustes() {}
}
