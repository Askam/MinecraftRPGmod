package fr.needaname.mc_rpg_mod.effect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.needaname.mc_rpg_mod.skill.model.Filter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Résout les entrées d'un {@link Filter} (id ou tag {@code #…}) contre un bloc, une entité ou un item.
 * Côté serveur uniquement : compare par id ou par tag vanilla (jamais par instance). Un id ou un tag
 * inconnu (mod absent) ne matche simplement jamais — il ne plante pas.
 */
public final class Filters {
    private Filters() {
    }

    /** Lit un champ {@code filter} (chaîne ou tableau) d'un objet d'effet ; absent → vide. */
    public static java.util.Optional<Filter> of(JsonObject fields, String key) {
        if (!fields.has(key) || fields.get(key).isJsonNull()) {
            return java.util.Optional.empty();
        }
        JsonElement element = fields.get(key);
        java.util.List<String> entries = new java.util.ArrayList<>();
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> entries.add(e.getAsString()));
        } else {
            entries.add(element.getAsString());
        }
        return java.util.Optional.of(new Filter(entries));
    }

    public static boolean matchesBlock(Filter filter, BlockState state) {
        for (String entry : filter.entries()) {
            ResourceLocation id = ResourceLocation.tryParse(Filter.isTag(entry) ? entry.substring(1) : entry);
            if (id == null) {
                continue;
            }
            if (Filter.isTag(entry)) {
                if (state.is(TagKey.create(Registries.BLOCK, id))) {
                    return true;
                }
            } else if (id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesEntity(Filter filter, EntityType<?> type) {
        for (String entry : filter.entries()) {
            ResourceLocation id = ResourceLocation.tryParse(Filter.isTag(entry) ? entry.substring(1) : entry);
            if (id == null) {
                continue;
            }
            if (Filter.isTag(entry)) {
                if (type.is(TagKey.create(Registries.ENTITY_TYPE, id))) {
                    return true;
                }
            } else if (id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type))) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesItem(Filter filter, ItemStack stack) {
        for (String entry : filter.entries()) {
            ResourceLocation id = ResourceLocation.tryParse(Filter.isTag(entry) ? entry.substring(1) : entry);
            if (id == null) {
                continue;
            }
            if (Filter.isTag(entry)) {
                if (stack.is(TagKey.create(Registries.ITEM, id))) {
                    return true;
                }
            } else if (id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return true;
            }
        }
        return false;
    }

    /** Variante pour un {@link Block} seul (sans état), utile au butin. */
    public static boolean matchesBlock(Filter filter, Block block) {
        return matchesBlock(filter, block.defaultBlockState());
    }
}
