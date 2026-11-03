package fr.needaname.mc_rpg_mod.client.hud;

import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Enregistre le HUD des recharges. Bus mod, client seulement. */
@Mod.EventBusSubscriber(modid = RpgMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RpgOverlays {
    private RpgOverlays() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("cooldowns", new CooldownHud());
    }
}
