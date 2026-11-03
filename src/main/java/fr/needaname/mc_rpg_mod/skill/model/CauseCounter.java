package fr.needaname.mc_rpg_mod.skill.model;

import java.util.Optional;

/**
 * Une cause de mort comptée séparément (clé {@code counters.per_cause} de {@code mort.json}),
 * source de vérité du design remplaçant les anciens {@code death_categories}.
 *
 * <p>Les critères restent des chaînes brutes ; leur résolution en types/tags de dégâts et en
 * entités (comparaison par id ou tag, jamais par instance) se fait côté serveur, au bloc 3.
 *
 * <p>{@code excludingSourceEntity} sépare l'explosion générique du creeper : les causes sont
 * pensées comme disjointes.
 */
public record CauseCounter(String id,
                           Optional<String> damageType,
                           Optional<String> damageTypeTag,
                           Optional<String> sourceEntity,
                           Optional<String> excludingSourceEntity) {
}
