package fr.needaname.mc_rpg_mod.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Serveur → client : ouvre l'écran d'administration, après vérification de la permission côté
 * serveur (un client ne doit jamais l'ouvrir de lui-même). Porte les états et les comptes par arbre.
 * L'écran lui-même relève du handoff 3 ; côté client la réception est pour l'instant un point
 * d'ancrage (journalisé).
 */
public record OpenAdminPacket(CompoundTag data) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public static OpenAdminPacket decode(FriendlyByteBuf buf) {
        CompoundTag data = buf.readNbt();
        return new OpenAdminPacket(data != null ? data : new CompoundTag());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenAdmin(this));
    }
}
