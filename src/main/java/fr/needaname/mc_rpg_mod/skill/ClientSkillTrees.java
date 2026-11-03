package fr.needaname.mc_rpg_mod.skill;

import fr.needaname.mc_rpg_mod.skill.model.SkillTree;

import java.util.Map;
import java.util.Optional;

/**
 * Registre des arbres <b>côté client</b>, alimenté par {@code SyncSkillTreesPacket} et relu avec le
 * même {@link SkillTreeParser} que le serveur. Distinct de {@link SkillTreeManager#INSTANCE}, qui
 * reste la vérité du serveur : sans cette séparation, le solo (INSTANCE partagée) masquerait le fait
 * que, sur un serveur dédié, le client ne reçoit jamais les barèmes. Pour l'affichage seulement.
 */
public final class ClientSkillTrees {
    private static volatile Map<String, SkillTree> trees = Map.of();

    private ClientSkillTrees() {
    }

    public static void set(Map<String, SkillTree> newTrees) {
        trees = Map.copyOf(newTrees);
    }

    public static Optional<SkillTree> byId(String id) {
        return Optional.ofNullable(trees.get(id));
    }

    public static Map<String, SkillTree> all() {
        return trees;
    }
}
