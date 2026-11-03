package fr.needaname.mc_rpg_mod.skill.model;

import java.util.List;

/**
 * Un palier. {@code exclusive} rend ses nœuds mutuellement exclusifs (le joueur en prend un) ;
 * un palier non exclusif n'a qu'un nœud, offert. Ces deux invariants sont vérifiés au chargement.
 */
public record Tier(int level, boolean exclusive, List<Node> nodes) {
    public Tier {
        nodes = List.copyOf(nodes);
    }
}
