package fr.needaname.mc_rpg_mod.appearance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * État d'apparence par entité, côté client, alimenté par {@code SyncAppearancePacket}. Les
 * {@code RenderLayer} le lisent pour décider quoi afficher — y compris sur un AUTRE joueur, puisque
 * le niveau de Mort et les compteurs ne sont pas synchronisés individuellement aux observateurs :
 * le serveur calcule ici des drapeaux d'apparence et les envoie aux observateurs.
 */
public final class ClientAppearances {
    public record State(float aura, boolean ash, boolean spectral) {
        public static final State NONE = new State(0f, false, false);
    }

    private static final Map<Integer, State> BY_ENTITY = new ConcurrentHashMap<>();

    private ClientAppearances() {
    }

    public static void set(int entityId, State state) {
        if (state.aura() <= 0 && !state.ash() && !state.spectral()) {
            BY_ENTITY.remove(entityId);
        } else {
            BY_ENTITY.put(entityId, state);
        }
    }

    public static State get(int entityId) {
        return BY_ENTITY.getOrDefault(entityId, State.NONE);
    }
}
