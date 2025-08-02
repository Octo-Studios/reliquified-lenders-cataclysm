package it.hurts.octostudios.reliquified_lenders_cataclysm.client.renderer.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.hurts.octostudios.reliquified_lenders_cataclysm.client.models.entities.IgnitedShieldModel;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.fire_plate.IgnitedShieldEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class IgnitedShieldRenderer extends EntityRenderer<IgnitedShieldEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("cataclysm", "textures/entity/revenant_shield.png");

    public IgnitedShieldRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
    }

    @Override
    public void render(@NotNull IgnitedShieldEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.translate(0F, -0.7F, 0F);

        float angle = (entity.tickCount + partialTicks) * 4F;
        poseStack.mulPose(Axis.YP.rotationDegrees(-angle + 135));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        new IgnitedShieldModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull IgnitedShieldEntity entity) {
        return TEXTURE;
    }
}
