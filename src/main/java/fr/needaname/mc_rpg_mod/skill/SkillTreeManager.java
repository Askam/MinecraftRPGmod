package fr.needaname.mc_rpg_mod.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Charge {@code data/<namespace>/competences/*.json}, rechargé par {@code /reload}.
 * L'id d'un arbre est celui du fichier (ex. {@code mc_rpg_mod:agilite}).
 *
 * <p>Un barème invalide est refusé : on journalise l'erreur (qui nomme le fichier et le champ)
 * et l'arbre n'est pas enregistré. Un {@code /reload} ne doit jamais faire tomber le serveur
 * sur un JSON cassé — le build, lui, échoue déjà via {@code validerBaremes}.
 */
public class SkillTreeManager extends SimpleJsonResourceReloadListener {
    // GSON doit être initialisé avant INSTANCE (ordre d'initialisation statique).
    private static final Gson GSON = new GsonBuilder().create();
    public static final SkillTreeManager INSTANCE = new SkillTreeManager();

    private volatile Map<ResourceLocation, SkillTree> trees = Map.of();
    /** JSON source de chaque arbre chargé (id de fichier → texte), pour l'envoyer tel quel au client. */
    private volatile Map<String, String> rawByTreeId = Map.of();

    private SkillTreeManager() {
        super(GSON, "competences");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, SkillTree> loaded = new HashMap<>();
        Map<String, String> raw = new HashMap<>();
        jsons.forEach((id, json) -> {
            try {
                if (!json.isJsonObject()) {
                    throw new SkillLoadException(id.getPath() + ".json : racine : objet JSON attendu");
                }
                loaded.put(id, SkillTreeParser.parse(id.getPath(), json.getAsJsonObject()));
                raw.put(id.getPath(), json.toString());
            } catch (SkillLoadException ex) {
                RpgMod.LOGGER.error("Barème refusé {} : {}", id, ex.getMessage());
            }
        });
        trees = Map.copyOf(loaded);
        rawByTreeId = Map.copyOf(raw);
        RpgMod.LOGGER.info("Chargé {} arbre(s) de compétences", trees.size());
    }

    public Map<ResourceLocation, SkillTree> trees() {
        return trees;
    }

    /** JSON source des arbres chargés (id → texte), pour la synchro client. */
    public Map<String, String> rawByTreeId() {
        return rawByTreeId;
    }

    public Optional<SkillTree> byId(String id) {
        return trees.values().stream().filter(tree -> tree.id().equals(id)).findFirst();
    }
}
