package rateros.cliente;

import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import rateros.red.Paquete;
import rateros.red.Red;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Settings screen (key Z). Every change is saved at once, like Minecraft's own options. */
public class PantallaAjustes extends Screen {
    private enum Pestana { GENERAL, BANDA, HURTO, ENTRADA }

    private static Pestana pestana = Pestana.GENERAL;
    private boolean cargado = false;
    private int izq, der;
    /** Labels drawn on the left of each row. */
    private final List<Object[]> etiquetas = new ArrayList<>();
    private Component aviso = null;
    private long avisoHasta = 0;

    public PantallaAjustes() {
        super(Component.translatable("rateros.pantalla.titulo"));
        Red.alServidor(new Paquete("pedir", ""));
    }

    public void actualizar() {
        if (!cargado) {
            cargado = true;
            rebuildWidgets();
        }
    }

    // ---------- values ----------

    private static JsonObject a() {
        return DatosCliente.ajustes;
    }

    private static boolean bool(String k, boolean def) {
        try { return a().has(k) ? a().get(k).getAsBoolean() : def; } catch (Exception e) { return def; }
    }

    private static String texto(String k, String def) {
        try { return a().has(k) ? a().get(k).getAsString() : def; } catch (Exception e) { return def; }
    }

    private static double num(String k, double def) {
        try { return a().has(k) ? a().get(k).getAsDouble() : def; } catch (Exception e) { return def; }
    }

    private void cambiar(String clave, Object valor) {
        JsonObject j = new JsonObject();
        if (valor instanceof Boolean b) j.addProperty(clave, b);
        else if (valor instanceof Number n) j.addProperty(clave, n);
        else j.addProperty(clave, String.valueOf(valor));
        a().add(clave, j.get(clave));
        Red.alServidor(new Paquete("cambiar", j.toString()));
        aviso = Component.translatable("rateros.pantalla.guardado");
        avisoHasta = System.currentTimeMillis() + 1500;
    }

    // ---------- layout ----------

    private int fila(int i) {
        return 34 + i * 23;
    }

    private void etiqueta(int fila, String clave) {
        etiquetas.add(new Object[]{fila, Component.translatable(clave)});
    }

    /** On/off button; with a name it shows "Name: ON", without one just "ON". */
    private void interruptor(int fila, int x, int w, String clave, String etiquetaBoton) {
        CycleButton.Builder<Boolean> b = CycleButton.onOffBuilder(bool(clave, true));
        if (etiquetaBoton == null) b = b.displayOnlyValue();
        Component nombre = etiquetaBoton == null ? Component.empty() : Component.translatable(etiquetaBoton);
        addRenderableWidget(b.create(x, fila(fila), w, 20, nombre, (btn, v) -> cambiar(clave, v)));
    }

    private void opciones(int fila, int x, int w, String clave, List<String> valores, String prefijo) {
        String actual = texto(clave, valores.get(0));
        if (!valores.contains(actual)) actual = valores.get(0);
        addRenderableWidget(CycleButton.builder((String v) -> Component.translatable(prefijo + v), actual)
                .withValues(valores).displayOnlyValue()
                .create(x, fila(fila), w, 20, Component.empty(), (b, v) -> cambiar(clave, v)));
    }

