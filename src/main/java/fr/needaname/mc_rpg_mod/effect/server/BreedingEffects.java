package fr.needaname.mc_rpg_mod.effect.server;

import com.google.gson.JsonObject;
import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.effect.ServerEffects;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Effet {@code breeding} (Éleveur) : un seul tirage dans la table de poids décide du nombre de petits.
 * Le petit de l'événement compte pour un ; on fait apparaître les suivants sur place.
 *
 * <p>Les villageois sont exclus ({@code excludes_entity}) : dix bébés d'un coup feraient sauter le
 * watchdog. Le délai de reproduction vanilla n'est pas touché. La table elle-même est testée
 * ({@link BreedingTable}) ; l'apparition des petits supplémentaires reste à vérifier en jeu.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class BreedingEffects {
    private BreedingEffects() {
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player)) {
            return;
        }
        AgeableMob child = event.getChild();
        if (child == null || !(child.level() instanceof ServerLevel level)) {
            return;
        }
        ActiveEffect effect = ServerEffects.active(player, EffectType.BREEDING).stream().findFirst().orElse(null);
        if (effect == null) {
            return;
        }
        JsonObject fields = effect.effect().fields();

        if (fields.has("excludes_entity")) {
            ResourceLocation excluded = ResourceLocation.tryParse(fields.get("excludes_entity").getAsString());
            if (excluded != null && excluded.equals(BuiltInRegistries.ENTITY_TYPE.getKey(child.getType()))) {
                return;
            }
        }
        if (!fields.has("table")) {
            return;
        }
        BreedingTable table = BreedingTable.fromJson(fields.getAsJsonArray("table"));
        if (table.totalWeight() <= 0) {
            return;
        }
        int children = table.children(player.getRandom().nextInt(table.totalWeight()));
        for (int i = 1; i < children; i++) {
            if (!(child.getType().create(level) instanceof AgeableMob extra)) {
                continue;
            }
            extra.setBaby(true);
            extra.moveTo(child.getX(), child.getY(), child.getZ(), child.getYRot(), child.getXRot());
            level.addFreshEntity(extra);
        }
    }
}
