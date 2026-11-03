package fr.needaname.mc_rpg_mod.client.screen;

import fr.needaname.mc_rpg_mod.category.CategoryState;
import fr.needaname.mc_rpg_mod.category.ClientCategoryState;
import fr.needaname.mc_rpg_mod.death.DeathStats;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.effect.EffectResolver;
import fr.needaname.mc_rpg_mod.skill.ClientSkillTrees;
import fr.needaname.mc_rpg_mod.skill.model.Node;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.Tier;
import fr.needaname.mc_rpg_mod.skill.model.TriggerFamily;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkills;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Accès client aux données synchronisées (barèmes, progression, compteurs de mort, états de
 * catégorie) et calcul des états d'affichage des nœuds. Le client n'en fait qu'un usage d'affichage ;
 * il lit {@link ClientSkillTrees} (jamais {@code SkillTreeManager.INSTANCE}) et la capability locale.
 */
public final class SkillScreens {
    /** Les cinq états de lecture d'un nœud (voir handoff 3). Chacun doit être lisible sans la couleur seule. */
    public enum NodeDisplay {
        ACQUIRED("✔", "Acquis", 0xFF55FF55),
        TO_PLACE("◆", "À placer", 0xFFFFD040),
        NOT_CHOSEN("✖", "Non choisi", 0xFF888888),
        LOCKED("🔒", "Verrouillé", 0xFF777777),
        DISABLED("⊘", "Désactivé", 0xFF777777);

        public final String marker;
        public final String label;
        public final int color;

        NodeDisplay(String marker, String label, int color) {
            this.marker = marker;
            this.label = label;
            this.color = color;
        }
    }

    private SkillScreens() {
    }

    public static Optional<PlayerSkills> skills() {
        return Minecraft.getInstance().player == null
                ? Optional.empty()
                : PlayerSkillsProvider.get(Minecraft.getInstance().player);
    }

    public static ToIntFunction<String> deathCounts() {
        if (Minecraft.getInstance().player == null) {
            return cause -> 0;
        }
        return DeathStats.get(Minecraft.getInstance().player)
                .<ToIntFunction<String>>map(stats -> stats::getCount)
                .orElse(cause -> 0);
    }

    public static boolean visible(String treeId) {
        return ClientCategoryState.state(treeId) != CategoryState.HIDDEN;
    }

    public static boolean disabled(String treeId) {
        return ClientCategoryState.state(treeId) == CategoryState.DISABLED;
    }

    public static NodeDisplay stateOf(SkillTree tree, Tier tier, Node node, PlayerSkills skills) {
        if (disabled(tree.id())) {
            return NodeDisplay.DISABLED;
        }
        int level = skills.level(tree.id());
        if (level < tier.level()) {
            return NodeDisplay.LOCKED;
        }
        if (!tier.exclusive()) {
            return NodeDisplay.ACQUIRED; // nœud offert, accordé d'office au niveau requis
        }
        if (skills.hasNode(node.id())) {
            return NodeDisplay.ACQUIRED;
        }
        boolean siblingChosen = tier.nodes().stream().anyMatch(other -> skills.hasNode(other.id()));
        return siblingChosen ? NodeDisplay.NOT_CHOSEN : NodeDisplay.TO_PLACE;
    }

    /** Un talent attend d'être placé : un palier exclusif atteint dont aucun nœud n'est pris. */
    public static boolean hasPendingChoice(SkillTree tree, PlayerSkills skills) {
        if (disabled(tree.id())) {
            return false;
        }
        int level = skills.level(tree.id());
        return tree.tiers().stream().anyMatch(tier -> tier.exclusive() && level >= tier.level()
                && tier.nodes().stream().noneMatch(node -> skills.hasNode(node.id())));
    }

    /** Pouvoirs débloqués (nœuds actifs de famille power), tous arbres confondus. */
    public static List<PowerRef> unlockedPowers(PlayerSkills skills) {
        ToIntFunction<String> counts = deathCounts();
        List<PowerRef> powers = new ArrayList<>();
        for (SkillTree tree : ClientSkillTrees.all().values()) {
            EffectResolver.Progress progress =
                    EffectResolver.progress(skills.level(tree.id()), skills.nodes(), counts);
            var activeOwners = EffectResolver.active(tree, progress).stream().map(ActiveEffect::ownerId).toList();
            for (Tier tier : tree.tiers()) {
                for (Node node : tier.nodes()) {
                    boolean power = node.trigger().map(t -> t.family() == TriggerFamily.POWER).orElse(false);
                    if (power && activeOwners.contains(node.id())) {
                        powers.add(new PowerRef(tree.id(), node.id(), node.name()));
                    }
                }
            }
        }
        return powers;
    }

    public record PowerRef(String treeId, String nodeId, String name) {
    }
}
