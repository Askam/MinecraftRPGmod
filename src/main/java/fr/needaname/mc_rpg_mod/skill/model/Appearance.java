package fr.needaname.mc_rpg_mod.skill.model;

import com.google.gson.JsonObject;

/**
 * Une couche de rendu attachée à un niveau, invisible dans l'arbre (arbre Mort). N'a aucun effet
 * de jeu ; elle impose seulement que le client connaisse le niveau. Les champs au-delà de
 * {@code at_level}/{@code layer} (ex. {@code intensity}) restent bruts : le rendu n'est pas de ce côté.
 */
public record Appearance(int atLevel, String layer, JsonObject fields) {
}
