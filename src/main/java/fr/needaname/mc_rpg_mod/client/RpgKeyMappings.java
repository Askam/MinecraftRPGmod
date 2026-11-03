package fr.needaname.mc_rpg_mod.client;

import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Touches du mod. {@code C}, {@code R}, {@code G}, {@code X} sont libres en vanilla ; {@code V} est
 * évité (parole de Simple Voice Chat, présent dans le modpack). Toutes restent remappables.
 *
 * <p>Classe client uniquement (chargée seulement via le bus mod côté client). À vérifier en jeu : le
 * ressenti et l'absence de conflit avec le modpack.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RpgKeyMappings {
    private static final String CATEGORY = "key.categories." + RpgMod.MODID;

    /** Touche de récolte maintenue (Veinage, Abattage, Moisson, Carrier). */
    public static final KeyMapping HOLD = new KeyMapping("key." + RpgMod.MODID + ".hold", GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping POWER_R = new KeyMapping("key." + RpgMod.MODID + ".power_r", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping POWER_G = new KeyMapping("key." + RpgMod.MODID + ".power_g", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping POWER_X = new KeyMapping("key." + RpgMod.MODID + ".power_x", GLFW.GLFW_KEY_X, CATEGORY);

    private RpgKeyMappings() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(HOLD);
        event.register(POWER_R);
        event.register(POWER_G);
        event.register(POWER_X);
    }
}
