package fr.needaname.mc_rpg_mod.effect.server;

import com.google.gson.JsonObject;
import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.effect.ServerEffects;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.jetbrains.annotations.Nullable;

/**
 * Effets {@code damage_dealt} (le joueur attaque) et {@code damage_taken} (le joueur encaisse),
 * appliqués comme multiplicateurs sur {@link LivingHurtEvent}, filtrés par leur {@code when}
 * (voir {@link DamageWhen}).
 *
 * <p>Seules les conditions {@code when} statiques sont gérées pour l'instant ; les conditions
 * dynamiques ({@code while_flag} de Riposte, {@code not_retreating_for_ticks} de Tenace) ne sont pas
 * encore évaluées et ces effets ne s'appliquent donc pas tant qu'elles ne le sont pas. À implémenter
 * et vérifier en jeu avec les fenêtres de pouvoir (bloc ultérieur).
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class CombatEffects {
    private CombatEffects() {
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        LivingEntity victim = event.getEntity();

        if (source.getEntity() instanceof ServerPlayer attacker) {
            double multiplier = combinedMultiplier(attacker, EffectType.DAMAGE_DEALT, source, victim);
            if (multiplier != 1.0) {
                event.setAmount((float) (event.getAmount() * multiplier));
            }
        }
        if (victim instanceof ServerPlayer hurtPlayer) {
            LivingEntity assailant = source.getEntity() instanceof LivingEntity living ? living : null;
            double multiplier = combinedMultiplier(hurtPlayer, EffectType.DAMAGE_TAKEN, source, assailant);
            if (multiplier != 1.0) {
                event.setAmount((float) (event.getAmount() * multiplier));
            }
        }
    }

    private static double combinedMultiplier(ServerPlayer player, EffectType type,
                                             DamageSource source, @Nullable LivingEntity subject) {
        double multiplier = 1.0;
        for (ActiveEffect active : ServerEffects.active(player, type)) {
            JsonObject fields = active.effect().fields();
            if (!fields.has("multiplier")) {
                continue;
            }
            if (fields.has("when") && fields.get("when").isJsonObject()
                    && !DamageWhen.matches(fields.getAsJsonObject("when"), source, subject)) {
                continue;
            }
            multiplier *= fields.get("multiplier").getAsDouble();
        }
        return multiplier;
    }
}
