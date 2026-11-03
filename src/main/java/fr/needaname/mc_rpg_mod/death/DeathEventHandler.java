package fr.needaname.mc_rpg_mod.death;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.XpOnDeath;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import fr.needaname.mc_rpg_mod.skill.player.SkillXp;
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
    private static final int TICKS_PER_SECOND = 20;

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
        grantDeathXp(player);
        DeathSyncEvents.syncToSelfAndTrackers(player);
    }

    /**
     * XP de l'arbre Mort : tarif fixe {@code flat}, débloqué seulement si le joueur a accumulé assez
     * de secondes de jeu actif depuis sa dernière mort (garde-fou contre le farm de morts). Le
     * compteur de temps actif est remis à zéro à chaque mort, qu'il y ait eu gain ou non.
     */
    private static void grantDeathXp(ServerPlayer player) {
        XpOnDeath xpOnDeath = SkillTreeManager.INSTANCE.byId(DEATH_TREE)
                .flatMap(SkillTree::xpOnDeath)
                .orElse(null);
        if (xpOnDeath == null) {
            return;
        }
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            int activeSeconds = skills.activeTicksSinceDeath() / TICKS_PER_SECOND;
            if (activeSeconds >= xpOnDeath.requiresActiveSecondsSinceLastDeath()) {
                SkillXp.grant(player, DEATH_TREE, xpOnDeath.flat());
            }
            skills.resetActiveTicksSinceDeath();
        });
    }
}
