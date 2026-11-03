package fr.needaname.mc_rpg_mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.death.DeathStats;
import fr.needaname.mc_rpg_mod.effect.server.AttributeEffects;
import fr.needaname.mc_rpg_mod.effect.server.HoldKeyState;
import fr.needaname.mc_rpg_mod.effect.server.PowerActivation;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.Node;
import fr.needaname.mc_rpg_mod.skill.model.Tier;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import fr.needaname.mc_rpg_mod.skill.player.SkillSyncEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.Map;
import java.util.Optional;

/**
 * Commandes {@code /mc_rpg_mod}. {@code deaths} est consultable par tous (pour soi) ; {@code node},
 * {@code slot}, {@code power} et {@code hold} sont de l'outillage de test réservé aux ops, en
 * attendant l'écran de compétences et les touches (handoff 3) : sans eux, ni les paliers exclusifs,
 * ni les pouvoirs, ni la touche de maintien ne sont atteignables.
 *
 * <p>Les anciennes {@code xp}, {@code level} et {@code skills} ont migré vers {@code /competence}
 * ({@code xp}, {@code niveau}, {@code info}), pour qu'il n'y ait qu'un seul chemin.
 */
public final class RpgCommands {
    private static final SuggestionProvider<CommandSourceStack> NODES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(SkillTreeManager.INSTANCE.trees().values().stream()
                    .flatMap(tree -> tree.tiers().stream())
                    .flatMap(tier -> tier.nodes().stream())
                    .map(Node::id), builder);
    private static final SuggestionProvider<CommandSourceStack> SLOTS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new String[]{"R", "G", "X"}, builder);

    private RpgCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(RpgMod.MODID)
                .then(Commands.literal("deaths")
                        .executes(ctx -> showDeaths(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> showDeaths(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(debug("node")
                        .then(Commands.argument("node", StringArgumentType.word()).suggests(NODES)
                                .executes(RpgCommands::takeNode)))
                .then(debug("slot")
                        .then(Commands.argument("slot", StringArgumentType.word()).suggests(SLOTS)
                                .then(Commands.argument("node", StringArgumentType.word()).suggests(NODES)
                                        .executes(RpgCommands::assignSlot))))
                .then(debug("power")
                        .then(Commands.argument("slot", StringArgumentType.word()).suggests(SLOTS)
                                .executes(ctx -> {
                                    PowerActivation.activate(ctx.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(ctx, "slot"));
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Déclenchement demandé (résultat dans le log : pouvoir non possédé, en recharge, ou déclenché)"), false);
                                    return 1;
                                })))
                .then(debug("hold")
                        .then(Commands.argument("held", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean held = BoolArgumentType.getBool(ctx, "held");
                                    HoldKeyState.set(ctx.getSource().getPlayerOrException(), held);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Touche de maintien " + (held ? "enfoncée" : "relâchée")), false);
                                    return 1;
                                }))));
    }

    /** Ops seulement en production ; ouvert à tous dans l'environnement de dev (runClient), même sans cheats. */
    private static LiteralArgumentBuilder<CommandSourceStack> debug(String name) {
        return Commands.literal(name).requires(source -> !FMLEnvironment.production || source.hasPermission(2));
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
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> source.sendSuccess(() -> Component.literal("  " + e.getKey() + " : " + e.getValue()), false));
        return stats.getTotal();
    }

    /** Prend un nœud de palier exclusif, en retirant les autres nœuds du même palier (un seul choix). */
    private static int takeNode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String nodeId = StringArgumentType.getString(ctx, "node");
        Optional<Tier> tier = SkillTreeManager.INSTANCE.trees().values().stream()
                .flatMap(tree -> tree.tiers().stream())
                .filter(t -> t.nodes().stream().anyMatch(n -> n.id().equals(nodeId)))
                .findFirst();
        if (tier.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Nœud inconnu : " + nodeId));
            return 0;
        }
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            tier.get().nodes().forEach(n -> skills.removeNode(n.id()));
            skills.addNode(nodeId);
        });
        refresh(player);
        String note = tier.get().exclusive() ? "" : " (palier non exclusif : déjà accordé d'office au niveau requis)";
        ctx.getSource().sendSuccess(() -> Component.literal("Nœud choisi : " + nodeId + " (palier " + tier.get().level()
                + ", actif seulement une fois ce niveau atteint)" + note), false);
        return 1;
    }

    private static int assignSlot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String slot = StringArgumentType.getString(ctx, "slot");
        String nodeId = StringArgumentType.getString(ctx, "node");
        PlayerSkillsProvider.get(player).ifPresent(skills -> skills.setPowerSlot(slot, nodeId));
        SkillSyncEvents.syncTo(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Emplacement " + slot + " → " + nodeId), false);
        return 1;
    }

    private static void refresh(ServerPlayer player) {
        AttributeEffects.reapply(player);
        SkillSyncEvents.syncTo(player);
    }
}
