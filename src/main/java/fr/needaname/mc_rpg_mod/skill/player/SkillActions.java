package fr.needaname.mc_rpg_mod.skill.player;

import fr.needaname.mc_rpg_mod.effect.server.AttributeEffects;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.Tier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Actions serveur déclenchées par les écrans (choisir un nœud exclusif, assigner un pouvoir). Le
 * client n'envoie que l'intention ; tout passe ici par le chemin normal (reapply + synchro), le
 * serveur décide. Partagé entre les paquets d'écran et, le temps de la transition, les commandes.
 */
public final class SkillActions {
    private SkillActions() {
    }

    public static void takeNode(ServerPlayer player, String nodeId) {
        Optional<Tier> tier = SkillTreeManager.INSTANCE.trees().values().stream()
                .flatMap(tree -> tree.tiers().stream())
                .filter(t -> t.nodes().stream().anyMatch(n -> n.id().equals(nodeId)))
                .findFirst();
        if (tier.isEmpty()) {
            return;
        }
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            if (tier.get().exclusive()) {
                // Un seul choix par palier exclusif : on retire les frères avant d'ajouter.
                tier.get().nodes().forEach(node -> skills.removeNode(node.id()));
            }
            skills.addNode(nodeId);
        });
        AttributeEffects.reapply(player);
        SkillSyncEvents.syncTo(player);
    }

    public static void assignPower(ServerPlayer player, String slot, String nodeId) {
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            if (nodeId == null || nodeId.isEmpty()) {
                skills.clearPowerSlot(slot);
            } else {
                skills.setPowerSlot(slot, nodeId);
            }
        });
        SkillSyncEvents.syncTo(player);
    }
}
