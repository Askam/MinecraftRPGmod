package fr.needaname.mc_rpg_mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.category.CategoryAdmin;
import fr.needaname.mc_rpg_mod.category.CategorySavedData;
import fr.needaname.mc_rpg_mod.category.CategoryState;
import fr.needaname.mc_rpg_mod.category.CategorySyncEvents;
import fr.needaname.mc_rpg_mod.death.DeathStats;
import fr.needaname.mc_rpg_mod.death.DeathSyncEvents;
import fr.needaname.mc_rpg_mod.effect.server.AttributeEffects;
import fr.needaname.mc_rpg_mod.network.OpenAdminPacket;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.CauseCounter;
import fr.needaname.mc_rpg_mod.skill.model.Node;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import fr.needaname.mc_rpg_mod.skill.player.SkillSyncEvents;
import fr.needaname.mc_rpg_mod.skill.player.SkillXp;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code /competence} : le chemin d'administration unique de la progression. Tout passe par le
 * chemin normal (XP → montée → nœuds → effets → synchro) : aucune écriture directe qui court-circuite
 * la réapplication des effets ou la synchro client. Les complétions d'arbre et de cause viennent du
 * registre chargé, pas d'une liste en dur.
 *
 * <p>Niveau de permission 2, sauf {@code info} sur soi-même (ouvert à tous).
 * {@code categorie} et {@code admin} sont ajoutés avec l'état des catégories (chantier 2b-2).
 */
