package fr.needaname.mc_rpg_mod.skill.model;

import com.google.gson.JsonObject;

import java.util.Optional;

/**
 * Ce qui fait monter un arbre. Deux formes (voir {@code docs/SCHEMA.md}) :
 * <ul>
 *   <li>{@link Stat} : une statistique vanilla cumulative, {@code xp} tous les {@code per} pas ;</li>
 *   <li>{@link Event} : un événement ponctuel ({@code break_block}, {@code kill}, …), filtré et
 *       conditionné au besoin, éventuellement cumulatif lui aussi ({@code per}, ex. dégâts subis).</li>
 * </ul>
 */
public sealed interface XpSource {

    int xp();

    /** Statistique vanilla cumulative : {@code xp} tous les {@code per} unités de {@code stat}. */
    record Stat(String stat, int per, int xp) implements XpSource {
    }

    /**
     * Événement ponctuel. {@code per} est présent quand l'événement s'accumule (ex. {@code damage_survived}
     * tous les 20 points) ; absent, chaque occurrence rapporte {@code xp}.
     */
    record Event(String on, Optional<Integer> per, Optional<Filter> filter,
                 Optional<JsonObject> require, int xp) implements XpSource {
    }
}
