package fr.needaname.mc_rpg_mod.client;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.death.DeathStats;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /rpgmodclient deaths : affiche ce que le CLIENT sait des joueurs chargés, pour vérifier la synchro. */
@Mod.EventBusSubscriber(modid = RpgMod.MODID, value = Dist.CLIENT)
public final class ClientDebugCommands {
    private ClientDebugCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(RpgMod.MODID + "client")
                .then(Commands.literal("deaths").executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) {
                        return 0;
                    }
                    for (Player player : mc.level.players()) {
                        int total = DeathStats.get(player).map(DeathStats::getTotal).orElse(-1);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "[client] " + player.getGameProfile().getName() + " : " + total + " mort(s)"), false);
                    }
                    return 1;
                })));
    }
}
