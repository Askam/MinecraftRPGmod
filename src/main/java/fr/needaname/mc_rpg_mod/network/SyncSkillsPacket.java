package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.skill.player.PlayerSkills;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Serveur → client : la capability {@link PlayerSkills} complète d'un joueur (niveaux, XP, nœuds,
 * emplacements de pouvoirs, recharges), pour alimenter l'interface. Envoyée au joueur pour lui-même
 * (sa propre progression ; les autres joueurs n'en ont pas besoin).
 */
public record SyncSkillsPacket(int entityId, CompoundTag data) {

    public static SyncSkillsPacket of(Player player, PlayerSkills skills) {
        return new SyncSkillsPacket(player.getId(), skills.save());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeNbt(data);
    }

    public static SyncSkillsPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CompoundTag data = buf.readNbt();
        return new SyncSkillsPacket(entityId, data != null ? data : new CompoundTag());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        // Indirection pour ne jamais charger de classe client sur un serveur dédié.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSkills(this));
    }
}
