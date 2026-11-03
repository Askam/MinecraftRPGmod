package fr.needaname.mc_rpg_mod.effect.server;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.death.DeathStats;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.effect.EffectResolver;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.TriggerFamily;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkills;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.ToIntFunction;

/**
 * Déclenchement d'un pouvoir depuis un emplacement (R/G/X), côté serveur, après vérification que le
 * joueur possède bien un nœud de famille {@code power} actif dans cet emplacement. La recharge est
 * ajoutée au bloc 9 ; l'exécution de l'ability elle-même (summon_ally, whirlwind…) dépasse le cadre
 * des neuf blocs — ici on valide et on laisse un point d'ancrage.
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
        if (!ownsActivePower(player, skills, nodeId)) {
            RpgMod.LOGGER.debug("{} : pouvoir {} non possédé/actif, ignoré",
                    player.getGameProfile().getName(), nodeId);
            return;
        }
        // Bloc 9 : vérifier et poser la recharge (Cooldowns) avant d'exécuter.
        // Exécution de l'ability : point d'ancrage (hors des neuf blocs).
        RpgMod.LOGGER.debug("{} déclenche le pouvoir {} (emplacement {})",
                player.getGameProfile().getName(), nodeId, slot);
    }

    private static boolean ownsActivePower(ServerPlayer player, PlayerSkills skills, String nodeId) {
        ToIntFunction<String> causeCount = DeathStats.get(player)
                .<ToIntFunction<String>>map(stats -> stats::getCount)
                .orElse(cause -> 0);
        for (SkillTree tree : SkillTreeManager.INSTANCE.trees().values()) {
            EffectResolver.Progress progress =
                    EffectResolver.progress(skills.level(tree.id()), skills.nodes(), causeCount);
            for (ActiveEffect active : EffectResolver.active(tree, progress)) {
                if (active.ownerId().equals(nodeId)
                        && active.trigger().map(t -> t.family() == TriggerFamily.POWER).orElse(false)) {
                    return true;
                }
            }
        }
        return false;
    }
}
