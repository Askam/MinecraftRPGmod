package fr.needaname.mc_rpg_mod.skill;

/**
 * Levée quand un barème est invalide. Le message nomme toujours le fichier et le champ fautif :
 * un barème cassé est refusé, jamais chargé en devinant. Le chargeur la capte, journalise, et
 * n'enregistre pas l'arbre ; les tests l'attendent directement.
 */
public class SkillLoadException extends RuntimeException {
    public SkillLoadException(String message) {
        super(message);
    }
}
