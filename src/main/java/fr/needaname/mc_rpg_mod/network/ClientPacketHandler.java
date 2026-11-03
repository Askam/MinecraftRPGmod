package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.death.DeathStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Uniquement chargée côté client (via DistExecutor). */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handleDeathStats(SyncDeathStatsPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(packet.entityId()) instanceof Player player) {
            DeathStats.get(player).ifPresent(stats -> stats.load(packet.data()));
        }
    }
}
