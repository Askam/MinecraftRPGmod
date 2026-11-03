package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.death.DeathStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Serveur → client : compteurs de morts complets d'un joueur (soi-même ou un joueur visible). */
public record SyncDeathStatsPacket(int entityId, CompoundTag data) {

    public static SyncDeathStatsPacket of(Player player, DeathStats stats) {
        return new SyncDeathStatsPacket(player.getId(), stats.save());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeNbt(data);
    }

    public static SyncDeathStatsPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CompoundTag data = buf.readNbt();
        return new SyncDeathStatsPacket(entityId, data != null ? data : new CompoundTag());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        // Indirection pour ne jamais charger de classe client sur un serveur dédié.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDeathStats(this));
    }
}
