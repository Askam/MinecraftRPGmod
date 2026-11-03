package fr.needaname.mc_rpg_mod.appearance;

import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Déclenche l'envoi des drapeaux d'apparence aux bons moments : connexion, respawn, changement de
 * dimension (l'entité du joueur est recréée), et quand un observateur commence à voir un joueur.
 * Le niveau de Mort change aussi via {@code SkillXp}, qui appelle alors {@link AppearanceSync#sendFor}.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class AppearanceEvents {
    private AppearanceEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AppearanceSync.sendFor(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AppearanceSync.sendFor(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AppearanceSync.sendFor(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            AppearanceSync.sendTo(viewer, target);
        }
    }
}
