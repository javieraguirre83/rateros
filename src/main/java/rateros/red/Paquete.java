package rateros.red;

import net.minecraft.network.FriendlyByteBuf;

/** A message between the settings screen and the world: an action and a JSON text. */
public class Paquete {
    private final String accion;
    private final String json;

    public Paquete(String accion, String json) {
        this.accion = accion;
        this.json = json == null ? "" : json;
    }

    public Paquete(FriendlyByteBuf buf) {
        this.accion = buf.readUtf(64);
        this.json = buf.readUtf(262144);
    }

    public void escribir(FriendlyByteBuf buf) {
        buf.writeUtf(accion, 64);
        buf.writeUtf(json, 262144);
    }

    public String accion() {
        return accion;
    }

    public String json() {
        return json;
    }
}
