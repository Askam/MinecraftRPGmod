package fr.needaname.mc_rpg_mod.client.render;

import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ajoute les {@code RenderLayer} du mod aux deux modèles du joueur (bras larges « default » et bras
 * fins « slim »), pilotés par les compteurs de Mort synchronisés. Bus mod, client seulement.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RpgRenderLayers {
    private RpgRenderLayers() {
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        addToSkin(event, "default");
        addToSkin(event, "slim");
    }

    private static void addToSkin(EntityRenderersEvent.AddLayers event, String skin) {
        PlayerRenderer renderer = event.getSkin(skin);
        if (renderer == null) {
            return;
        }
        // L'aura d'abord, puis l'armure spectrale par-dessus (ordre voulu par le handoff).
        renderer.addLayer(new AuraLayer(renderer));
        renderer.addLayer(new AshSkinLayer(renderer));
        renderer.addLayer(new SpectralArmorLayer(renderer));
    }
}
