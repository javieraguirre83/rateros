package rateros.core;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rateros.comandos.Comandos;
import rateros.entidad.Ratero;
import rateros.red.Red;

/** Rateros: bandit gangs that sniff out and steal your worked goods. */
@Mod(Rateros.MODID)
public final class Rateros {
    public static final String MODID = "rateros";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final RegistryObject<EntityType<Ratero>> RATERO = ENTIDADES.register("ratero",
            () -> EntityType.Builder.<Ratero>of(Ratero::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build(ENTIDADES.key("ratero")));

    public Rateros(FMLJavaModLoadingContext context) {
        var modBus = context.getModBusGroup();
        ENTIDADES.register(modBus);
        EntityAttributeCreationEvent.BUS.addListener(e -> e.put(RATERO.get(), Ratero.crearAtributos().build()));
        FMLCommonSetupEvent.getBus(modBus).addListener(e -> Ajustes.cargar());
        TickEvent.PlayerTickEvent.Post.BUS.addListener(e -> {
            if (e.player() instanceof ServerPlayer p) Bandas.revisar(p);
        });
        // Keep the unlocked level (and the last gang time) when the player respawns
        PlayerEvent.Clone.BUS.addListener(e -> Bandas.copiar(e.getOriginal(), e.getEntity()));
        RegisterCommandsEvent.BUS.addListener(e -> Comandos.registrar(e.getDispatcher()));
        Red.registrar();
        if (FMLEnvironment.dist.isClient()) {
            rateros.cliente.Cliente.iniciar();
        }
    }
}
