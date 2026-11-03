package fr.needaname.mc_rpg_mod.skill.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeSet;

/**
 * Données RPG d'un joueur, hors compteurs de mort (ceux-ci restent dans {@link
 * fr.needaname.mc_rpg_mod.death.DeathStats}, voir le bloc 3) : niveau et XP par arbre, nœuds pris,
 * emplacements de pouvoirs, et l'horodatage des recharges.
 *
 * <p>Classe de données pure (seule dépendance : le NBT vanilla, lui-même sans {@code Bootstrap}),
 * volontairement séparée du jeton de capability ({@link PlayerSkillsProvider}) pour rester testable
 * sans démarrer Minecraft.
 *
 * <p>Aucune valeur d'équilibrage ici : les seuils de niveau, les coûts et les recharges vivent dans
 * les barèmes. Cette classe ne fait que retenir où en est le joueur.
 */
public class PlayerSkills {
    private final Map<String, Integer> levels = new HashMap<>();
    private final Map<String, Long> xp = new HashMap<>();
    private final Set<String> nodes = new HashSet<>();
    /** Emplacement de pouvoir (R / G / X) → id du nœud assigné. */
    private final Map<String, String> powerSlots = new HashMap<>();
    /** Recharge : id de nœud → {@code gameTime} du dernier usage (la logique est au bloc 9). */
    private final Map<String, Long> cooldowns = new HashMap<>();

    // --- Progression ---

    public int level(String tree) {
        return levels.getOrDefault(tree, 0);
    }

    public void setLevel(String tree, int level) {
        levels.put(tree, level);
    }

    public long xp(String tree) {
        return xp.getOrDefault(tree, 0L);
    }

    public void setXp(String tree, long value) {
        xp.put(tree, value);
    }

    public void addXp(String tree, long delta) {
        xp.merge(tree, delta, Long::sum);
    }

    // --- Nœuds pris ---

    public boolean hasNode(String nodeId) {
        return nodes.contains(nodeId);
    }

    public boolean addNode(String nodeId) {
        return nodes.add(nodeId);
    }

    public boolean removeNode(String nodeId) {
        return nodes.remove(nodeId);
    }

    public Set<String> nodes() {
        return Collections.unmodifiableSet(nodes);
    }

    // --- Emplacements de pouvoirs ---

    public Optional<String> powerSlot(String slot) {
        return Optional.ofNullable(powerSlots.get(slot));
    }

    public void setPowerSlot(String slot, String nodeId) {
        powerSlots.put(slot, nodeId);
    }

    public void clearPowerSlot(String slot) {
        powerSlots.remove(slot);
    }

    public Map<String, String> powerSlots() {
        return Collections.unmodifiableMap(powerSlots);
    }

    // --- Recharges (stockage seul ; horodatage comparé à level.getGameTime() au bloc 9) ---

    public OptionalLong lastUsed(String nodeId) {
        Long value = cooldowns.get(nodeId);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    public void setLastUsed(String nodeId, long gameTime) {
        cooldowns.put(nodeId, gameTime);
    }

    public Map<String, Long> cooldowns() {
        return Collections.unmodifiableMap(cooldowns);
    }

    // --- Recopie (respawn / retour de dimension, voir PlayerEvent.Clone) ---

    public void copyFrom(PlayerSkills other) {
        levels.clear();
        levels.putAll(other.levels);
        xp.clear();
        xp.putAll(other.xp);
        nodes.clear();
        nodes.addAll(other.nodes);
        powerSlots.clear();
        powerSlots.putAll(other.powerSlots);
        cooldowns.clear();
        cooldowns.putAll(other.cooldowns);
    }

    // --- NBT ---

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        CompoundTag trees = new CompoundTag();
        Set<String> treeIds = new TreeSet<>(levels.keySet());
        treeIds.addAll(xp.keySet());
        for (String id : treeIds) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("level", level(id));
            entry.putLong("xp", xp(id));
            trees.put(id, entry);
        }
        tag.put("trees", trees);

        ListTag nodesTag = new ListTag();
        new TreeSet<>(nodes).forEach(id -> nodesTag.add(StringTag.valueOf(id)));
        tag.put("nodes", nodesTag);

        CompoundTag powers = new CompoundTag();
        powerSlots.forEach(powers::putString);
        tag.put("powers", powers);

        CompoundTag cds = new CompoundTag();
        cooldowns.forEach(cds::putLong);
        tag.put("cooldowns", cds);

        return tag;
    }

    public void load(CompoundTag tag) {
        levels.clear();
        xp.clear();
        nodes.clear();
        powerSlots.clear();
        cooldowns.clear();

        CompoundTag trees = tag.getCompound("trees");
        for (String id : trees.getAllKeys()) {
            CompoundTag entry = trees.getCompound(id);
            if (entry.contains("level")) {
                levels.put(id, entry.getInt("level"));
            }
            if (entry.contains("xp")) {
                xp.put(id, entry.getLong("xp"));
            }
        }

        ListTag nodesTag = tag.getList("nodes", Tag.TAG_STRING);
        for (int i = 0; i < nodesTag.size(); i++) {
            nodes.add(nodesTag.getString(i));
        }

        CompoundTag powers = tag.getCompound("powers");
        for (String slot : powers.getAllKeys()) {
            powerSlots.put(slot, powers.getString(slot));
        }

        CompoundTag cds = tag.getCompound("cooldowns");
        for (String id : cds.getAllKeys()) {
            cooldowns.put(id, cds.getLong(id));
        }
    }
}
