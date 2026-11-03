package fr.needaname.mc_rpg_mod.effect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.needaname.mc_rpg_mod.skill.SkillTreeParser;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectResolverTest {

    // Arbre synthétique : un nœud offert (5), un palier exclusif (10), un nœud qui améliore (15).
    private static final SkillTree TREE = SkillTreeParser.parse("t", JsonParser.parseString("""
            {
              "schema": 1, "id": "t", "name": "T", "color": "#fff", "max_level": 50,
              "xp_curve": { "base": 60, "exponent": 1.15 },
              "xp_sources": [ { "on": "kill", "xp": 1 } ],
              "tiers": [
                { "level": 5, "nodes": [
                  { "id": "base", "name": "B", "description": "d", "effects": [ { "type": "flag", "flag": "f1" } ] } ] },
                { "level": 10, "exclusive": true, "nodes": [
                  { "id": "pick_a", "name": "A", "description": "d", "effects": [ { "type": "flag", "flag": "fa" } ] },
                  { "id": "pick_b", "name": "B", "description": "d",
                    "effects": [ { "type": "attribute", "attribute": "minecraft:generic.max_health",
                                   "operation": "addition", "value": 2 } ] } ] },
                { "level": 15, "nodes": [
                  { "id": "improve_a", "name": "I", "description": "d", "requires_node": "pick_a",
                    "effects": [ { "type": "flag", "flag": "fi" } ] } ] }
              ]
            }
            """).getAsJsonObject());

    private static Set<String> owners(List<ActiveEffect> effects) {
        return effects.stream().map(ActiveEffect::ownerId).collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void nothingActiveBelowFirstTier() {
        assertTrue(EffectResolver.active(TREE, EffectResolver.progress(4, Set.of(), c -> 0)).isEmpty());
    }

    @Test
    void offeredNodeActiveOnLevelButExclusiveNeedsPick() {
        var active = EffectResolver.active(TREE, EffectResolver.progress(14, Set.of(), c -> 0));
        assertEquals(Set.of("base"), owners(active));
    }

    @Test
    void exclusivePickUnlocksItsNode() {
        var active = EffectResolver.active(TREE, EffectResolver.progress(14, Set.of("pick_a"), c -> 0));
        assertEquals(Set.of("base", "pick_a"), owners(active));
    }

    @Test
    void requiresNodeGatesOnTheRequiredNodeBeingActive() {
        // improve_a nécessite pick_a : sans pick_a, il reste inactif même au niveau 15.
        assertFalse(owners(EffectResolver.active(TREE, EffectResolver.progress(15, Set.of(), c -> 0)))
                .contains("improve_a"));
        assertFalse(owners(EffectResolver.active(TREE, EffectResolver.progress(15, Set.of("pick_b"), c -> 0)))
                .contains("improve_a"));
        // avec pick_a, improve_a s'active.
        assertTrue(owners(EffectResolver.active(TREE, EffectResolver.progress(15, Set.of("pick_a"), c -> 0)))
                .contains("improve_a"));
    }

    @Test
    void activeOfTypeFilters() {
        var progress = EffectResolver.progress(15, Set.of("pick_a"), c -> 0);
        assertEquals(3, EffectResolver.activeOfType(TREE, progress, EffectType.FLAG).size()); // f1, fa, fi
        assertEquals(0, EffectResolver.activeOfType(TREE, progress, EffectType.ATTRIBUTE).size());

        var withB = EffectResolver.progress(15, Set.of("pick_b"), c -> 0);
        assertEquals(1, EffectResolver.activeOfType(TREE, withB, EffectType.ATTRIBUTE).size());
    }

    @Test
    void branchesUnlockByCauseCount() throws Exception {
        SkillTree mort = loadMort();
        // os_rompus : cause chute, threshold 10. atterrissage : chute, threshold 25.
        var below = EffectResolver.active(mort, EffectResolver.progress(50, Set.of(), c -> c.equals("chute") ? 9 : 0));
        assertFalse(owners(below).contains("os_rompus"));

        var at = EffectResolver.active(mort, EffectResolver.progress(50, Set.of(), c -> c.equals("chute") ? 10 : 0));
        assertTrue(owners(at).contains("os_rompus"));
        assertFalse(owners(at).contains("atterrissage"));

        var high = EffectResolver.active(mort, EffectResolver.progress(50, Set.of(), c -> c.equals("chute") ? 25 : 0));
        assertTrue(owners(high).contains("atterrissage"));
    }

    @Test
    void requiresNodeAcrossTiersResolvesInMort() throws Exception {
        SkillTree mort = loadMort();
        // ossuaire (45) requiert appel_d_outre_tombe (25, offert) : au niveau 50, les deux sont actifs.
        var active = owners(EffectResolver.active(mort, EffectResolver.progress(50, Set.of(), c -> 0)));
        assertTrue(active.contains("appel_d_outre_tombe"));
        assertTrue(active.contains("ossuaire"));
    }

    private static SkillTree loadMort() throws Exception {
        try (InputStream in = EffectResolverTest.class.getResourceAsStream("/data/mc_rpg_mod/competences/mort.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return SkillTreeParser.parse("mort", root);
        }
    }
}
