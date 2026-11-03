package fr.needaname.mc_rpg_mod.effect;

import fr.needaname.mc_rpg_mod.skill.model.Effect;
import fr.needaname.mc_rpg_mod.skill.model.Trigger;

import java.util.Optional;

/**
 * Un effet actuellement <b>possédé</b> par un joueur, le nœud ou la branche d'où il vient, et le
 * {@code trigger} de ce nœud (permettant au consommateur de distinguer un effet permanent d'un effet
 * conditionné à un reflex / un maintien / un pouvoir).
 *
 * <p>« Actif » signifie ici « débloqué / possédé », pas « en train de se produire » : un effet porté
 * par un {@code trigger} figure dès que le nœud est pris ; c'est le code du déclenchement qui décide
 * quand il s'applique. Les effets permanents (attribute, air…), eux, s'appliquent simplement parce
 * qu'ils sont dans cette liste.
 */
public record ActiveEffect(String ownerId, Effect effect, boolean fromBranch, Optional<Trigger> trigger) {
}
