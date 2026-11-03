package fr.needaname.mc_rpg_mod.category;

/**
 * État d'une catégorie (un des dix arbres), décidé par l'administrateur. Trois états, pas deux
 * interrupteurs — et dans tous les cas rien n'est effacé (seul {@code /competence reset} efface).
 */
public enum CategoryState {
    /** Normal. */
    ACTIVE("actif"),
    /** Visible côté joueur mais grisé : l'XP ne monte plus, les effets sont retirés. */
    DISABLED("desactive"),
    /** Absent de l'écran du joueur, comme s'il n'existait pas — mais les effets continuent. */
    HIDDEN("masque");

    private final String id;

    CategoryState(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static CategoryState byId(String id) {
        for (CategoryState state : values()) {
            if (state.id.equals(id)) {
                return state;
            }
        }
        return ACTIVE;
    }
}
