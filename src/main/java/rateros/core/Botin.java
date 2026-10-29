package rateros.core;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/** What bandits want: "worked" goods (anything that is not junk), with valuables ranked higher. */
public final class Botin {

    private static Identifier id(String texto) {
        String t = texto.startsWith("#") ? texto.substring(1) : texto;
        if (!t.contains(":")) t = "minecraft:" + t;
        Identifier i = Identifier.tryParse(t.toLowerCase(Locale.ROOT));
        return i == null ? Identifier.withDefaultNamespace("air") : i;
    }

    /** One rule of a list: "minecraft:diamond", "#minecraft:planks" or "encantado" (any enchanted item). */
    public static boolean coincide(String regla, ItemStack stack) {
        if (stack.isEmpty()) return false;
        String r = regla.trim().toLowerCase(Locale.ROOT);
        if (r.equals("encantado") || r.equals("enchanted")) {
            return !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
                    || !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
        }
        if (r.startsWith("#")) return stack.is(TagKey.create(Registries.ITEM, id(r)));
        return id(r).equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean enLista(List<String> lista, ItemStack stack) {
        for (String regla : lista) if (coincide(regla, stack)) return true;
        return false;
    }

    public static boolean bloqueEnLista(List<String> lista, BlockState state) {
        for (String regla : lista) {
            String r = regla.trim().toLowerCase(Locale.ROOT);
            if (r.startsWith("#")) {
                if (state.is(TagKey.create(Registries.BLOCK, id(r)))) return true;
            } else if (id(r).equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
                return true;
            }
        }
        return false;
    }

    /** Is it something a bandit wants? (worked goods: not junk and not protected) */
    public static boolean interesa(ItemStack stack) {
        return !stack.isEmpty() && !enLista(Ajustes.nuncaRobar, stack) && !enLista(Ajustes.ignorar, stack);
    }

    public static boolean valioso(ItemStack stack) {
        return interesa(stack) && enLista(Ajustes.valiosos, stack);
    }

    /** How attractive a container is: worked items count, valuables much more. */
    public static int puntaje(Container c) {
        int p = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (!interesa(s)) continue;
            p += s.getCount();
            if (valioso(s)) p += 20 * s.getCount();
        }
        return p;
    }

    private Botin() {}
}