public final class CompetenceCommands {
    static final SuggestionProvider<CommandSourceStack> TREES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(SkillTreeManager.INSTANCE.trees().values().stream().map(SkillTree::id), builder);
    static final SuggestionProvider<CommandSourceStack> CAUSES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(SkillTreeManager.INSTANCE.byId("mort").stream()
                    .flatMap(tree -> tree.counters().stream())
                    .flatMap(counters -> counters.perCause().stream())
                    .map(CauseCounter::id), builder);
    static final SuggestionProvider<CommandSourceStack> STATES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    java.util.Arrays.stream(CategoryState.values()).map(CategoryState::id), builder);

    private CompetenceCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("competence")
                .then(Commands.literal("xp").requires(op())
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .then(Commands.argument("arbre", StringArgumentType.word()).suggests(TREES)
                                        .then(Commands.argument("montant", LongArgumentType.longArg())
                                                .executes(CompetenceCommands::grantXp)))))
                .then(Commands.literal("niveau").requires(op())
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .then(Commands.argument("arbre", StringArgumentType.word()).suggests(TREES)
                                        .then(Commands.argument("niveau", IntegerArgumentType.integer(0))
                                                .executes(CompetenceCommands::setLevel)))))
                .then(Commands.literal("mort").requires(op())
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .then(Commands.argument("cause", StringArgumentType.word()).suggests(CAUSES)
                                        .then(Commands.argument("nombre", IntegerArgumentType.integer(0))
                                                .executes(CompetenceCommands::setDeathCount)))))
                .then(Commands.literal("reset").requires(op())
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .executes(ctx -> reset(ctx, null))
                                .then(Commands.argument("arbre", StringArgumentType.word()).suggests(TREES)
                                        .executes(ctx -> reset(ctx, StringArgumentType.getString(ctx, "arbre"))))))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("joueur", EntityArgument.player()).requires(op())
                                .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "joueur")))))
                .then(Commands.literal("categorie").requires(op())
                        .then(Commands.argument("arbre", StringArgumentType.word()).suggests(TREES)
                                .then(Commands.argument("etat", StringArgumentType.word()).suggests(STATES)
                                        .executes(CompetenceCommands::setCategory))))
                .then(Commands.literal("admin").requires(op())
                        .executes(CompetenceCommands::openAdmin)));
    }

    private static int setCategory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        MinecraftServer server = ctx.getSource().getServer();
        SkillTree tree = requireTree(ctx);
        if (tree == null) {
            return 0;
        }
        CategoryState state = CategoryState.byId(StringArgumentType.getString(ctx, "etat"));
        CategorySavedData.get(server).setState(tree.id(), state);
        // Désactiver retire les effets par le même chemin que le reste : reapply sur chaque joueur.
        server.getPlayerList().getPlayers().forEach(AttributeEffects::reapply);
        CategorySyncEvents.syncToAll(server);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Catégorie " + tree.id() + " → " + state.id()), true);
        return 1;
    }

    private static int openAdmin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        // L'ouverture part du serveur, après la vérification de permission de la commande (requires op).
        RpgNetwork.sendTo(player, new OpenAdminPacket(CategoryAdmin.payload(ctx.getSource().getServer())));
        return 1;
    }

    private static java.util.function.Predicate<CommandSourceStack> op() {
        return source -> source.hasPermission(2);
    }

    private static int grantXp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "joueurs");
        SkillTree tree = requireTree(ctx);
        if (tree == null) {
            return 0;
        }
        long amount = LongArgumentType.getLong(ctx, "montant");
        players.forEach(player -> SkillXp.adjust(player, tree.id(), amount));
        String sign = amount >= 0 ? "+" : "";
        feedback(ctx, sign + amount + " XP en " + tree.id(), players);
        return players.size();
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "joueurs");
        SkillTree tree = requireTree(ctx);
        if (tree == null) {
            return 0;
        }
        int level = Math.min(IntegerArgumentType.getInteger(ctx, "niveau"), tree.maxLevel());
        for (ServerPlayer player : players) {
            PlayerSkillsProvider.get(player).ifPresent(skills -> {
                skills.setLevel(tree.id(), level);
                skills.setXp(tree.id(), 0);
            });
            // Chemin normal en aval : effets réappliqués, client resynchronisé.
            AttributeEffects.reapply(player);
            SkillSyncEvents.syncTo(player);
        }
        feedback(ctx, tree.id() + " → niveau " + level, players);
        return players.size();
    }

    private static int setDeathCount(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "joueurs");
        String cause = StringArgumentType.getString(ctx, "cause");
        int number = IntegerArgumentType.getInteger(ctx, "nombre");
        for (ServerPlayer player : players) {
            DeathStats.get(player).ifPresent(stats -> stats.setCount(cause, number));
            DeathSyncEvents.syncToSelfAndTrackers(player);
        }
        feedback(ctx, "compteur de mort " + cause + " = " + number, players);
        return players.size();
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, String treeId) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "joueurs");
        SkillTree tree = null;
        if (treeId != null) {
            tree = SkillTreeManager.INSTANCE.byId(treeId).orElse(null);
            if (tree == null) {
                ctx.getSource().sendFailure(Component.literal("Arbre inconnu : " + treeId));
                return 0;
            }
        }
        String what = tree == null ? "toute la progression" : "l'arbre " + tree.id();
        for (ServerPlayer player : players) {
            if (tree == null) {
                PlayerSkillsProvider.get(player).ifPresent(skills -> skills.resetAll());
                DeathStats.get(player).ifPresent(DeathStats::reset);
            } else {
                Set<String> nodeIds = treeNodeIds(tree);
                SkillTree finalTree = tree;
                PlayerSkillsProvider.get(player).ifPresent(skills -> skills.resetTree(finalTree.id(), nodeIds));
                if (tree.id().equals("mort")) {
                    DeathStats.get(player).ifPresent(DeathStats::reset);
                }
            }
            AttributeEffects.reapply(player);
            SkillSyncEvents.syncTo(player);
            DeathSyncEvents.syncToSelfAndTrackers(player);
        }
        feedback(ctx, "réinitialisation : " + what, players);
        return players.size();
    }

    private static int info(CommandSourceStack source, ServerPlayer target) {
        var skills = PlayerSkillsProvider.get(target).orElse(null);
        if (skills == null) {
            source.sendFailure(Component.literal("Aucune donnée de compétences pour " + target.getGameProfile().getName()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Compétences de " + target.getGameProfile().getName() + " :"), false);
        SkillTreeManager.INSTANCE.trees().values().stream()
                .sorted(Comparator.comparing(SkillTree::id))
                .forEach(tree -> {
                    int level = skills.level(tree.id());
                    String next = level >= tree.maxLevel() ? "max"
                            : skills.xp(tree.id()) + "/" + tree.xpCurve().xpForLevel(level + 1);
                    String nodes = tree.tiers().stream()
                            .flatMap(tier -> tier.nodes().stream())
                            .map(Node::id)
                            .filter(skills::hasNode)
                            .collect(Collectors.joining(", "));
                    source.sendSuccess(() -> Component.literal("  " + tree.id() + " : niv. " + level + " (" + next + ")"
                            + (nodes.isEmpty() ? "" : " · choisis : " + nodes)), false);
                });
        if (!skills.powerSlots().isEmpty()) {
            source.sendSuccess(() -> Component.literal("  Pouvoirs : " + skills.powerSlots()), false);
        }
        return 1;
    }

    static Set<String> treeNodeIds(SkillTree tree) {
        Set<String> ids = tree.tiers().stream()
                .flatMap(tier -> tier.nodes().stream())
                .map(Node::id)
                .collect(Collectors.toCollection(java.util.HashSet::new));
        tree.branches().forEach(branch -> ids.add(branch.id()));
        return ids;
    }

    private static SkillTree requireTree(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "arbre");
        SkillTree tree = SkillTreeManager.INSTANCE.byId(id).orElse(null);
        if (tree == null) {
            ctx.getSource().sendFailure(Component.literal("Arbre inconnu : " + id));
        }
        return tree;
    }

    private static void feedback(CommandContext<CommandSourceStack> ctx, String what, Collection<ServerPlayer> players) {
        String who = players.size() == 1
                ? players.iterator().next().getGameProfile().getName()
                : players.size() + " joueurs";
        ctx.getSource().sendSuccess(() -> Component.literal(what + " — " + who), true);
    }
}
