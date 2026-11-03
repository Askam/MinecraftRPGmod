package fr.needaname.mc_rpg_mod.skill.model;

/**
 * XP de l'arbre Mort : un tarif fixe {@code flat}, débloqué seulement après
 * {@code requiresActiveSecondsSinceLastDeath} secondes de jeu <b>actif</b> depuis la dernière mort
 * (le garde-fou contre le farm de morts). Aucune modulation par le temps de survie.
 */
public record XpOnDeath(int flat, int requiresActiveSecondsSinceLastDeath) {
}
