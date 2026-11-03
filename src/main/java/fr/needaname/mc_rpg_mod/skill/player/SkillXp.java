package fr.needaname.mc_rpg_mod.skill.player;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.appearance.AppearanceSync;
import fr.needaname.mc_rpg_mod.category.CategorySavedData;
import fr.needaname.mc_rpg_mod.effect.server.AttributeEffects;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.XpSource;
import net.minecraft.server.MinecraftServer;
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
        if (tree == null || isDisabled(player, treeId)) {
            return;
        }
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            SkillProgression.Result result = SkillProgression.gain(
                    tree.xpCurve(), tree.maxLevel(), skills.level(treeId), skills.xp(treeId), amount);
            skills.setLevel(treeId, result.level());
            skills.setXp(treeId, result.xpIntoLevel());
            if (result.levelsGained() > 0) {
                // Un palier peut avoir débloqué un effet attribute : sans ça il n'apparaîtrait qu'à la reconnexion.
                AttributeEffects.reapply(player);
                // Le niveau peut franchir un seuil d'apparence (aura de Mort) : prévenir les observateurs.
                AppearanceSync.sendFor(player);
                RpgMod.LOGGER.debug("{} : +{} niveau(x) en {} (niveau {})",
                        player.getGameProfile().getName(), result.levelsGained(), treeId, result.level());
            }
        });
        // L'interface a besoin de la progression à jour : on renvoie la capability au joueur.
        SkillSyncEvents.syncTo(player);
    }

    /**
     * Fixe la progression d'un arbre à une XP totale (commandes d'administration) en passant par le
     * même calcul niveau/reliquat que le jeu, puis réapplique les effets et resynchronise — comme une
     * vraie montée (ou descente) de niveau. {@code deltaXp} peut être négatif.
     */
    public static void adjust(ServerPlayer player, String treeId, long deltaXp) {
        SkillTree tree = SkillTreeManager.INSTANCE.byId(treeId).orElse(null);
        if (tree == null || isDisabled(player, treeId)) {
            return;
        }
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            long total = SkillProgression.totalXp(tree.xpCurve(), skills.level(treeId), skills.xp(treeId)) + deltaXp;
            SkillProgression.Result result = SkillProgression.fromTotal(tree.xpCurve(), tree.maxLevel(), total);
            skills.setLevel(treeId, result.level());
            skills.setXp(treeId, result.xpIntoLevel());
        });
        AttributeEffects.reapply(player);
        SkillSyncEvents.syncTo(player);
    }

    /** Un arbre désactivé par l'administrateur ne gagne plus d'XP (ni par événement, ni par commande). */
    private static boolean isDisabled(ServerPlayer player, String treeId) {
        MinecraftServer server = player.getServer();
        return server != null && CategorySavedData.get(server).isDisabled(treeId);
    }

    /**
     * Route un événement cumulatif ({@code damage_survived}, {@code heal}) : {@code amount} s'ajoute au
     * reliquat de chaque source {@code on} correspondante, et chaque tranche de {@code per} rapporte son
     * {@code xp}. Le reliquat est conservé (NBT) pour qu'aucune fraction ne se perde.
     * Les sources portant un {@code filter} ou un {@code require} ne sont pas évaluées ici et sont écartées.
     */
    public static void accumulate(ServerPlayer player, String on, double amount) {
        if (amount <= 0) {
            return;
        }
        PlayerSkills skills = PlayerSkillsProvider.get(player).orElse(null);
        if (skills == null) {
            return;
        }
        for (SkillTree tree : SkillTreeManager.INSTANCE.trees().values()) {
            for (XpSource source : tree.xpSources()) {
                if (!(source instanceof XpSource.Event event) || !event.on().equals(on)
                        || event.filter().isPresent() || event.require().isPresent()) {
                    continue;
                }
                int per = event.per().orElse(0);
                if (per <= 0) {
                    grant(player, tree.id(), event.xp());
                    continue;
                }
                String key = tree.id() + "/" + on;
                double total = skills.accumulator(key) + amount;
                long steps = (long) (total / per);
                skills.setAccumulator(key, total - steps * per);
                if (steps > 0) {
                    grant(player, tree.id(), steps * event.xp());
                }
            }
        }
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
