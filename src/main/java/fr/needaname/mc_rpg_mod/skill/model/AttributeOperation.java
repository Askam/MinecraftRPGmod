package fr.needaname.mc_rpg_mod.skill.model;

import java.util.Optional;

/** Les trois opérations d'un modificateur d'attribut vanilla. */
public enum AttributeOperation {
    ADDITION("addition"),
    MULTIPLY_BASE("multiply_base"),
    MULTIPLY_TOTAL("multiply_total");

    private final String id;

    AttributeOperation(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<AttributeOperation> byId(String id) {
        for (AttributeOperation op : values()) {
            if (op.id.equals(id)) {
                return Optional.of(op);
            }
        }
        return Optional.empty();
    }
}
