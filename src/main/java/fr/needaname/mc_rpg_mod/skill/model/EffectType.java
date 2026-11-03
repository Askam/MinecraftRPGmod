package fr.needaname.mc_rpg_mod.skill.model;

import java.util.Optional;

/**
 * Catalogue fermé des douze types d'effets (voir {@code docs/SCHEMA.md}).
 * Un {@code type} absent de cet énuméré fait refuser le fichier : jamais un comportement deviné.
 */
public enum EffectType {
    ATTRIBUTE("attribute"),
    MOVEMENT("movement"),
    DAMAGE_TAKEN("damage_taken"),
    DAMAGE_DEALT("damage_dealt"),
    FALL_DISTANCE("fall_distance"),
    DROP_BONUS("drop_bonus"),
    BREAK_SHAPE("break_shape"),
    BREEDING("breeding"),
    AIR("air"),
    ABILITY("ability"),
    APPEARANCE("appearance"),
    FLAG("flag");

    private final String id;

    EffectType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<EffectType> byId(String id) {
        for (EffectType type : values()) {
            if (type.id.equals(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
