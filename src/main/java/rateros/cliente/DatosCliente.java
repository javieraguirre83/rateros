package rateros.cliente;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import rateros.red.Paquete;

/** The settings as the server sent them last. */
public final class DatosCliente {
    private static final Gson GSON = new Gson();
    public static JsonObject ajustes = new JsonObject();
    public static int version = 0;

    public static void recibir(Paquete p) {
        if (!p.accion().equals("ajustes")) return;
        try {
            JsonObject j = GSON.fromJson(p.json(), JsonObject.class);
            if (j != null) ajustes = j;
        } catch (Exception ignored) {
        }
        version++;
        if (Minecraft.getInstance().gui.screen() instanceof PantallaAjustes s) s.actualizar();
    }

    private DatosCliente() {}
}
