package fr.needaname.mc_rpg_mod.skill.model;

import java.util.Optional;

/** Les trois familles de déclenchement d'un nœud ({@code docs/SCHEMA.md}, « Déclenchement »). */
public enum TriggerFamily {
    REFLEX("reflex"),
    HOLD("hold"),
    POWER("power");

    private final String id;

    TriggerFamily(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<TriggerFamily> byId(String id) {
        for (TriggerFamily family : values()) {
            if (family.id.equals(id)) {
                return Optional.of(family);
            }
        }
        return Optional.empty();
    }
}
