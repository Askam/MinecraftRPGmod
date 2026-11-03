package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.effect.server.PowerActivation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → serveur : le joueur active l'emplacement de pouvoir {@code slot} (R / G / X). Validé côté
 * serveur par {@link PowerActivation} (possession + recharge au bloc 9) ; jamais exécuté côté client.
 */
public record TriggerPowerPacket(String slot) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(slot, 8);
    }

    public static TriggerPowerPacket decode(FriendlyByteBuf buf) {
        return new TriggerPowerPacket(buf.readUtf(8));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            PowerActivation.activate(sender, slot);
        }
    }
}
