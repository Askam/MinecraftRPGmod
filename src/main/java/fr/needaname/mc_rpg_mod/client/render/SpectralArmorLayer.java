package fr.needaname.mc_rpg_mod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.needaname.mc_rpg_mod.appearance.ClientAppearances;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Armure spectrale : rendue avec le {@code RenderType} de glint d'armure SEUL, sans passe de texture
 * dessous — c'est ce qui donne l'effet spectral. Posée après l'aura.
 *
 * <p>ÉCRIT SANS CLIENT. De plus, le drapeau {@code spectral} est pour l'instant toujours faux (l'état
 * transitoire {@code spectral_armour_active} n'est pas encore suivi côté serveur — voir
 * {@code AppearanceSync}). À relier à l'ability de respawn et à vérifier en jeu : le glint seul,
 * et sa fin sur le vidage du cadavre (Corpse).
 */
public class SpectralArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public SpectralArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!ClientAppearances.get(player.getId()).spectral() || player.isInvisible()) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.armorEntityGlint());
        getParentModel().renderToBuffer(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
    }
}
