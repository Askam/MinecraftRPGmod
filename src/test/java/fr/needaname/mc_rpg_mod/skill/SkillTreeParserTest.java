package fr.needaname.mc_rpg_mod.skill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.XpSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cas synthétiques : un fichier valide se charge, chaque règle rejetée nomme fichier et champ. */
class SkillTreeParserTest {

    private static SkillTree parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return SkillTreeParser.parse("test", root);
    }

    private static SkillLoadException refuse(String json) {
        return assertThrows(SkillLoadException.class, () -> parse(json));
    }

    private static final String VALID = """
            {
              "schema": 1, "id": "test", "name": "Test", "color": "#ffffff", "max_level": 50,
              "xp_curve": { "base": 60, "exponent": 1.15 },
              "xp_sources": [ { "on": "kill", "filter": "#minecraft:monster", "xp": 12 } ],
              "tiers": [
                { "level": 5, "nodes": [
                  { "id": "a", "name": "A", "description": "d",
                    "effects": [ { "type": "flag", "flag": "x" } ] } ] },
                { "level": 10, "exclusive": true, "nodes": [
                  { "id": "b", "name": "B", "description": "d",
                    "effects": [ { "type": "attribute", "attribute": "minecraft:generic.max_health",
                                   "operation": "addition", "value": 2 } ] },
                  { "id": "c", "name": "C", "description": "d", "requires_node": "a",
                    "effects": [ { "type": "flag", "flag": "y" } ] } ] }
              ]
            }
            """;

    @Test
    void validFileLoads() {
        SkillTree tree = parse(VALID);
        assertEquals("test", tree.id());
        assertEquals(50, tree.maxLevel());
        assertEquals(2, tree.tiers().size());
        assertEquals(1, tree.xpSources().size());
        assertTrue(tree.xpSources().get(0) instanceof XpSource.Event);
        assertEquals(EffectType.FLAG, tree.tiers().get(0).nodes().get(0).effects().get(0).type());
        assertTrue(tree.tiers().get(1).exclusive());
        assertFalse(tree.counters().isPresent());
    }

    @Test
    void unknownEffectTypeIsRefused() {
        String json = VALID.replace("\"type\": \"flag\", \"flag\": \"x\"", "\"type\": \"nope\"");
        assertTrue(refuse(json).getMessage().contains("type d'effet inconnu 'nope'"));
    }

    @Test
    void unknownSchemaIsRefused() {
        assertTrue(refuse(VALID.replace("\"schema\": 1", "\"schema\": 2")).getMessage().contains("schema"));
    }

    @Test
    void idMustMatchFileName() {
        assertTrue(refuse(VALID.replace("\"id\": \"test\"", "\"id\": \"autre\""))
                .getMessage().contains("ne correspond pas au nom de fichier"));
    }

    @Test
    void twoNodesWithoutExclusiveIsRefused() {
        // Le palier 10 (exclusif, 2 nœuds) devient non exclusif : interdit.
        String json = VALID.replace("\"level\": 10, \"exclusive\": true", "\"level\": 10");
        assertTrue(refuse(json).getMessage().contains("sans exclusive=true"));
    }

    @Test
    void singleNodeWithExclusiveIsRefused() {
        String json = VALID.replace("\"level\": 5, \"nodes\"", "\"level\": 5, \"exclusive\": true, \"nodes\"");
        assertTrue(refuse(json).getMessage().contains("exclusive=true avec un seul nœud"));
    }

    @Test
    void duplicateNodeIdIsRefused() {
        String json = VALID.replace("\"id\": \"b\"", "\"id\": \"a\"");
        assertTrue(refuse(json).getMessage().contains("en double 'a'"));
    }

    @Test
    void missingRequiresNodeIsRefused() {
        String json = VALID.replace("\"requires_node\": \"a\"", "\"requires_node\": \"fantome\"");
        assertTrue(refuse(json).getMessage().contains("requires_node 'fantome' introuvable"));
    }

    @Test
    void abilityWithoutCooldownOrOverridesIsRefused() {
        String json = VALID.replace("{ \"type\": \"flag\", \"flag\": \"x\" }",
                "{ \"type\": \"ability\", \"action\": \"dash\" }");
        assertTrue(refuse(json).getMessage().contains("ability sans cooldown_ticks"));
    }

    @Test
    void abilityWithOverridesIsAccepted() {
        String json = VALID.replace("{ \"type\": \"flag\", \"flag\": \"x\" }",
                "{ \"type\": \"ability\", \"action\": \"dash\", \"overrides\": \"b\" }");
        // overrides sans cooldown_ticks est valide : l'effet hérite de la recharge du nœud corrigé.
        assertEquals("test", parse(json).id());
    }

    @Test
    void badAttributeOperationIsRefused() {
        String json = VALID.replace("\"operation\": \"addition\"", "\"operation\": \"nope\"");
        assertTrue(refuse(json).getMessage().contains("operation invalide 'nope'"));
    }

    @Test
    void tiersOutOfOrderIsRefused() {
        String json = VALID.replace("\"level\": 10, \"exclusive\": true", "\"level\": 3, \"exclusive\": true");
        assertTrue(refuse(json).getMessage().contains("désordre"));
    }

    @Test
    void missingRootKeyIsRefused() {
        assertTrue(refuse(VALID.replace("\"color\": \"#ffffff\",", "")).getMessage().contains("color"));
    }

    @Test
    void neitherXpSourcesNorXpOnDeathIsRefused() {
        // On retire le bloc xp_sources entier.
        String json = VALID.replace(
                "\"xp_sources\": [ { \"on\": \"kill\", \"filter\": \"#minecraft:monster\", \"xp\": 12 } ],", "");
        assertTrue(refuse(json).getMessage().contains("ni xp_sources ni xp_on_death"));
    }
}
