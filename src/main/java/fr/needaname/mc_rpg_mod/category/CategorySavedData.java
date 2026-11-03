package fr.needaname.mc_rpg_mod.category;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * État des catégories (arbre → {@link CategoryState}), stocké dans la <b>sauvegarde du monde</b>
 * (pas dans la configuration) : il voyage avec la sauvegarde et se change en jeu. Seuls les états
 * non {@code ACTIVE} sont écrits ; un arbre absent est actif.
 *
 * <p>Rien ici n'efface la progression des joueurs : désactiver ou masquer ne touche jamais leurs
 * niveaux, XP, nœuds ou compteurs.
 */
public class CategorySavedData extends SavedData {
    private static final String NAME = "mc_rpg_mod_categories";

    private final Map<String, CategoryState> states = new HashMap<>();

    public static CategorySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(CategorySavedData::load, CategorySavedData::new, NAME);
    }

    public static CategorySavedData load(CompoundTag tag) {
        CategorySavedData data = new CategorySavedData();
        for (String treeId : tag.getAllKeys()) {
            data.states.put(treeId, CategoryState.byId(tag.getString(treeId)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        states.forEach((treeId, state) -> tag.putString(treeId, state.id()));
        return tag;
    }

    public CategoryState state(String treeId) {
        return states.getOrDefault(treeId, CategoryState.ACTIVE);
    }

    public boolean isDisabled(String treeId) {
        return state(treeId) == CategoryState.DISABLED;
    }

    public void setState(String treeId, CategoryState state) {
        if (state == CategoryState.ACTIVE) {
            states.remove(treeId);
        } else {
            states.put(treeId, state);
        }
        setDirty();
    }

    /** Copie immuable des états non actifs (les seuls stockés). */
    public Map<String, CategoryState> states() {
        return Map.copyOf(states);
    }
}
