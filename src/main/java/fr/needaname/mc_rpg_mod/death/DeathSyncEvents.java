package fr.needaname.mc_rpg_mod.death;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.network.SyncDeathStatsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Le client recrée son entité joueur (vierge) à la connexion, au respawn et au changement de dimension :
 * on renvoie les compteurs à chaque fois. Les autres joueurs les reçoivent quand ils commencent à le voir.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class DeathSyncEvents {
    private DeathSyncEvents() {}

    public static void syncToSelfAndTrackers(ServerPlayer player) {
        DeathStats.get(player).ifPresent(stats ->
                RpgNetwork.sendToTrackingAndSelf(player, SyncDeathStatsPacket.of(player, stats)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToSelfAndTrackers(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToSelfAndTrackers(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToSelfAndTrackers(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer tracker) {
            DeathStats.get(target).ifPresent(stats ->
                    RpgNetwork.sendTo(tracker, SyncDeathStatsPacket.of(target, stats)));
        }
    }
}
