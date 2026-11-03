package fr.needaname.mc_rpg_mod.category;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.network.SyncCategoriesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Envoie les états de catégorie : à chaque connexion, et à tous après un changement. */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class CategorySyncEvents {
    private CategorySyncEvents() {
    }

    public static void syncToAll(MinecraftServer server) {
        SyncCategoriesPacket packet = SyncCategoriesPacket.of(CategorySavedData.get(server));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RpgNetwork.sendTo(player, packet);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            RpgNetwork.sendTo(player, SyncCategoriesPacket.of(CategorySavedData.get(player.getServer())));
        }
    }
}
