package fr.needaname.mc_rpg_mod.skill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * La synchro des barèmes vers le client passe par le texte JSON ({@code JsonElement.toString()}),
 * relu avec le même {@link SkillTreeParser}. Ce test vérifie que l'aller-retour texte → arbre redonne
 * un arbre identique pour les dix barèmes réels : c'est ce qui garantit qu'aucune divergence
 * serveur/client ne se glisse.
 */
class SkillTreeWireRoundTripTest {

    private static final List<String> TREES = List.of(
            "agilite", "bucheronnage", "combat", "combat_distance", "faune",
            "flore", "minage", "mort", "natation", "vitalite");

    private static JsonObject original(String stem) throws Exception {
        String path = "/data/mc_rpg_mod/competences/" + stem + ".json";
        try (InputStream in = SkillTreeWireRoundTripTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void wireRoundTripYieldsIdenticalTrees() throws Exception {
        for (String stem : TREES) {
            JsonObject source = original(stem);
            SkillTree fromSource = SkillTreeParser.parse(stem, source);

            // Ce que le paquet transporte puis relit côté client : le texte, reparsé.
            String wire = source.toString();
            SkillTree fromWire = SkillTreeParser.parse(stem, JsonParser.parseString(wire).getAsJsonObject());

            assertEquals(fromSource, fromWire, stem + " : l'arbre reçu doit être identique à l'arbre serveur");
        }
    }
}
