package fr.needaname.mc_rpg_mod.category;

import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Agrégats pour l'écran d'administration : combien de joueurs connectés ont des niveaux dans chaque
 * arbre, et le niveau le plus haut — à voir avant de couper quoi que ce soit. Produit aussi la charge
 * utile (états + ces comptes) du paquet d'ouverture de l'écran.
 */
public final class CategoryAdmin {
    private CategoryAdmin() {
    }

    public record TreeSummary(int players, int maxLevel) {
    }

    public static Map<String, TreeSummary> summary(MinecraftServer server) {
        Map<String, int[]> accumulator = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerSkillsProvider.get(player).ifPresent(skills -> {
                for (SkillTree tree : SkillTreeManager.INSTANCE.trees().values()) {
                    int level = skills.level(tree.id());
                    if (level > 0) {
                        int[] counters = accumulator.computeIfAbsent(tree.id(), key -> new int[2]);
                        counters[0]++;
                        counters[1] = Math.max(counters[1], level);
                    }
                }
            });
        }
        Map<String, TreeSummary> summary = new HashMap<>();
        accumulator.forEach((treeId, counters) -> summary.put(treeId, new TreeSummary(counters[0], counters[1])));
        return summary;
    }

    /** {@code { categories: {id: etat}, counts: {id: {players, max}} }} pour l'écran (handoff 3). */
    public static CompoundTag payload(MinecraftServer server) {
        CompoundTag tag = new CompoundTag();

        CompoundTag categories = new CompoundTag();
        CategorySavedData.get(server).states().forEach((id, state) -> categories.putString(id, state.id()));
        tag.put("categories", categories);

        CompoundTag counts = new CompoundTag();
        summary(server).forEach((id, s) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("players", s.players());
            entry.putInt("max", s.maxLevel());
            counts.put(id, entry);
        });
        tag.put("counts", counts);

        return tag;
    }
}
