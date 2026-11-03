package fr.needaname.mc_rpg_mod.skill.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La formule {@code round(base × n^exponent)} fait foi. Avec base 60 et exposant 1,15 elle donne
 * 848 / 2 998 / 5 395 aux niveaux 10 / 30 / 50 — et NON les 846 / 2 940 / 5 220 cités en exemple
 * dans {@code docs/SCHEMA.md}. Cet écart est un point ouvert du propriétaire : on ne « corrige »
 * pas la formule pour coller aux exemples, on verrouille la formule par ce test.
 */
class XpCurveTest {
    private static final XpCurve CURVE = new XpCurve(60, 1.15);

    @Test
    void matchesFormulaNotSchemaExamples() {
        assertEquals(848L, CURVE.xpForLevel(10));
        assertEquals(2998L, CURVE.xpForLevel(30));
        assertEquals(5395L, CURVE.xpForLevel(50));
    }

    @Test
    void firstLevelsAreCheap() {
        assertEquals(60L, CURVE.xpForLevel(1));
        assertEquals(Math.round(60 * Math.pow(2, 1.15)), CURVE.xpForLevel(2));
    }
}
