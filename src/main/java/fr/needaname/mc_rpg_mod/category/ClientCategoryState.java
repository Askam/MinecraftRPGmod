package fr.needaname.mc_rpg_mod.category;

import java.util.Map;

/**
 * Miroir client des états de catégorie, alimenté par {@code SyncCategoriesPacket}. Distinct de la
 * {@link CategorySavedData} du serveur (la vérité) : le client n'en fait qu'un usage d'affichage
 * (griser un arbre désactivé, masquer un arbre masqué). Le serveur revérifie tout.
 */
public final class ClientCategoryState {
    private static volatile Map<String, CategoryState> states = Map.of();

    private ClientCategoryState() {
    }

    public static void set(Map<String, CategoryState> newStates) {
        states = Map.copyOf(newStates);
    }

    public static CategoryState state(String treeId) {
        return states.getOrDefault(treeId, CategoryState.ACTIVE);
    }

    public static Map<String, CategoryState> all() {
        return states;
    }
}
