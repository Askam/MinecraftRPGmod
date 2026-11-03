package fr.needaname.mc_rpg_mod.skill.player;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.effect.server.HoldKeyState;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.network.SyncSkillsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renvoie la capability {@link PlayerSkills} au joueur quand son entité client est (re)créée :
 * connexion, respawn, changement de dimension — et après chaque gain d'XP (via {@link SkillXp}).
 * Nettoie aussi l'état de maintien à la déconnexion.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class SkillSyncEvents {
    private SkillSyncEvents() {
    }

    public static void syncTo(ServerPlayer player) {
        PlayerSkillsProvider.get(player).ifPresent(skills ->
                RpgNetwork.sendTo(player, SyncSkillsPacket.of(player, skills)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HoldKeyState.clear(player);
        }
    }
}
