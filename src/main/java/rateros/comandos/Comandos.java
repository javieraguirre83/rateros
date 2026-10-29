package rateros.comandos;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import rateros.core.Ajustes;
import rateros.core.Bandas;
import rateros.core.Niveles;
import rateros.entidad.Ratero;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /rateros (alias /bandits)
 *   (nothing): status | activar | desactivar | ajustes [clave valor] | recargar
 *   invocar [cuantos] | retirar   (these two need cheats)
 */
public final class Comandos {
    private static final DynamicCommandExceptionType CLAVE_MALA =
            new DynamicCommandExceptionType(c -> Component.translatable("rateros.error.clave", c));

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        LiteralCommandNode<CommandSourceStack> nodo = d.register(Commands.literal("rateros")
                .executes(Comandos::estado)
                .then(Commands.literal("activar").executes(ctx -> activar(ctx, true)))
                .then(Commands.literal("desactivar").executes(ctx -> activar(ctx, false)))
                .then(Commands.literal("recargar").executes(ctx -> {
                    Ajustes.cargar();
                    ctx.getSource().sendSuccess(() -> Component.translatable("rateros.recargado").withStyle(ChatFormatting.GREEN), false);
                    return 1;
                }))
                .then(Commands.literal("invocar").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> invocar(ctx, 0))
                        .then(Commands.argument("cuantos", IntegerArgumentType.integer(1, 32))
                                .executes(ctx -> invocar(ctx, IntegerArgumentType.getInteger(ctx, "cuantos")))))
                .then(Commands.literal("retirar").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(Comandos::retirar))
                .then(Commands.literal("ajustes").executes(Comandos::verAjustes)
                        .then(Commands.argument("clave", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(claves(), b))
                                .then(Commands.argument("valor", StringArgumentType.greedyString())
                                        .executes(Comandos::cambiar)))));
        d.register(Commands.literal("bandits").executes(Comandos::estado).redirect(nodo));
    }

    /** Settings that can be changed by command (the lists are edited in the file). */
    private static List<String> claves() {
        List<String> r = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : Ajustes.aJson().entrySet()) {
            if (!e.getKey().startsWith("_") && e.getValue().isJsonPrimitive()) r.add(e.getKey());
        }
        return r;
    }

    private static int estado(CommandContext<CommandSourceStack> ctx) {
        long dia = Bandas.dia(ctx.getSource().getLevel());
        int prob = (int) Math.round(Bandas.probPorDia(dia) * 100);
        ctx.getSource().sendSuccess(() -> Component.translatable("rateros.estado.titulo").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        ctx.getSource().sendSuccess(() -> Component.translatable(Ajustes.activado ? "rateros.estado.activado" : "rateros.estado.desactivado")
                .withStyle(Ajustes.activado ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("rateros.estado.dia", dia + 1, prob,
                Component.translatable("rateros.horario." + Ajustes.horario)), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("rateros.estado.hurto", Component.translatable("rateros.nivel." + Ajustes.nivelHurto)), false);
        if (ctx.getSource().getEntity() instanceof ServerPlayer p) {
            Niveles.actualizar(p);
            String nombre = Niveles.datos(p).nombre();
            ctx.getSource().sendSuccess(() -> Component.translatable("rateros.estado.nivel",
                    Component.translatableWithFallback("rateros.etapa." + nombre, nombre).withStyle(ChatFormatting.YELLOW)), false);
            int cerca = Bandas.alrededor(p, 128).size();
            if (cerca > 0) ctx.getSource().sendSuccess(() -> Component.translatable("rateros.estado.cerca", cerca).withStyle(ChatFormatting.RED), false);
        }
        return 1;
    }

    private static int activar(CommandContext<CommandSourceStack> ctx, boolean si) {
        Ajustes.activado = si;
        Ajustes.guardar();
        ctx.getSource().sendSuccess(() -> Component.translatable(si ? "rateros.estado.activado" : "rateros.estado.desactivado")
                .withStyle(si ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return 1;
    }

    private static int invocar(CommandContext<CommandSourceStack> ctx, int cuantos) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        int n = Bandas.invocar(p, cuantos);
        if (n == 0) ctx.getSource().sendFailure(Component.translatable("rateros.error.lugar"));
        return n;
    }

    private static int retirar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        List<Ratero> lista = Bandas.alrededor(p, 128);
        lista.forEach(r -> r.retirarDevolviendo(p.level()));
        ctx.getSource().sendSuccess(() -> Component.translatable("rateros.retirados", lista.size()).withStyle(ChatFormatting.GREEN), false);
        return lista.size();
    }

    private static int verAjustes(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.translatable("rateros.ajustes.titulo").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (Map.Entry<String, JsonElement> e : Ajustes.aJson().entrySet()) {
            if (e.getKey().startsWith("_")) continue;
            String valor = e.getValue().isJsonPrimitive() ? e.getValue().getAsString() : e.getValue().toString();
            ctx.getSource().sendSuccess(() -> Component.literal(" " + e.getKey() + ": ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(valor).withStyle(ChatFormatting.WHITE)), false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("rateros.ajustes.ayuda").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int cambiar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String clave = StringArgumentType.getString(ctx, "clave");
        String valor = StringArgumentType.getString(ctx, "valor").trim();
        if (!claves().contains(clave)) throw CLAVE_MALA.create(clave);
        JsonObject j = new JsonObject();
        String v = valor.toLowerCase(Locale.ROOT);
        if (v.equals("si") || v.equals("sí") || v.equals("true") || v.equals("yes")) j.add(clave, new JsonPrimitive(true));
        else if (v.equals("no") || v.equals("false")) j.add(clave, new JsonPrimitive(false));
        else {
            try {
                j.add(clave, new JsonPrimitive(Double.parseDouble(valor.replace(',', '.'))));
            } catch (NumberFormatException e) {
                j.add(clave, new JsonPrimitive(valor));
            }
        }
        Ajustes.aplicar(j);
        Ajustes.guardar();
        JsonElement nuevo = Ajustes.aJson().get(clave);
        ctx.getSource().sendSuccess(() -> Component.literal(clave + " = " + (nuevo == null ? "?" : nuevo.getAsString())).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private Comandos() {}
}
