package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.skill.player.SkillActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → serveur : le joueur choisit un nœud (palier exclusif) depuis l'écran d'arbre. */
public record ChooseNodePacket(String nodeId) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(nodeId, 128);
    }

    public static ChooseNodePacket decode(FriendlyByteBuf buf) {
        return new ChooseNodePacket(buf.readUtf(128));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            SkillActions.takeNode(sender, nodeId);
        }
    }
}
