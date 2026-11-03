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
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Déclenchement serveur d'un reflex que le serveur ne peut pas observer lui-même : les deux gestes
 * {@code client_*} (double saut, double appui de direction). Le client envoie l'intention, le serveur
 * vérifie la possession du nœud (reflex, {@code on} correspondant) et la recharge. L'exécution de
 * l'ability (impulse, dash) dépasse le cadre des blocs livrés : point d'ancrage journalisé.
 */
public final class ReflexActivation {
    private ReflexActivation() {
    }

    public static void activate(ServerPlayer player, String reflexOn) {
        PlayerSkills skills = PlayerSkillsProvider.get(player).orElse(null);
        if (skills == null) {
            return;
        }
        List<ActiveEffect> matching = activeReflexEffects(player, skills, reflexOn);
        if (matching.isEmpty()) {
            return; // le joueur ne possède pas (ou pas encore) de nœud pour ce reflex
        }
        String nodeId = matching.get(0).ownerId();
        long cooldownTicks = matching.stream()
                .filter(active -> active.effect().type() == EffectType.ABILITY)
                .map(active -> active.effect().fields())
                .filter(fields -> fields.has("cooldown_ticks"))
                .mapToLong(fields -> fields.get("cooldown_ticks").getAsLong())
                .findFirst()
                .orElse(0L);
        long now = player.level().getGameTime();
        if (!Cooldowns.isReady(skills.lastUsed(nodeId), cooldownTicks, now)) {
            return;
        }
        if (cooldownTicks > 0) {
            skills.setLastUsed(nodeId, now);
        }
        RpgMod.LOGGER.debug("{} déclenche le reflex {} (nœud {})",
                player.getGameProfile().getName(), reflexOn, nodeId);
        // Exécution de l'ability : point d'ancrage (hors des blocs livrés).
    }

    private static List<ActiveEffect> activeReflexEffects(ServerPlayer player, PlayerSkills skills, String reflexOn) {
        ToIntFunction<String> causeCount = DeathStats.get(player)
                .<ToIntFunction<String>>map(stats -> stats::getCount)
                .orElse(cause -> 0);
        List<ActiveEffect> result = new ArrayList<>();
        for (SkillTree tree : SkillTreeManager.INSTANCE.trees().values()) {
            EffectResolver.Progress progress =
                    EffectResolver.progress(skills.level(tree.id()), skills.nodes(), causeCount);
            for (ActiveEffect active : EffectResolver.active(tree, progress)) {
                boolean matches = active.trigger()
                        .filter(trigger -> trigger.family() == TriggerFamily.REFLEX)
                        .flatMap(trigger -> trigger.on())
                        .filter(on -> on.equals(reflexOn))
                        .isPresent();
                if (matches) {
                    result.add(active);
                }
            }
        }
        return result;
    }
}
