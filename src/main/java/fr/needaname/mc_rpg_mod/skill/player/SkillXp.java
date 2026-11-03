package fr.needaname.mc_rpg_mod.skill.player;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.XpSource;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiPredicate;

/**
 * Attribution d'XP côté serveur : applique {@link SkillProgression} à la capability du joueur.
 * La synchro vers le client viendra au bloc 8 ; ici on met seulement l'état serveur à jour.
 */
public final class SkillXp {
    private SkillXp() {
    }

    /** Ajoute {@code amount} d'XP à l'arbre {@code treeId} du joueur, avec montées de niveau. */
    public static void grant(ServerPlayer player, String treeId, long amount) {
        if (amount <= 0) {
            return;
        }
        SkillTree tree = SkillTreeManager.INSTANCE.byId(treeId).orElse(null);
        if (tree == null) {
            return;
        }
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            SkillProgression.Result result = SkillProgression.gain(
                    tree.xpCurve(), tree.maxLevel(), skills.level(treeId), skills.xp(treeId), amount);
            skills.setLevel(treeId, result.level());
            skills.setXp(treeId, result.xpIntoLevel());
            if (result.levelsGained() > 0) {
                RpgMod.LOGGER.debug("{} : +{} niveau(x) en {} (niveau {})",
                        player.getGameProfile().getName(), result.levelsGained(), treeId, result.level());
            }
        });
        // L'interface a besoin de la progression à jour : on renvoie la capability au joueur.
        SkillSyncEvents.syncTo(player);
    }

    /**
     * Route un événement ponctuel : pour chaque arbre et chaque source {@code xp_sources} dont le
     * champ {@code on} correspond et que {@code accept} valide (filtre + require), attribue son XP.
     */
    public static void forEvent(ServerPlayer player, String on, BiPredicate<SkillTree, XpSource.Event> accept) {
        for (SkillTree tree : SkillTreeManager.INSTANCE.trees().values()) {
            for (XpSource source : tree.xpSources()) {
                if (source instanceof XpSource.Event event && event.on().equals(on) && accept.test(tree, event)) {
                    grant(player, tree.id(), event.xp());
                }
            }
        }
    }
}
