package fr.needaname.mc_rpg_mod.effect.server;

import com.google.gson.JsonObject;
import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.effect.ServerEffects;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.TriggerFamily;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Effet {@code fall_distance} : retire des blocs à la distance de chute avant le calcul vanilla
 * (qui en retire déjà 3). Somme les {@code subtract_blocks} des effets actifs.
 *
 * <p>Un effet porté par un reflex (Roulade : « s'accroupir à l'atterrissage ») n'est appliqué que si
 * le joueur est accroupi à l'atterrissage — proxy serveur de la condition. La nuance exacte
 * (accroupi dans les 5 ticks) reste à vérifier en jeu. Les effets sans trigger (branche Atterrissage)
 * s'appliquent toujours.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class FallDistanceEffects {
    private FallDistanceEffects() {
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        double subtract = 0;
        for (ActiveEffect active : ServerEffects.active(player, EffectType.FALL_DISTANCE)) {
            boolean reflex = active.trigger().map(t -> t.family() == TriggerFamily.REFLEX).orElse(false);
            if (reflex && !player.isShiftKeyDown()) {
                continue;
            }
            JsonObject fields = active.effect().fields();
            if (fields.has("subtract_blocks")) {
                subtract += fields.get("subtract_blocks").getAsDouble();
            }
        }
        if (subtract > 0) {
            event.setDistance((float) Math.max(0, event.getDistance() - subtract));
        }
    }
}
