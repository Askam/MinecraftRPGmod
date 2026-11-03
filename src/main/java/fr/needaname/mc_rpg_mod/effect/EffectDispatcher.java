package fr.needaname.mc_rpg_mod.effect;

import fr.needaname.mc_rpg_mod.skill.model.EffectType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * La table « type d'effet → code qui l'applique ». Pièce centrale : les blocs d'effets serveur
 * (6), de break_shape (7) et les pouvoirs y enregistrent leur gestionnaire, et les événements de
 * jeu la consultent pour router un effet vers son code.
 *
 * <p>Aucun gestionnaire n'est enregistré ici au bloc 4 : la table est le point de branchement, les
 * blocs suivants la remplissent. Un double enregistrement pour un même type est une erreur de
 * programmation (deux chemins de code pour un type) et lève immédiatement.
 */
public final class EffectDispatcher {
    public static final EffectDispatcher INSTANCE = new EffectDispatcher();

    private final Map<EffectType, EffectHandler> handlers = new EnumMap<>(EffectType.class);

    public void register(EffectHandler handler) {
        if (handlers.putIfAbsent(handler.type(), handler) != null) {
            throw new IllegalStateException("Gestionnaire déjà enregistré pour le type " + handler.type());
        }
    }

    public Optional<EffectHandler> handler(EffectType type) {
        return Optional.ofNullable(handlers.get(type));
    }

    public boolean isHandled(EffectType type) {
        return handlers.containsKey(type);
    }

    public Set<EffectType> handledTypes() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    /** Vide la table (usage interne / tests). */
    public void clear() {
        handlers.clear();
    }
}
