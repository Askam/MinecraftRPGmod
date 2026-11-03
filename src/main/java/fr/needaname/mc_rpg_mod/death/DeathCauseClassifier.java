package fr.needaname.mc_rpg_mod.death;

import fr.needaname.mc_rpg_mod.skill.model.CauseCounter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.List;

/**
 * Range une mort dans une cause déclarée par {@code counters.per_cause} de {@code mort.json}.
 *
 * <p>Les causes sont traitées comme <b>disjointes</b> : une mort compte dans la <b>première</b> cause
 * qu'elle satisfait, dans l'ordre du barème, puis {@link #FALLBACK}. {@code SCHEMA.md} ne tranche pas,
 * mais {@code excluding_source_entity} sur {@code explosion} (pour écarter le creeper) montre que les
 * causes sont pensées sans recouvrement — d'où ce choix, signalé dans la PR.
 *
 * <p>En 1.20.1 les types de dégâts sont data-driven : on compare par id ou par tag ({@code source.is(…)}),
 * jamais par instance. L'entité responsable est {@code source.getEntity()} (le tireur pour un projectile,
 * pas la flèche).
 */
public final class DeathCauseClassifier {
    public static final String FALLBACK = "other";

    private DeathCauseClassifier() {
    }

    /** @return l'id de la première cause satisfaite, sinon {@link #FALLBACK}. */
    public static String classify(List<CauseCounter> causes, DamageSource source) {
        for (CauseCounter cause : causes) {
            if (matches(cause, source)) {
                return cause.id();
            }
        }
        return FALLBACK;
    }

    static boolean matches(CauseCounter cause, DamageSource source) {
        if (!damageMatches(cause, source)) {
            return false;
        }
        Entity responsible = source.getEntity();
        if (cause.sourceEntity().isPresent() && !isEntity(responsible, cause.sourceEntity().get())) {
            return false;
        }
        // Le creeper est sa propre source indirecte : excluding_source_entity l'écarte de l'explosion générique.
        return cause.excludingSourceEntity().isEmpty() || !isEntity(responsible, cause.excludingSourceEntity().get());
    }

    private static boolean damageMatches(CauseCounter cause, DamageSource source) {
        boolean hasType = cause.damageType().isPresent();
        boolean hasTag = cause.damageTypeTag().isPresent();
        if (!hasType && !hasTag) {
            return true; // aucun critère de dégât : n'importe lequel convient
        }
        if (hasType) {
            ResourceLocation id = ResourceLocation.tryParse(cause.damageType().get());
            if (id != null && source.is(ResourceKey.create(Registries.DAMAGE_TYPE, id))) {
                return true;
            }
        }
        if (hasTag) {
            ResourceLocation id = ResourceLocation.tryParse(cause.damageTypeTag().get());
            if (id != null && source.is(TagKey.create(Registries.DAMAGE_TYPE, id))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEntity(Entity entity, String id) {
        if (entity == null) {
            return false;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && EntityType.getKey(entity.getType()).equals(rl);
    }
}
