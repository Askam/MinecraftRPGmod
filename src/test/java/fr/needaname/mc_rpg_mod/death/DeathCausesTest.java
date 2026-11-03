package fr.needaname.mc_rpg_mod.death;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.needaname.mc_rpg_mod.skill.SkillTreeParser;
import fr.needaname.mc_rpg_mod.skill.model.CauseCounter;
import fr.needaname.mc_rpg_mod.skill.model.Counters;
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
 * Vérifie, sur les données, la forme des causes de mort — et documente le bug connu du compteur
 * {@code creeper}. On ne peut pas construire de {@code DamageSource} sans démarrer Minecraft, donc
 * le classement effectif (disjoint, première cause satisfaite) et ce bug restent à vérifier en jeu :
 * mourir d'un creeper, respawn, {@code /mc_rpg_mod deaths}.
 */
class DeathCausesTest {

    private static List<CauseCounter> deathCauses() throws Exception {
        try (InputStream in = DeathCausesTest.class.getResourceAsStream("/data/mc_rpg_mod/competences/mort.json")) {
            assertNotNull(in, "mort.json introuvable sur le classpath");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            SkillTree mort = SkillTreeParser.parse("mort", root);
            return mort.counters().map(Counters::perCause).orElseThrow();
        }
    }

    private static CauseCounter cause(List<CauseCounter> causes, String id) {
        return causes.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void causeOrderIsDisjointFriendly() throws Exception {
        List<CauseCounter> causes = deathCauses();
        // creeper (spécifique) est déclaré AVANT explosion (générique) : avec un classement disjoint
        // « première cause satisfaite », une mort de creeper irait dans creeper, pas dans explosion.
        List<String> ids = causes.stream().map(CauseCounter::id).toList();
        assertTrue(ids.indexOf("creeper") < ids.indexOf("explosion"),
                "creeper doit précéder explosion pour que le classement disjoint ait un sens");
    }

    /**
     * BUG CONNU (point ouvert, décision du propriétaire — NE PAS corriger le barème) :
     * le compteur {@code creeper} est déclaré avec {@code damage_type: minecraft:explosion}, alors
     * qu'une explosion de creeper porte le type {@code minecraft:player_explosion} (dès que l'attaquant
     * et la source indirecte sont non nuls). Tel quel, ce compteur ne s'incrémentera JAMAIS : une mort
     * de creeper tombera dans {@code explosion} (tag {@code minecraft:is_explosion}) seulement si
     * {@code excluding_source_entity} ne l'écarte pas — or il l'écarte. Donc une mort de creeper
     * finit dans {@link DeathCauseClassifier#FALLBACK}. Ce test fige l'état actuel ; le jour où le
     * propriétaire tranche, il met à jour ce test en même temps que le barème.
     */
    @Test
    void creeperCounterIsMisdeclared_knownOpenIssue() throws Exception {
        List<CauseCounter> causes = deathCauses();
        CauseCounter creeper = cause(causes, "creeper");
        assertEquals("minecraft:explosion", creeper.damageType().orElseThrow(),
                "état actuel du barème (bug connu : devrait viser player_explosion pour fonctionner)");
        assertEquals("minecraft:creeper", creeper.sourceEntity().orElseThrow());

        CauseCounter explosion = cause(causes, "explosion");
        assertEquals("minecraft:is_explosion", explosion.damageTypeTag().orElseThrow());
        assertEquals("minecraft:creeper", explosion.excludingSourceEntity().orElseThrow());
    }
}
