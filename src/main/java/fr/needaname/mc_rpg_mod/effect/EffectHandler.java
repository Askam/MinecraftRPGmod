package fr.needaname.mc_rpg_mod.effect;

import fr.needaname.mc_rpg_mod.skill.model.EffectType;

/**
 * Le code qui applique un type d'effet du catalogue. Un gestionnaire par type, enregistré dans la
 * table {@link EffectDispatcher}. La façon d'appliquer dépend du type (modificateur permanent, hook
 * de dégâts, butin, maintien de touche…), donc les contrats spécifiques sont des sous-interfaces
 * introduites par les blocs qui les implémentent ; ce contrat de base ne porte que l'identité du type.
 */
public interface EffectHandler {
    EffectType type();
}
