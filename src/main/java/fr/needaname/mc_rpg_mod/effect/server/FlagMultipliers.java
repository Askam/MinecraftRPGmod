package fr.needaname.mc_rpg_mod.effect.server;

import com.google.gson.JsonObject;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;

import java.util.List;

/**
 * Chemin de code unique et générique pour les drapeaux-multiplicateurs : 14 des 36 {@code flag}
 * portent un champ {@code multiplier} sur une grandeur vanilla nommée (dig_speed, durability,
 * draw_speed, food_nutrition…). Plutôt qu'un gestionnaire par nom, le code consommateur demande
 * ici le produit des multiplicateurs actifs pour un nom de drapeau donné.
 *
 * <p>C'est volontairement structuré ainsi pour qu'un éventuel repli en type {@code tweak
 * { quantity, multiplier }} (décision NON prise par le propriétaire) reste mécanique : il suffirait
 * de changer la source des multiplicateurs ici, sans toucher aux appelants.
 */
public final class FlagMultipliers {
    private FlagMultipliers() {
    }

    /** Produit des {@code multiplier} de tous les effets flag actifs nommés {@code flagName}. */
    public static double product(List<ActiveEffect> activeFlagEffects, String flagName) {
        double product = 1.0;
        for (ActiveEffect active : activeFlagEffects) {
            JsonObject fields = active.effect().fields();
            if (fields.has("flag") && flagName.equals(fields.get("flag").getAsString()) && fields.has("multiplier")) {
                product *= fields.get("multiplier").getAsDouble();
            }
        }
        return product;
    }
}
