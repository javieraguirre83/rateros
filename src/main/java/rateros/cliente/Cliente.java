package rateros.cliente;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import rateros.core.Rateros;
import net.minecraft.world.entity.monster.illager.Pillager;

/** Client side: the bandits' look and the Z key (changeable in Options -> Controls). */
public final class Cliente {
    public static final KeyMapping.Category CATEGORIA = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Rateros.MODID, "teclas"));
    public static final KeyMapping TECLA_AJUSTES = new KeyMapping("rateros.tecla.ajustes", GLFW.GLFW_KEY_Z, CATEGORIA);

    public static void iniciar() {
        // A bandit is a kind of pillager, so the pillager-based renderer fits it
        EntityRendererProvider<Pillager> renderer = RateroRenderer::new;
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(e -> e.registerEntityRenderer(Rateros.RATERO.get(), renderer));
        RegisterKeyMappingsEvent.BUS.addListener(e -> e.register(TECLA_AJUSTES));
        InputEvent.Key.BUS.addListener(Cliente::alTecla);
    }

    private static void alTecla(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || event.getAction() != GLFW.GLFW_PRESS) return;
        if (!TECLA_AJUSTES.isUnbound() && TECLA_AJUSTES.getKey().getValue() == event.getKey()) {
            mc.gui.setScreen(new PantallaAjustes());
        }
    }

    private Cliente() {}
}
