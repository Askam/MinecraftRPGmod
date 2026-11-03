package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.skill.player.SkillActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → serveur : assigne (ou vide, si nodeId vide) un pouvoir à un emplacement R/G/X. */
public record AssignPowerPacket(String slot, String nodeId) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(slot, 8);
        buf.writeUtf(nodeId, 128);
    }

    public static AssignPowerPacket decode(FriendlyByteBuf buf) {
        return new AssignPowerPacket(buf.readUtf(8), buf.readUtf(128));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            SkillActions.assignPower(sender, slot, nodeId);
        }
    }
}
