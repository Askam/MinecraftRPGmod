package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.effect.server.ReflexActivation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → serveur : un geste que le serveur ne voit pas (double saut en plein vol, double appui de
 * direction), identifié par la valeur {@code on} du trigger ({@code client_jump_airborne},
 * {@code client_double_tap_direction}). Toujours validé côté serveur ({@link ReflexActivation}).
 */
public record ClientReflexPacket(String on) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(on, 64);
    }

    public static ClientReflexPacket decode(FriendlyByteBuf buf) {
        return new ClientReflexPacket(buf.readUtf(64));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            ReflexActivation.activate(sender, on);
        }
    }
}
