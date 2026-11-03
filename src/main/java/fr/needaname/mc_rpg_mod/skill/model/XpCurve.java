package fr.needaname.mc_rpg_mod.skill.model;

/**
 * Courbe d'XP d'un arbre : {@code round(base × niveau^exponent)} pour passer au niveau donné.
 * La formule fait foi (voir le point ouvert sur {@code docs/SCHEMA.md} dans {@code CLAUDE.md}) :
 * avec base 60 et exposant 1,15, elle donne 848 / 2 998 / 5 395 aux niveaux 10 / 30 / 50.
 */
public record XpCurve(double base, double exponent) {
    /** XP nécessaire pour atteindre {@code level}. */
    public long xpForLevel(int level) {
        return Math.round(base * Math.pow(level, exponent));
    }
}
