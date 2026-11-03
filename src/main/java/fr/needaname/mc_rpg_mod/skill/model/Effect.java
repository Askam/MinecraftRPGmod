package fr.needaname.mc_rpg_mod.skill.model;

import com.google.gson.JsonObject;

/**
 * Un effet : son {@link EffectType} (validé au chargement, catalogue fermé) et l'objet JSON brut
 * de ses champs. Le répartiteur d'effets (bloc 4) lira {@code fields} selon le type ; le chargeur
 * ne garantit que le type et les quelques invariants de {@code tools/valider.py}.
 */
public record Effect(EffectType type, JsonObject fields) {
}
