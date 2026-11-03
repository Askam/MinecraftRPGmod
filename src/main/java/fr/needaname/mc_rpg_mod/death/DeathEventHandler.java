package fr.needaname.mc_rpg_mod.death;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Incrémente les compteurs de mort à partir des causes déclarées dans {@code mort.json}
 * ({@code counters.per_cause}), via {@link DeathCauseClassifier}.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class DeathEventHandler {
    private static final String DEATH_TREE = "mort";

    private DeathEventHandler() {
    }

    /**
     * LOWEST et sans receiveCanceled : si un autre mod annule la mort (résurrection, etc.),
     * on ne l'a jamais vue et elle ne compte pas.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String cause = SkillTreeManager.INSTANCE.byId(DEATH_TREE)
                .flatMap(SkillTree::counters)
                .map(counters -> DeathCauseClassifier.classify(counters.perCause(), event.getSource()))
                .orElse(DeathCauseClassifier.FALLBACK);
        DeathStats.get(player).ifPresent(stats -> {
            stats.recordDeath(cause);
            RpgMod.LOGGER.debug("{} died ({}, damage type {}), total {}",
                    player.getGameProfile().getName(), cause, event.getSource().getMsgId(), stats.getTotal());
        });
        DeathSyncEvents.syncToSelfAndTrackers(player);
    }
}
