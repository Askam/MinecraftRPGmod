package fr.needaname.mc_rpg_mod.skill.model;

import java.util.List;
import java.util.Optional;

/**
 * Un nœud d'un palier. {@code requiresNode} n'est présent que lorsqu'il améliore un nœud antérieur
 * (Ossuaire sur Appel d'outre-tombe) ; le chargeur vérifie que la cible existe.
 */
public record Node(String id,
                   String name,
                   String description,
                   Optional<Trigger> trigger,
                   List<Effect> effects,
                   Optional<String> requiresNode) {
    public Node {
        effects = List.copyOf(effects);
    }
}
