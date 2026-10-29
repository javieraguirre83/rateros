package rateros.cliente;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PillagerRenderer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.Pillager;
import rateros.core.Rateros;
import rateros.entidad.Ratero;
import rateros.entidad.Rol;

/** Same body as a pillager, with each role's own clothes (bandana, vest and a sack on the back). */
public class RateroRenderer extends PillagerRenderer {
    private static final Identifier[] TEXTURAS = new Identifier[Rol.values().length];

    static {
        for (Rol r : Rol.values()) {
            TEXTURAS[r.ordinal()] = Identifier.fromNamespaceAndPath(Rateros.MODID, "textures/entity/" + r.id + ".png");
        }
    }

    /** The render state also remembers the role. */
    public static class Estado extends IllagerRenderState {
        public Rol rol = Rol.LADRON;
    }

    public RateroRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public IllagerRenderState createRenderState() {
        return new Estado();
    }

    @Override
    public void extractRenderState(Pillager entity, IllagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (state instanceof Estado e && entity instanceof Ratero r) e.rol = r.getRol();
    }

    @Override
    public Identifier getTextureLocation(IllagerRenderState state) {
        Rol rol = state instanceof Estado e ? e.rol : Rol.LADRON;
        return TEXTURAS[rol.ordinal()];
    }
}
