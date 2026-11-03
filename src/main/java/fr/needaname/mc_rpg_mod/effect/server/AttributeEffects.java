package fr.needaname.mc_rpg_mod.effect.server;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.effect.ServerEffects;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.Branch;
import fr.needaname.mc_rpg_mod.skill.model.Effect;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.Node;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.Tier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Applique les effets {@code attribute} comme modificateurs d'attribut vanilla. Chaque couple
 * (nœud/branche, attribut) a un UUID fixe et déterministe : on retire toujours avant de réappliquer,
 * sinon le modificateur se cumule à chaque reconnexion (piège connu). Modificateurs transitoires :
 * cette classe est la source de vérité, ils ne sont pas sauvegardés dans le NBT de l'entité.
 *
 * <p>Recalcul aux moments où la progression a pu changer et où l'entité est fraîche : connexion,
 * respawn, changement de dimension. À câbler en plus (point ouvert) : un recalcul immédiat après une
 * montée de niveau (sinon un attribut gagné s'applique à la prochaine reconnexion). À vérifier en jeu.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class AttributeEffects {
    private AttributeEffects() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reapply(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reapply(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reapply(player);
        }
    }

    public static void reapply(ServerPlayer player) {
        // 1. Retirer nos modificateurs partout où un effet attribute est déclaré (actif ou non).
        for (SkillTree tree : SkillTreeManager.INSTANCE.trees().values()) {
            forAttributeEffects(tree, (ownerId, effect) -> {
                ResourceLocation attrId = parseAttributeId(effect);
                Attribute attribute = attrId == null ? null : BuiltInRegistries.ATTRIBUTE.get(attrId);
                if (attribute != null) {
                    AttributeInstance instance = player.getAttribute(attribute);
                    if (instance != null) {
                        instance.removeModifier(modifierId(ownerId, attrId));
                    }
                }
            });
        }
        // 2. Réappliquer pour les effets actuellement actifs.
        for (ActiveEffect active : ServerEffects.active(player, EffectType.ATTRIBUTE)) {
            ResourceLocation attrId = parseAttributeId(active.effect());
            if (attrId == null) {
                continue;
            }
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(attrId);
            if (attribute == null) {
                RpgMod.LOGGER.warn("Attribut inconnu {} (effet {})", attrId, active.ownerId());
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            AttributeModifier.Operation operation = operation(active.effect().fields().get("operation").getAsString());
            double value = active.effect().fields().get("value").getAsDouble();
            UUID id = modifierId(active.ownerId(), attrId);
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, RpgMod.MODID + ":" + active.ownerId(), value, operation));
        }
    }

    private static void forAttributeEffects(SkillTree tree, BiConsumer<String, Effect> consumer) {
        for (Tier tier : tree.tiers()) {
            for (Node node : tier.nodes()) {
                for (Effect effect : node.effects()) {
                    if (effect.type() == EffectType.ATTRIBUTE) {
                        consumer.accept(node.id(), effect);
                    }
                }
            }
        }
        for (Branch branch : tree.branches()) {
            for (Effect effect : branch.effects()) {
                if (effect.type() == EffectType.ATTRIBUTE) {
                    consumer.accept(branch.id(), effect);
                }
            }
        }
    }

    private static ResourceLocation parseAttributeId(Effect effect) {
        return effect.fields().has("attribute")
                ? ResourceLocation.tryParse(effect.fields().get("attribute").getAsString())
                : null;
    }

    private static UUID modifierId(String ownerId, ResourceLocation attribute) {
        return UUID.nameUUIDFromBytes((RpgMod.MODID + "/" + ownerId + "/" + attribute).getBytes(StandardCharsets.UTF_8));
    }

    private static AttributeModifier.Operation operation(String id) {
        return switch (id) {
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }
}
