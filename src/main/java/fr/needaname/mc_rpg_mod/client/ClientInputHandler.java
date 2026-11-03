package fr.needaname.mc_rpg_mod.client;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.network.ClientReflexPacket;
import fr.needaname.mc_rpg_mod.network.HoldKeyPacket;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.network.TriggerPowerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Traduit les entrées client en intentions envoyées au serveur, qui décide (le client n'exécute
 * jamais un effet de jeu). Toute la logique « fine » (double saut, double appui, maintien) vit ici,
 * côté client, car le serveur ne peut pas l'observer.
 *
 * <p>À VÉRIFIER / AFFINER EN JEU (session locale) : le ressenti et les seuils de détection du double
 * saut et du double appui sont des heuristiques écrites sans client ; la roulade (appui frais
 * d'accroupissement dans les 5 ticks avant l'impact) reste pour l'instant gérée côté serveur par
 * {@code isShiftKeyDown} (voir {@code FallDistanceEffects}) — son raffinement client est à faire ici.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID, value = Dist.CLIENT)
public final class ClientInputHandler {
    private static final int DOUBLE_TAP_MAX_TICKS = 7;

    private static boolean holdDown;
    private static boolean jumpDown;
    private static boolean airJumpUsed;
    private static boolean forwardDown;
    private static long lastForwardTap = Long.MIN_VALUE;
    private static long clientTick;

    private ClientInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        clientTick++;

        // Ouverture de la vue d'ensemble.
        while (RpgKeyMappings.OPEN_SKILLS.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new fr.needaname.mc_rpg_mod.client.screen.OverviewScreen());
            }
        }

        // Touche de maintien : on n'envoie qu'au changement d'état, pas à chaque bloc.
        boolean held = RpgKeyMappings.HOLD.isDown();
        if (held != holdDown) {
            holdDown = held;
            RpgNetwork.sendToServer(new HoldKeyPacket(held));
        }

        // Pouvoirs : chaque appui consommé devient un déclenchement (le serveur vérifie possession + recharge).
        while (RpgKeyMappings.POWER_R.consumeClick()) {
            RpgNetwork.sendToServer(new TriggerPowerPacket("R"));
        }
        while (RpgKeyMappings.POWER_G.consumeClick()) {
            RpgNetwork.sendToServer(new TriggerPowerPacket("G"));
        }
        while (RpgKeyMappings.POWER_X.consumeClick()) {
            RpgNetwork.sendToServer(new TriggerPowerPacket("X"));
        }

        detectDoubleJump(mc, player);
        detectForwardDoubleTap(mc, player);
    }

    /** Double saut : appui frais sur saut, en l'air, non encore consommé depuis le dernier contact au sol. */
    private static void detectDoubleJump(Minecraft mc, LocalPlayer player) {
        if (player.onGround()) {
            airJumpUsed = false;
        }
        boolean jump = mc.options.keyJump.isDown();
        boolean rising = jump && !jumpDown;
        jumpDown = jump;
        if (rising && !player.onGround() && !airJumpUsed) {
            airJumpUsed = true;
            RpgNetwork.sendToServer(new ClientReflexPacket("client_jump_airborne"));
        }
    }

    /**
     * Double appui vers l'avant : le deuxième appui est le geste de sprint vanilla, donc on exige déjà
     * le sprint pour que la ruée (Élan) ne se consomme pas à chaque départ en course. Les trois autres
     * directions n'ont pas de sens vanilla ; leur détection reste à ajouter si besoin en jeu.
     */
    private static void detectForwardDoubleTap(Minecraft mc, LocalPlayer player) {
        boolean forward = mc.options.keyUp.isDown();
        boolean rising = forward && !forwardDown;
        forwardDown = forward;
        if (rising) {
            if (clientTick - lastForwardTap <= DOUBLE_TAP_MAX_TICKS && player.isSprinting()) {
                RpgNetwork.sendToServer(new ClientReflexPacket("client_double_tap_direction"));
            }
            lastForwardTap = clientTick;
        }
    }
}
