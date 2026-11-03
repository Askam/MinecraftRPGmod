package fr.needaname.mc_rpg_mod.skill.player;

import fr.needaname.mc_rpg_mod.skill.model.XpCurve;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillProgressionTest {
    private static final XpCurve CURVE = new XpCurve(60, 1.15);
    private static final int MAX = 50;

    @Test
    void notEnoughForALevel() {
        SkillProgression.Result r = SkillProgression.gain(CURVE, MAX, 0, 0, 59);
        assertEquals(0, r.level());
        assertEquals(59, r.xpIntoLevel());
        assertEquals(0, r.levelsGained());
    }

    @Test
    void exactlyOneLevel() {
        SkillProgression.Result r = SkillProgression.gain(CURVE, MAX, 0, 0, CURVE.xpForLevel(1));
        assertEquals(1, r.level());
        assertEquals(0, r.xpIntoLevel());
        assertEquals(1, r.levelsGained());
    }

    @Test
    void multipleLevelsAtOnceCarryRemainder() {
        long toTwo = CURVE.xpForLevel(1) + CURVE.xpForLevel(2);
        SkillProgression.Result r = SkillProgression.gain(CURVE, MAX, 0, 0, toTwo + 5);
        assertEquals(2, r.level());
        assertEquals(5, r.xpIntoLevel());
        assertEquals(2, r.levelsGained());
    }

    @Test
    void accumulatesOntoExistingProgress() {
        long need = CURVE.xpForLevel(1);
        SkillProgression.Result r = SkillProgression.gain(CURVE, MAX, 0, need - 10, 10);
        assertEquals(1, r.level());
        assertEquals(0, r.xpIntoLevel());
    }

    @Test
    void cappedAtMaxLevel() {
        SkillProgression.Result atMax = SkillProgression.gain(CURVE, MAX, MAX, 0, 100000);
        assertEquals(MAX, atMax.level());
        assertEquals(0, atMax.xpIntoLevel());
        assertEquals(0, atMax.levelsGained());

        // Depuis l'avant-dernier niveau, un gros gain plafonne sans reste.
        SkillProgression.Result toMax = SkillProgression.gain(CURVE, MAX, MAX - 1, 0, 100000);
        assertEquals(MAX, toMax.level());
        assertEquals(0, toMax.xpIntoLevel());
        assertEquals(1, toMax.levelsGained());
    }
}
