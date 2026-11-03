package fr.needaname.mc_rpg_mod.skill.model;

import com.google.gson.JsonObject;

import java.util.Optional;

/**
 * Le déclenchement d'un nœud. {@code on} n'existe que pour {@link TriggerFamily#REFLEX}
 * (l'événement observé) ; {@code require} porte les conditions supplémentaires, brutes,
 * interprétées plus tard selon l'événement.
 */
public record Trigger(TriggerFamily family, Optional<String> on, Optional<JsonObject> require) {
}
