package fr.needaname.mc_rpg_mod.effect.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.needaname.mc_rpg_mod.skill.SkillTreeParser;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Table de reproduction de {@code faune.json} : poids 1 / 19 / 80 → 10 / 2 / 1 petits, soit
 * 1 % / 19 % / 80 %. On teste la fonction déterministe {@code children(point de poids)} ; le hasard
 * reste à l'appelant serveur.
 */
class BreedingTableTest {

    private static BreedingTable eleveurTable() throws Exception {
        try (InputStream in = BreedingTableTest.class.getResourceAsStream("/data/mc_rpg_mod/competences/faune.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            SkillTree faune = SkillTreeParser.parse("faune", root);
            var breeding = faune.tiers().stream()
                    .flatMap(t -> t.nodes().stream())
                    .flatMap(n -> n.effects().stream())
                    .filter(e -> e.type() == EffectType.BREEDING)
                    .findFirst().orElseThrow();
            return BreedingTable.fromJson(breeding.fields().getAsJsonArray("table"));
        }
    }

    @Test
    void distributionMatchesOnePercentNineteenEighty() throws Exception {
        BreedingTable table = eleveurTable();
        assertEquals(100, table.totalWeight());
        assertEquals(10, table.children(0));    // 1 %  → dix petits
        assertEquals(2, table.children(1));     // 19 % → deux
        assertEquals(2, table.children(19));
        assertEquals(1, table.children(20));    // 80 % → un seul
        assertEquals(1, table.children(99));
    }
}
