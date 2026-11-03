package fr.needaname.mc_rpg_mod.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.needaname.mc_rpg_mod.skill.model.Effect;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.Node;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les dix barèmes réels (ressources du mod) se chargent sans erreur par le chargeur Java.
 * C'est le pendant côté Java de {@code tools/valider.py} : les deux doivent accepter les mêmes fichiers.
 */
class RealBaremesTest {

    private static final List<String> TREES = List.of(
            "agilite", "bucheronnage", "combat", "combat_distance", "faune",
            "flore", "minage", "mort", "natation", "vitalite");

    private static SkillTree load(String stem) throws Exception {
        String path = "/data/mc_rpg_mod/competences/" + stem + ".json";
        try (InputStream in = RealBaremesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "ressource introuvable sur le classpath : " + path);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return SkillTreeParser.parse(stem, root);
        }
    }

    @Test
    void allTenTreesLoad() throws Exception {
        for (String stem : TREES) {
            SkillTree tree = load(stem);
            assertEquals(stem, tree.id(), stem + " : id");
            assertEquals(50, tree.maxLevel(), stem + " : max_level");
            assertTrue(tree.xpCurve().base() > 0, stem + " : base de courbe");
            assertTrue(!tree.tiers().isEmpty(), stem + " : paliers");
        }
    }

    @Test
    void deathTreeHasCountersBranchesAndAppearances() throws Exception {
        SkillTree mort = load("mort");
        assertTrue(mort.xpOnDeath().isPresent(), "mort : xp_on_death");
        assertEquals(300, mort.xpOnDeath().get().requiresActiveSecondsSinceLastDeath());
        assertTrue(mort.counters().isPresent(), "mort : counters");
        assertEquals(6, mort.counters().get().perCause().size());
        assertEquals(8, mort.branches().size());
        assertEquals(4, mort.appearances().size());
        // mort est le seul arbre sans xp_sources.
        assertTrue(mort.xpSources().isEmpty());
    }

    @Test
    void onlyDeathTreeUsesXpOnDeath() throws Exception {
        for (String stem : TREES) {
            SkillTree tree = load(stem);
            if (stem.equals("mort")) {
                assertTrue(tree.xpOnDeath().isPresent());
            } else {
                assertTrue(tree.xpOnDeath().isEmpty(), stem + " ne doit pas porter xp_on_death");
                assertTrue(!tree.xpSources().isEmpty(), stem + " doit porter des xp_sources");
            }
        }
    }

    /**
     * Table de reproduction de {@code faune.json} : 1 / 19 / 80 pour 10 / 2 / 1 petits.
     * On vérifie ici que le modèle la lit fidèlement ; la distribution de probabilité
     * (1 % / 19 % / 80 %) sera testée avec la logique de reproduction (bloc 6).
     */
    @Test
    void breedingTableParsesWithExpectedWeights() throws Exception {
        SkillTree faune = load("faune");
        Node eleveur = faune.tiers().stream()
                .flatMap(t -> t.nodes().stream())
                .filter(n -> n.id().equals("eleveur"))
                .findFirst().orElseThrow();
        Effect breeding = eleveur.effects().stream()
                .filter(e -> e.type() == EffectType.BREEDING)
                .findFirst().orElseThrow();
        JsonArray table = breeding.fields().getAsJsonArray("table");
        assertEquals(3, table.size());
        assertRow(table.get(0).getAsJsonObject(), 1, 10);
        assertRow(table.get(1).getAsJsonObject(), 19, 2);
        assertRow(table.get(2).getAsJsonObject(), 80, 1);
    }

    private static void assertRow(JsonObject row, int weight, int children) {
        assertEquals(weight, row.get("weight").getAsInt());
        assertEquals(children, row.get("children").getAsInt());
    }
}
