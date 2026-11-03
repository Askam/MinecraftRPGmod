package fr.needaname.mc_rpg_mod.death;

import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class DeathEventHandler {
    private DeathEventHandler() {}

    /**
     * LOWEST et sans receiveCanceled : si un autre mod annule la mort (résurrection, etc.),
     * on ne l'a jamais vue et elle ne compte pas.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation cause = DeathCategoryManager.INSTANCE.classify(event.getSource());
        DeathStats.get(player).ifPresent(stats -> {
            stats.recordDeath(cause);
            RpgMod.LOGGER.debug("{} died ({}, damage type {}), total {}",
                    player.getGameProfile().getName(), cause, event.getSource().getMsgId(), stats.getTotal());
        });
        DeathSyncEvents.syncToSelfAndTrackers(player);
    }
}
