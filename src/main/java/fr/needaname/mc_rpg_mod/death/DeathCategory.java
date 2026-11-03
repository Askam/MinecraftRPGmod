package fr.needaname.mc_rpg_mod.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Catégorie de mort déclarée en JSON. Deux axes, combinés en ET :
 * <ul>
 *   <li>type de dégât : {@code damage_tags} OU {@code damage_types} (un seul suffit)</li>
 *   <li>entité responsable : {@code killer_types}, ids ou tags préfixés par {@code #}</li>
 * </ul>
 * Un axe vide accepte tout. Les ids/tags inconnus (mod absent) ne matchent simplement jamais.
 */
public record DeathCategory(int priority,
                            List<TagKey<DamageType>> damageTags,
                            List<ResourceKey<DamageType>> damageTypes,
                            KillerFilter killers) {

    public static final Codec<DeathCategory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("priority", 0).forGetter(DeathCategory::priority),
            TagKey.codec(Registries.DAMAGE_TYPE).listOf().optionalFieldOf("damage_tags", List.of()).forGetter(DeathCategory::damageTags),
            ResourceKey.codec(Registries.DAMAGE_TYPE).listOf().optionalFieldOf("damage_types", List.of()).forGetter(DeathCategory::damageTypes),
            KillerFilter.CODEC.optionalFieldOf("killer_types", KillerFilter.ANY).forGetter(DeathCategory::killers)
    ).apply(instance, DeathCategory::new));

    /** Une catégorie sans aucun critère attraperait toutes les morts : on la rejette au chargement. */
    public boolean isEmpty() {
        return damageTags.isEmpty() && damageTypes.isEmpty() && killers.isEmpty();
    }

    public boolean matches(DamageSource source) {
        // getEntity() remonte au tireur pour les projectiles (squelette, et non sa flèche).
        return matchesDamage(source) && killers.matches(source.getEntity());
    }

    private boolean matchesDamage(DamageSource source) {
        if (damageTags.isEmpty() && damageTypes.isEmpty()) {
            return true;
        }
        for (TagKey<DamageType> tag : damageTags) {
            if (source.is(tag)) {
                return true;
            }
        }
        for (ResourceKey<DamageType> type : damageTypes) {
            if (source.is(type)) {
                return true;
            }
        }
        return false;
    }

    public record KillerFilter(Set<ResourceLocation> ids, List<TagKey<EntityType<?>>> tags) {
        public static final KillerFilter ANY = new KillerFilter(Set.of(), List.of());
        public static final Codec<KillerFilter> CODEC = Codec.STRING.listOf().comapFlatMap(KillerFilter::parse, KillerFilter::toStrings);

        private static DataResult<KillerFilter> parse(List<String> entries) {
            Set<ResourceLocation> ids = new HashSet<>();
            List<TagKey<EntityType<?>>> tags = new ArrayList<>();
            for (String entry : entries) {
                boolean isTag = entry.startsWith("#");
                ResourceLocation id = ResourceLocation.tryParse(isTag ? entry.substring(1) : entry);
                if (id == null) {
                    return DataResult.error(() -> "Invalid killer type: " + entry);
                }
                if (isTag) {
                    tags.add(TagKey.create(Registries.ENTITY_TYPE, id));
                } else {
                    ids.add(id);
                }
            }
            return DataResult.success(new KillerFilter(Set.copyOf(ids), List.copyOf(tags)));
        }

        private List<String> toStrings() {
            List<String> out = new ArrayList<>();
            ids.forEach(id -> out.add(id.toString()));
            tags.forEach(tag -> out.add("#" + tag.location()));
            return out;
        }

        public boolean isEmpty() {
            return ids.isEmpty() && tags.isEmpty();
        }

        public boolean matches(@Nullable Entity killer) {
            if (isEmpty()) {
                return true;
            }
            if (killer == null) {
                return false;
            }
            EntityType<?> type = killer.getType();
            if (ids.contains(EntityType.getKey(type))) {
                return true;
            }
            for (TagKey<EntityType<?>> tag : tags) {
                if (type.is(tag)) {
                    return true;
                }
            }
            return false;
        }
    }
}
