package fr.needaname.mc_rpg_mod.effect;

import fr.needaname.mc_rpg_mod.skill.model.Branch;
import fr.needaname.mc_rpg_mod.skill.model.Effect;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.Node;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.Tier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Calcule les effets possédés par un joueur dans un arbre, à partir de sa progression. Source
 * centrale du répartiteur : chaque consommateur d'effets (dégâts, butin, attributs, break_shape…)
 * demande ici « quels effets de tel type ce joueur possède-t-il ? ».
 *
 * <p>Logique pure (modèle + progression), testable sans Minecraft. Aucune valeur d'équilibrage :
 * les seuils (niveaux de palier, {@code threshold} de branche) viennent du barème.
 *
 * <p>Règles d'activation :
 * <ul>
 *   <li>palier non exclusif (nœud offert) : actif dès que le niveau du joueur atteint le palier ;</li>
 *   <li>palier exclusif : actif seulement si le joueur a pris ce nœud ;</li>
 *   <li>{@code requires_node} : actif seulement si le nœud amélioré est lui-même actif ;</li>
 *   <li>branche : active dès que le compteur de sa cause atteint {@code threshold}.</li>
 * </ul>
 */
public final class EffectResolver {
    private EffectResolver() {
    }

    /** L'état d'un joueur vis-à-vis d'un arbre, abstrait pour rester découplé des capabilities. */
    public interface Progress {
        int level();

        boolean hasNode(String nodeId);

        int causeCount(String cause);
    }

    public static Progress progress(int level, Set<String> takenNodes, ToIntFunction<String> causeCount) {
        Set<String> taken = Set.copyOf(takenNodes);
        return new Progress() {
            @Override
            public int level() {
                return level;
            }

            @Override
            public boolean hasNode(String nodeId) {
                return taken.contains(nodeId);
            }

            @Override
            public int causeCount(String cause) {
                return causeCount.applyAsInt(cause);
            }
        };
    }

    public static List<ActiveEffect> active(SkillTree tree, Progress progress) {
        List<ActiveEffect> out = new ArrayList<>();
        Set<String> activeOwners = new HashSet<>();

        // Paliers dans l'ordre croissant (garanti au chargement) : les cibles de requires_node,
        // toujours d'un palier antérieur, sont déjà résolues quand on y arrive.
        for (Tier tier : tree.tiers()) {
            for (Node node : tier.nodes()) {
                boolean reached = progress.level() >= tier.level();
                boolean chosen = !tier.exclusive() || progress.hasNode(node.id());
                boolean requirementMet = node.requiresNode().isEmpty()
                        || activeOwners.contains(node.requiresNode().get());
                if (reached && chosen && requirementMet) {
                    activeOwners.add(node.id());
                    for (Effect effect : node.effects()) {
                        out.add(new ActiveEffect(node.id(), effect, false));
                    }
                }
            }
        }

        for (Branch branch : tree.branches()) {
            if (progress.causeCount(branch.cause()) >= branch.threshold()) {
                activeOwners.add(branch.id());
                for (Effect effect : branch.effects()) {
                    out.add(new ActiveEffect(branch.id(), effect, true));
                }
            }
        }

        return out;
    }

    public static List<ActiveEffect> activeOfType(SkillTree tree, Progress progress, EffectType type) {
        List<ActiveEffect> out = new ArrayList<>();
        for (ActiveEffect active : active(tree, progress)) {
            if (active.effect().type() == type) {
                out.add(active);
            }
        }
        return out;
    }
}
