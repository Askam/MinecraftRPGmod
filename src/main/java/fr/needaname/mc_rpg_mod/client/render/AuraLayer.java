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
 * Aura rouge : apparaît au niveau 40 de Mort, s'intensifie au 50 (intensité pilotée par les données,
 * via {@link ClientAppearances}). Passe translucide émissive sur le modèle, légèrement agrandie.
 *
 * <p>ÉCRIT SANS CLIENT : rendu non vu. À VÉRIFIER EN JEU, en particulier avec les shaders (Oculus +
 * Complementary) et sur Mac, et sur les deux modèles (bras fins / larges).
 */
public class AuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public AuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        float aura = ClientAppearances.get(player.getId()).aura();
        if (aura <= 0f || player.isInvisible()) {
            return;
        }
        pose.pushPose();
        pose.scale(1.07f, 1.07f, 1.07f);
        pose.translate(0.0, -0.03, 0.0);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(player.getSkinTextureLocation()));
        float alpha = Math.min(1f, 0.25f + 0.35f * aura);
        getParentModel().renderToBuffer(pose, buffer, 15728640, OverlayTexture.NO_OVERLAY, 1.0f, 0.12f, 0.12f, alpha);
        pose.popPose();
    }
}
