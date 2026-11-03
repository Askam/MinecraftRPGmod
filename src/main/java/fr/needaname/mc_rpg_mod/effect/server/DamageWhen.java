package fr.needaname.mc_rpg_mod.effect.server;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Évalue le {@code when} d'un effet {@code damage_taken}/{@code damage_dealt}. Toutes les clés
 * présentes doivent être satisfaites (ET). Les comparaisons de type de dégât se font par id ou tag.
 *
 * <p>Clés statiques interprétées : {@code damage_type}, {@code not_damage_type},
 * {@code damage_type_tag}, {@code not_damage_type_tag}, {@code entity_type}, {@code target_health_below}.
 * Toute autre clé (conditions dynamiques comme {@code while_flag} ou {@code not_retreating_for_ticks})
 * n'est pas encore évaluée : par prudence l'effet ne s'applique alors PAS (plutôt qu'un bonus erroné).
 * {@code subject} est la victime pour {@code damage_dealt}, l'attaquant pour {@code damage_taken}.
 */
public final class DamageWhen {
    private DamageWhen() {
    }

    public static boolean matches(JsonObject when, DamageSource source, @Nullable LivingEntity subject) {
        for (String key : when.keySet()) {
            switch (key) {
                case "damage_type" -> {
                    if (!isType(source, when.get(key).getAsString())) {
                        return false;
                    }
                }
                case "not_damage_type" -> {
                    if (isType(source, when.get(key).getAsString())) {
                        return false;
                    }
                }
                case "damage_type_tag" -> {
                    if (!isTag(source, when.get(key).getAsString())) {
                        return false;
                    }
                }
                case "not_damage_type_tag" -> {
                    if (isTag(source, when.get(key).getAsString())) {
                        return false;
                    }
                }
                case "entity_type" -> {
                    if (subject == null || !isEntity(subject, when.get(key).getAsString())) {
                        return false;
                    }
                }
                case "target_health_below" -> {
                    if (subject == null || subject.getMaxHealth() <= 0
                            || subject.getHealth() / subject.getMaxHealth() >= when.get(key).getAsDouble()) {
                        return false;
                    }
                }
                default -> {
                    // Condition dynamique non encore gérée : on n'applique pas (prudence).
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isType(DamageSource source, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && source.is(ResourceKey.create(Registries.DAMAGE_TYPE, rl));
    }

    private static boolean isTag(DamageSource source, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && source.is(TagKey.create(Registries.DAMAGE_TYPE, rl));
    }

    private static boolean isEntity(LivingEntity subject, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && rl.equals(EntityType.getKey(subject.getType()));
    }
}
