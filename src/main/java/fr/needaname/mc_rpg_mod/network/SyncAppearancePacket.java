package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.appearance.ClientAppearances;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Serveur → client : drapeaux d'apparence d'un joueur (intensité d'aura, peau de cendre, armure
 * spectrale), calculés côté serveur depuis sa progression et ses compteurs, pour que les observateurs
 * puissent les afficher sans connaître sa progression complète.
 */
public record SyncAppearancePacket(int entityId, float aura, boolean ash, boolean spectral) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeFloat(aura);
        buf.writeBoolean(ash);
        buf.writeBoolean(spectral);
    }

    public static SyncAppearancePacket decode(FriendlyByteBuf buf) {
        return new SyncAppearancePacket(buf.readVarInt(), buf.readFloat(), buf.readBoolean(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientAppearances.set(entityId, new ClientAppearances.State(aura, ash, spectral)));
    }
}
