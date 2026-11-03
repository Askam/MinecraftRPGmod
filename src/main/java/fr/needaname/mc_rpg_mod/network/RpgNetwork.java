package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RpgNetwork {
    // À incrémenter à chaque changement de format d'un paquet : un client désynchronisé sera refusé à la connexion.
    // v2 : SyncSkillsPacket + HoldKeyPacket + TriggerPowerPacket.
    // v3 : SyncCategoriesPacket et OpenAdminPacket (état des catégories).
    private static final String PROTOCOL_VERSION = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(RpgMod.MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private RpgNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncDeathStatsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDeathStatsPacket::encode)
                .decoder(SyncDeathStatsPacket::decode)
                .consumerMainThread(SyncDeathStatsPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSkillsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillsPacket::encode)
                .decoder(SyncSkillsPacket::decode)
                .consumerMainThread(SyncSkillsPacket::handle)
                .add();
        CHANNEL.messageBuilder(HoldKeyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(HoldKeyPacket::encode)
                .decoder(HoldKeyPacket::decode)
                .consumerMainThread(HoldKeyPacket::handle)
                .add();
        CHANNEL.messageBuilder(TriggerPowerPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TriggerPowerPacket::encode)
                .decoder(TriggerPowerPacket::decode)
                .consumerMainThread(TriggerPowerPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncCategoriesPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCategoriesPacket::encode)
                .decoder(SyncCategoriesPacket::decode)
                .consumerMainThread(SyncCategoriesPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenAdminPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenAdminPacket::encode)
                .decoder(OpenAdminPacket::decode)
                .consumerMainThread(OpenAdminPacket::handle)
                .add();
    }

    public static void sendTo(ServerPlayer receiver, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver), packet);
    }

    public static void sendToTrackingAndSelf(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    /** Côté client uniquement (déclencheurs de maintien / de pouvoir) : envoi au serveur. */
    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
