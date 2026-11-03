package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.effect.server.HoldKeyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → serveur : l'état de la touche de récolte maintenue. Le serveur ne peut pas observer le
 * maintien, d'où ce paquet ; il est validé côté serveur (expéditeur authentifié par la connexion) et
 * alimente {@link HoldKeyState}, que lit {@code break_shape}.
 */
public record HoldKeyPacket(boolean held) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(held);
    }

    public static HoldKeyPacket decode(FriendlyByteBuf buf) {
        return new HoldKeyPacket(buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            HoldKeyState.set(sender, held);
        }
    }
}
