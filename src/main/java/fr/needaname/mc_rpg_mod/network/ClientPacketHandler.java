package fr.needaname.mc_rpg_mod.network;

import com.google.gson.JsonParser;
import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.category.CategoryState;
import fr.needaname.mc_rpg_mod.category.ClientCategoryState;
import fr.needaname.mc_rpg_mod.death.DeathStats;
import fr.needaname.mc_rpg_mod.skill.ClientSkillTrees;
import fr.needaname.mc_rpg_mod.skill.SkillTreeParser;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Uniquement chargée côté client (via DistExecutor). */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handleDeathStats(SyncDeathStatsPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(packet.entityId()) instanceof Player player) {
            DeathStats.get(player).ifPresent(stats -> stats.load(packet.data()));
        }
    }

    public static void handleSkills(SyncSkillsPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(packet.entityId()) instanceof Player player) {
            PlayerSkillsProvider.get(player).ifPresent(skills -> skills.load(packet.data()));
        }
    }

    public static void handleCategories(SyncCategoriesPacket packet) {
        CompoundTag data = packet.data();
        Map<String, CategoryState> states = new HashMap<>();
        for (String treeId : data.getAllKeys()) {
            states.put(treeId, CategoryState.byId(data.getString(treeId)));
        }
        ClientCategoryState.set(states);
    }

    public static void handleOpenAdmin(OpenAdminPacket packet) {
        // L'écran d'administration relève du handoff 3. Pour l'instant : point d'ancrage.
        RpgMod.LOGGER.info("Ouverture de l'écran d'administration demandée (écran à venir, handoff 3) : {}", packet.data());
    }

    public static void handleSkillTrees(SyncSkillTreesPacket packet) {
        Map<String, SkillTree> trees = new HashMap<>();
        packet.raw().forEach((id, json) -> {
            try {
                trees.put(id, SkillTreeParser.parse(id, JsonParser.parseString(json).getAsJsonObject()));
            } catch (RuntimeException ex) {
                // Couvre SkillLoadException. Le serveur a déjà validé ; on ne plante pas le client si un arbre résiste.
                RpgMod.LOGGER.error("Barème reçu illisible {} : {}", id, ex.getMessage());
            }
        });
        ClientSkillTrees.set(trees);
    }
}
