package fr.needaname.mc_rpg_mod.skill.model;

import java.util.List;

/** Les compteurs de l'arbre Mort : un total, et un compteur par cause. */
public record Counters(boolean total, List<CauseCounter> perCause) {
    public Counters {
        perCause = List.copyOf(perCause);
    }
}
