package fr.needaname.mc_rpg_mod.effect.server;

import java.util.OptionalLong;

/**
 * Recharges, en horodatage : on compare le {@code gameTime} du dernier usage (stocké dans
 * {@link fr.needaname.mc_rpg_mod.skill.player.PlayerSkills}) à {@code level.getGameTime()} courant.
 * Pas de décompte au tick, donc pas de boucle de tick, et la recharge survit à une déconnexion.
 *
 * <p>Logique pure et testable. Les durées ({@code cooldown_ticks}) viennent du barème.
 */
public final class Cooldowns {
    private Cooldowns() {
    }

    public static boolean isReady(OptionalLong lastUsed, long cooldownTicks, long now) {
        if (cooldownTicks <= 0 || lastUsed.isEmpty()) {
            return true;
        }
        return now - lastUsed.getAsLong() >= cooldownTicks;
    }

    public static long remaining(OptionalLong lastUsed, long cooldownTicks, long now) {
        if (cooldownTicks <= 0 || lastUsed.isEmpty()) {
            return 0;
        }
        return Math.max(0, cooldownTicks - (now - lastUsed.getAsLong()));
    }
}
