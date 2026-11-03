package fr.needaname.mc_rpg_mod.skill;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.network.SyncSkillTreesPacket;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Envoie les barèmes au client via {@link OnDatapackSyncEvent}, qui couvre les deux cas : à la
 * connexion d'un joueur (joueur non nul) et après un {@code /reload} (joueur nul → tous).
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class SkillTreeSyncEvents {
    private SkillTreeSyncEvents() {
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncSkillTreesPacket packet = SyncSkillTreesPacket.of(SkillTreeManager.INSTANCE);
        if (event.getPlayer() != null) {
            RpgNetwork.sendTo(event.getPlayer(), packet);
        } else {
            event.getPlayerList().getPlayers().forEach(player -> RpgNetwork.sendTo(player, packet));
        }
    }
}
