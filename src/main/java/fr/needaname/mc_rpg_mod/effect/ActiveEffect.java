package fr.needaname.mc_rpg_mod.effect;

import fr.needaname.mc_rpg_mod.skill.model.Effect;

/**
 * Un effet actuellement <b>possédé</b> par un joueur, et le nœud ou la branche d'où il vient.
 *
 * <p>« Actif » signifie ici « débloqué / possédé », pas « en train de se produire » : un effet porté
 * par un {@code trigger} (reflex, hold, power) figure dès que le nœud est pris ; c'est le code du
 * déclenchement qui décide quand il s'applique. Les effets permanents (attribute, air…), eux,
 * s'appliquent simplement parce qu'ils sont dans cette liste.
 */
public record ActiveEffect(String ownerId, Effect effect, boolean fromBranch) {
}
