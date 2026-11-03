package fr.needaname.mc_rpg_mod.effect.server;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * État transitoire de la touche de maintien (la touche de récolte, « C ») par joueur, côté serveur.
 * C'est ce que les nœuds à trigger {@code hold} (Veinage, Abattage, Moisson, Carrier) consultent.
 *
 * <p>L'état réel arrive par un paquet client validé côté serveur (bloc 8) : il ne se devine pas. Ici
 * on ne fournit que le registre ; par défaut la touche est relâchée. Transitoire (jamais en NBT),
 * nettoyé à la déconnexion par le code réseau.
 */
public final class HoldKeyState {
    private static final Map<UUID, Boolean> HELD = new ConcurrentHashMap<>();

    private HoldKeyState() {
    }

    public static boolean isHeld(ServerPlayer player) {
        return HELD.getOrDefault(player.getUUID(), false);
    }

    public static void set(ServerPlayer player, boolean held) {
        if (held) {
            HELD.put(player.getUUID(), true);
        } else {
            HELD.remove(player.getUUID());
        }
    }

    public static void clear(ServerPlayer player) {
        HELD.remove(player.getUUID());
    }
}
