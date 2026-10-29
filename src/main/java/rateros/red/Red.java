package rateros.red;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import rateros.core.Ajustes;
import rateros.core.Niveles;
import rateros.core.Rateros;

/**
 * "pedir": the screen asks for the settings; "cambiar": it sends the changed values (saved at once);
 * "recargar": read the file again; "restaurar": back to the original values. The server always answers with "ajustes" (all values).
 */
public final class Red {
    private static final Gson GSON = new Gson();
    public static SimpleChannel CANAL;

    public static void registrar() {
        CANAL = ChannelBuilder.named(Identifier.fromNamespaceAndPath(Rateros.MODID, "ajustes"))
                .networkProtocolVersion(1)
                .simpleChannel();
        CANAL.messageBuilder(Paquete.class)
                .encoder(Paquete::escribir)
                .decoder(Paquete::new)
                .consumerMainThread(Red::recibir)
                .add();
        CANAL.build();
    }

    private static void recibir(Paquete p, CustomPayloadEvent.Context ctx) {
        if (ctx.isServerSide()) {
            ServerPlayer player = ctx.getSender();
            if (player != null) servidor(player, p);
        } else {
            rateros.cliente.DatosCliente.recibir(p);
        }
        ctx.setPacketHandled(true);
    }

    private static void servidor(ServerPlayer player, Paquete p) {
        switch (p.accion()) {
            case "cambiar" -> {
                try {
                    JsonObject j = GSON.fromJson(p.json(), JsonObject.class);
                    if (j != null) Ajustes.aplicar(j);
                    Ajustes.guardar();
                } catch (Exception e) {
                    Rateros.LOGGER.warn("Ajuste invalido: {}", p.json());
                }
            }
            case "recargar" -> Ajustes.cargar();
            case "restaurar" -> Ajustes.restaurar();
            default -> { }
        }
        JsonObject respuesta = Ajustes.aJson();
        // The level this player has unlocked, to show it on the screen
        Niveles.actualizar(player);
        respuesta.addProperty("_nivel", Ajustes.niveles.get(Niveles.nivel(player)).nombre());
        alJugador(player, new Paquete("ajustes", respuesta.toString()));
    }

    public static void alServidor(Paquete p) {
        CANAL.send(p, PacketDistributor.SERVER.noArg());
    }

    public static void alJugador(ServerPlayer player, Paquete p) {
        CANAL.send(p, PacketDistributor.PLAYER.with(player));
    }

    private Red() {}
}