    /** A number box; "escala" turns what is shown into what is saved (e.g. % -> 0..1). */
    private void numero(int fila, int x, int w, String clave, double def, double escala, boolean entero) {
        double v = num(clave, def) * escala;
        EditBox e = new EditBox(font, x, fila(fila), w, 20, Component.empty());
        e.setValue(entero || v == Math.rint(v) ? String.valueOf((long) Math.rint(v)) : String.valueOf(v));
        e.setResponder(s -> {
            try {
                double n = Double.parseDouble(s.trim().replace(',', '.'));
                if (n < 0) return;
                if (entero) cambiar(clave, (int) Math.round(n / escala));
                else cambiar(clave, n / escala);
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(e);
    }

    private void textoRango(int fila, int x, int w, String clave, String def) {
        EditBox e = new EditBox(font, x, fila(fila), w, 20, Component.empty());
        e.setValue(texto(clave, def));
        e.setResponder(s -> {
            if (s.trim().matches("\\d+\\s*(-\\s*\\d+)?")) cambiar(clave, s.trim().replace(" ", ""));
        });
        addRenderableWidget(e);
    }

    @Override
    protected void init() {
        etiquetas.clear();
        int ancho = Math.min(width - 20, 420);
        izq = (width - ancho) / 2;
        der = izq + ancho;
        Pestana[] todas = Pestana.values();
        int anchoTab = ancho / todas.length;
        for (int i = 0; i < todas.length; i++) {
            Pestana p = todas[i];
            Button b = Button.builder(Component.translatable("rateros.pestana." + p.name().toLowerCase()), x -> {
                pestana = p;
                rebuildWidgets();
            }).bounds(izq + i * anchoTab, 6, anchoTab - 2, 20).build();
            b.active = p != pestana;
            addRenderableWidget(b);
        }

        int x = izq + 170;
        int w = Math.max(80, der - x);
        switch (pestana) {
            case GENERAL -> {
                etiqueta(0, "rateros.aj.activado");
                interruptor(0, x, w, "activado", null);
                etiqueta(1, "rateros.aj.horario");
                opciones(1, x, w, "horario", List.of("cualquiera", "noche", "dia"), "rateros.horario.");
                etiqueta(2, "rateros.aj.multiplicadorNoche");
                numero(2, x, 50, "multiplicadorNoche", 2, 1, false);
                etiqueta(3, "rateros.aj.prob");
                numero(3, x, 40, "probInicio", 0.2, 100, true);
                numero(3, x + 44, 40, "probAvanzada", 0.5, 100, true);
                etiqueta(4, "rateros.aj.diaAvanzado");
                numero(4, x, 50, "diaAvanzado", 60, 1, true);
                etiqueta(5, "rateros.aj.aviso");
                interruptor(5, x, w, "aviso", null);
                etiqueta(6, "rateros.aj.asaltarAldeas");
                interruptor(6, x, w, "asaltarAldeas", null);
            }
            case BANDA -> {
                etiqueta(0, "rateros.aj.maxBanda");
                numero(0, x, 50, "maxBanda", 8, 1, true);
                etiqueta(1, "rateros.aj.tamanos");
                textoRango(1, x, 40, "tamInicio", "2-3");
                textoRango(1, x + 44, 40, "tamMedio", "3-5");
                textoRango(1, x + 88, 40, "tamAvanzado", "4-8");
                etiqueta(2, "rateros.aj.dias");
                numero(2, x, 40, "diaMedio", 11, 1, true);
                numero(2, x + 44, 40, "diaGrande", 31, 1, true);
                etiqueta(3, "rateros.aj.roles");
                int wr = Math.max(40, w / 4 - 2);
                interruptor(3, x, wr, "rolLadron", "rateros.rolcorto.ladron");
                interruptor(3, x + (wr + 2), wr, "rolBallestero", "rateros.rolcorto.ballestero");
                interruptor(3, x + 2 * (wr + 2), wr, "rolForzador", "rateros.rolcorto.forzador");
                interruptor(3, x + 3 * (wr + 2), wr, "rolJefe", "rateros.rolcorto.jefe");
                etiqueta(4, "rateros.aj.mejorarArmas");
                interruptor(4, x, w, "mejorarArmas", null);
                etiqueta(5, "rateros.aj.permitirDiamante");
                interruptor(5, x, w, "permitirDiamante", null);
                etiqueta(6, "rateros.aj.permitirEncantamientos");
                interruptor(6, x, w, "permitirEncantamientos", null);
            }
            case HURTO -> {
                etiqueta(0, "rateros.aj.nivelHurto");
                opciones(0, x, w, "nivelHurto", List.of("suave", "normal", "agresivo", "despiadado"), "rateros.nivel.");
                etiqueta(1, "rateros.aj.carga");
                numero(1, x, 40, "cargaLadron", 3, 1, true);
                numero(1, x + 44, 40, "cargaBallestero", 1, 1, true);
                numero(1, x + 88, 40, "cargaForzador", 2, 1, true);
                numero(1, x + 132, 40, "cargaJefe", 4, 1, true);
                etiqueta(2, "rateros.aj.valioso");
                numero(2, x, 40, "valiosoLadron", 10, 1, true);
                numero(2, x + 44, 40, "valiosoBallestero", 10, 1, true);
                numero(2, x + 88, 40, "valiosoForzador", 25, 1, true);
                numero(2, x + 132, 40, "valiosoJefe", 50, 1, true);
                etiqueta(3, "rateros.aj.tiempoEscape");
                numero(3, x, 50, "tiempoEscape", 120, 1, true);
                etiqueta(4, "rateros.aj.distanciaEscape");
                numero(4, x, 50, "distanciaEscape", 48, 1, true);
                etiqueta(5, "rateros.aj.radioOlfato");
                numero(5, x, 50, "radioOlfato", 48, 1, true);
            }
            case ENTRADA -> {
                etiqueta(0, "rateros.aj.romperBloques");
                interruptor(0, x, w, "romperBloques", null);
                etiqueta(1, "rateros.aj.segundosRomper");
                numero(1, x, 50, "segundosRomper", 4, 1, true);
                int w2 = (der - izq) / 2;
                addRenderableWidget(Button.builder(Component.translatable("rateros.pantalla.carpeta"), b -> abrirCarpeta())
                        .bounds(izq, height - 56, w2 - 2, 20).build());
                addRenderableWidget(Button.builder(Component.translatable("rateros.pantalla.recargar"), b -> {
                    Red.alServidor(new Paquete("recargar", ""));
                    cargado = false;
                    aviso = Component.translatable("rateros.recargado");
                    avisoHasta = System.currentTimeMillis() + 2500;
                }).bounds(izq + w2, height - 56, w2 - 2, 20).build());
            }
        }
    }

    private void abrirCarpeta() {
        try {
            Path carpeta = Path.of("config", "rateros").toAbsolutePath();
            Files.createDirectories(carpeta);
            Util.getPlatform().openPath(carpeta);
        } catch (Exception e) {
            aviso = Component.translatable("rateros.pantalla.no_carpeta");
            avisoHasta = System.currentTimeMillis() + 3000;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        for (Object[] e : etiquetas) {
            g.text(font, (Component) e[1], izq, fila((Integer) e[0]) + 6, 0xFFFFFFFF);
        }
        Function<String, Component> gris = k -> Component.translatable(k).withStyle(ChatFormatting.GRAY);
        int x = izq + 170;
        switch (pestana) {
            case GENERAL -> g.text(font, gris.apply("rateros.aj.prob_ayuda"), x + 90, fila(3) + 6, -1);
            case BANDA -> {
                g.text(font, gris.apply("rateros.aj.tamanos_ayuda"), x + 132, fila(1) + 6, -1);
                g.text(font, gris.apply("rateros.aj.dias_ayuda"), x + 88, fila(2) + 6, -1);
            }
            case HURTO -> {
                g.textWithWordWrap(font, gris.apply("rateros.aj.hurto_ayuda"), izq, fila(6) + 2, der - izq, -1);
            }
            case ENTRADA -> g.textWithWordWrap(font, gris.apply("rateros.aj.listas_ayuda"), izq, fila(2) + 6, der - izq, -1);
        }
        if (aviso != null && System.currentTimeMillis() < avisoHasta) {
            g.centeredText(font, aviso.copy().withStyle(ChatFormatting.YELLOW), width / 2, height - 14, -1);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
