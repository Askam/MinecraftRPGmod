package fr.needaname.mc_rpg_mod.skill.player;

import fr.needaname.mc_rpg_mod.skill.model.XpCurve;

/**
 * Accumulation d'XP et montée de niveau, à partir de la {@link XpCurve} du barème. Logique pure et
 * testable : aucune valeur d'équilibrage ici, tout vient de la courbe et de {@code max_level}.
 *
 * <p>L'XP retenue est l'XP <b>dans le niveau courant</b> ; atteindre {@code xpForLevel(niveau+1)}
 * consomme ce coût et fait monter d'un niveau, en reportant le reste. Au niveau maximum, l'XP
 * n'est plus accumulée (plafond).
 */
public final class SkillProgression {
    private SkillProgression() {
    }

    public record Result(int level, long xpIntoLevel, int levelsGained) {
    }

    public static Result gain(XpCurve curve, int maxLevel, int level, long xpIntoLevel, long amount) {
        if (level >= maxLevel) {
            return new Result(maxLevel, 0L, 0);
        }
        if (amount <= 0) {
            return new Result(level, xpIntoLevel, 0);
        }
        long xp = xpIntoLevel + amount;
        int gained = 0;
        while (level < maxLevel) {
            long needed = curve.xpForLevel(level + 1);
            if (needed <= 0 || xp < needed) {
                break;
            }
            xp -= needed;
            level++;
            gained++;
        }
        if (level >= maxLevel) {
            // Plafond : au niveau max, on ne conserve pas de reste d'XP.
            return new Result(maxLevel, 0L, gained);
        }
        return new Result(level, xp, gained);
    }
}
