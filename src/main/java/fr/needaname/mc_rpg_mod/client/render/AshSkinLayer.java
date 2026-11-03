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
 * Peau de cendre : assombrit durablement la peau, débloquée par la branche « Né des flammes »
 * (drapeau {@link ClientAppearances}). Passe translucide sombre sur le modèle.
 *
 * <p>ÉCRIT SANS CLIENT : à vérifier en jeu (visible sur un autre joueur, avec shaders, deux modèles).
 */
public class AshSkinLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public AshSkinLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!ClientAppearances.get(player.getId()).ash() || player.isInvisible()) {
            return;
        }
        pose.pushPose();
        pose.scale(1.02f, 1.02f, 1.02f);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        getParentModel().renderToBuffer(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY, 0.12f, 0.09f, 0.09f, 0.55f);
        pose.popPose();
    }
}
