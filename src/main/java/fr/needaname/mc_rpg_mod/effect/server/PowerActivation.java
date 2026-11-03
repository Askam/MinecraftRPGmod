package fr.needaname.mc_rpg_mod.effect.server;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.death.DeathStats;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.effect.EffectResolver;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.TriggerFamily;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkills;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import fr.needaname.mc_rpg_mod.skill.player.SkillSyncEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Déclenchement d'un pouvoir depuis un emplacement (R/G/X), côté serveur, après vérification que le
 * joueur possède un nœud de famille {@code power} actif dans cet emplacement ET que sa recharge est
 * écoulée ({@link Cooldowns}, horodatage {@code gameTime}). Au succès, on repose la recharge et on
 * renvoie la capability au client.
 *
 * <p>L'exécution de l'ability elle-même (summon_ally, whirlwind, grow_crops…) dépasse le cadre des
 * neuf blocs : point d'ancrage marqué ci-dessous. La recharge, elle, est désormais effective.
 */
public final class PowerActivation {
    private PowerActivation() {
    }

    public static void activate(ServerPlayer player, String slot) {
        PlayerSkills skills = PlayerSkillsProvider.get(player).orElse(null);
        if (skills == null) {
            return;
        }
        String nodeId = skills.powerSlot(slot).orElse(null);
        if (nodeId == null) {
            return; // aucun pouvoir assigné à cet emplacement
        }
        List<ActiveEffect> owned = activeEffectsOfOwner(player, skills, nodeId);
        boolean ownsPower = owned.stream()
                .anyMatch(active -> active.trigger().map(t -> t.family() == TriggerFamily.POWER).orElse(false));
        if (!ownsPower) {
            RpgMod.LOGGER.debug("{} : pouvoir {} non possédé/actif, ignoré",
                    player.getGameProfile().getName(), nodeId);
            return;
        }

        long cooldownTicks = cooldownTicksOf(owned);
        long now = player.level().getGameTime();
        if (!Cooldowns.isReady(skills.lastUsed(nodeId), cooldownTicks, now)) {
            RpgMod.LOGGER.debug("{} : {} en recharge ({} ticks restants)",
                    player.getGameProfile().getName(), nodeId,
                    Cooldowns.remaining(skills.lastUsed(nodeId), cooldownTicks, now));
            return;
        }

        skills.setLastUsed(nodeId, now);
        SkillSyncEvents.syncTo(player);
        RpgMod.LOGGER.debug("{} déclenche le pouvoir {} (emplacement {}), recharge {} ticks",
                player.getGameProfile().getName(), nodeId, slot, cooldownTicks);
        // Exécution de l'ability : point d'ancrage (hors des neuf blocs).
    }

    private static long cooldownTicksOf(List<ActiveEffect> ownerEffects) {
        return ownerEffects.stream()
                .filter(active -> active.effect().type() == EffectType.ABILITY)
                .map(active -> active.effect().fields())
                .filter(fields -> fields.has("cooldown_ticks"))
                .mapToLong(fields -> fields.get("cooldown_ticks").getAsLong())
                .findFirst()
                .orElse(0L);
    }

    private static List<ActiveEffect> activeEffectsOfOwner(ServerPlayer player, PlayerSkills skills, String nodeId) {
        ToIntFunction<String> causeCount = DeathStats.get(player)
                .<ToIntFunction<String>>map(stats -> stats::getCount)
                .orElse(cause -> 0);
        List<ActiveEffect> result = new ArrayList<>();
        for (SkillTree tree : SkillTreeManager.INSTANCE.trees().values()) {
            EffectResolver.Progress progress =
                    EffectResolver.progress(skills.level(tree.id()), skills.nodes(), causeCount);
            for (ActiveEffect active : EffectResolver.active(tree, progress)) {
                if (active.ownerId().equals(nodeId)) {
                    result.add(active);
                }
            }
        }
        return result;
    }
}
