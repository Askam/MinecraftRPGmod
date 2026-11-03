package fr.needaname.mc_rpg_mod.skill.model;

import java.util.List;
import java.util.Optional;

/**
 * Un nœud de l'arbre Mort ouvert par un compteur de cause plutôt que par le niveau :
 * débloqué quand le compteur {@code cause} atteint {@code threshold}. La {@code cause} doit
 * exister dans {@code counters.per_cause}.
 */
public record Branch(String id,
                     String name,
                     String description,
                     String cause,
                     int threshold,
                     Optional<Trigger> trigger,
                     List<Effect> effects) {
    public Branch {
        effects = List.copyOf(effects);
    }
}
