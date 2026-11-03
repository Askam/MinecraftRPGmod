package fr.needaname.mc_rpg_mod.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.death.DeathStats;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/** /rpgmod deaths [joueur] : consulter ses compteurs (ceux des autres réservés aux ops). */
public final class RpgCommands {
    private RpgCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(RpgMod.MODID)
                .then(Commands.literal("deaths")
                        .executes(ctx -> showDeaths(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> showDeaths(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))))));
    }

    private static int showDeaths(CommandSourceStack source, ServerPlayer target) {
        String name = target.getGameProfile().getName();
        DeathStats stats = DeathStats.get(target).orElse(null);
        if (stats == null) {
            source.sendFailure(Component.literal("Aucune donnée de mort pour " + name));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(name + " : " + stats.getTotal() + " mort(s)"), false);
        stats.getAll().entrySet().stream()
                .sorted(Map.Entry.<ResourceLocation, Integer>comparingByValue().reversed())
                .forEach(e -> source.sendSuccess(() -> Component.literal("  " + e.getKey() + " : " + e.getValue()), false));
        return stats.getTotal();
    }
}
