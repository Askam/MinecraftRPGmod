package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.category.CategoryState;
import fr.needaname.mc_rpg_mod.category.CategorySavedData;
import fr.needaname.mc_rpg_mod.category.CategorySyncEvents;
import fr.needaname.mc_rpg_mod.effect.server.AttributeEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → serveur : changement d'état d'une catégorie depuis l'écran d'administration. Le serveur
 * REVÉRIFIE la permission (niveau 2) avant d'appliquer — un client ne décide jamais seul.
 */
public record SetCategoryPacket(String treeId, String state) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(treeId, 128);
        buf.writeUtf(state, 16);
    }

    public static SetCategoryPacket decode(FriendlyByteBuf buf) {
        return new SetCategoryPacket(buf.readUtf(128), buf.readUtf(16));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender == null || !sender.hasPermissions(2)) {
            return; // revérification serveur : seul un opérateur peut changer l'état
        }
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return;
        }
        CategorySavedData.get(server).setState(treeId, CategoryState.byId(state));
        server.getPlayerList().getPlayers().forEach(AttributeEffects::reapply);
        CategorySyncEvents.syncToAll(server);
    }
}
