package fr.needaname.mc_rpg_mod.skill.model;

import java.util.List;
import java.util.Optional;

/**
 * Un arbre de compétences chargé depuis un JSON validé. Les dix arbres partagent ce modèle ;
 * Mort est le seul à porter {@code xpOnDeath}, {@code counters}, des {@code branches} et des
 * {@code appearances} (les autres arbres portent {@code xpSources} à la place de {@code xpOnDeath}).
 */
public record SkillTree(int schema,
                        String id,
                        String name,
                        String color,
                        int maxLevel,
                        XpCurve xpCurve,
                        List<XpSource> xpSources,
                        Optional<XpOnDeath> xpOnDeath,
                        Optional<Counters> counters,
                        List<Tier> tiers,
                        List<Branch> branches,
                        List<Appearance> appearances) {
    public SkillTree {
        xpSources = List.copyOf(xpSources);
        tiers = List.copyOf(tiers);
        branches = List.copyOf(branches);
        appearances = List.copyOf(appearances);
    }
}
