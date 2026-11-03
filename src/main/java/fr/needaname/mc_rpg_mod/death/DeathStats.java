package fr.needaname.mc_rpg_mod.death;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Compteurs de morts d'un joueur : un total global + un compteur par catégorie (id du JSON).
 * Les compteurs d'une catégorie retirée des JSON sont conservés, pour ne rien perdre si elle revient.
 */
public class DeathStats {
    public static final Capability<DeathStats> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private final Map<ResourceLocation, Integer> byCategory = new HashMap<>();
    private int total;

    public static Optional<DeathStats> get(Player player) {
        return player.getCapability(CAPABILITY).resolve();
    }

    public void recordDeath(ResourceLocation category) {
        total++;
        byCategory.merge(category, 1, Integer::sum);
    }

    public int getTotal() {
        return total;
    }

    public int getCount(ResourceLocation category) {
        return byCategory.getOrDefault(category, 0);
    }

    public Map<ResourceLocation, Integer> getAll() {
        return Collections.unmodifiableMap(byCategory);
    }

    public void copyFrom(DeathStats other) {
        byCategory.clear();
        byCategory.putAll(other.byCategory);
        total = other.total;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("total", total);
        CompoundTag categories = new CompoundTag();
        byCategory.forEach((id, count) -> categories.putInt(id.toString(), count));
        tag.put("categories", categories);
        return tag;
    }

    public void load(CompoundTag tag) {
        byCategory.clear();
        total = tag.getInt("total");
        CompoundTag categories = tag.getCompound("categories");
        for (String key : categories.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                byCategory.put(id, categories.getInt(key));
            }
        }
    }
}
