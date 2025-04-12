package it.hurts.octostudios.reliquified_lenders_cataclysm.client.renderer;

import com.github.L_Ender.cataclysm.client.render.CMRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.VoidVortexModifiedEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class VoidVortexModifiedRenderer extends EntityRenderer<VoidVortexModifiedEntity> {
    private static final ResourceLocation TEXTURE_1 =
            ResourceLocation.fromNamespaceAndPath("cataclysm", "textures/entity/void_vortex/void_vortex_idle1.png");
    private static final ResourceLocation TEXTURE_2 =
            ResourceLocation.fromNamespaceAndPath("cataclysm", "textures/entity/void_vortex/void_vortex_idle2.png");
    private static final ResourceLocation TEXTURE_3 =
            ResourceLocation.fromNamespaceAndPath("cataclysm", "textures/entity/void_vortex/void_vortex_idle3.png");
    private static final ResourceLocation TEXTURE_4 =
            ResourceLocation.fromNamespaceAndPath("cataclysm", "textures/entity/void_vortex/void_vortex_idle4.png");
    private static final ResourceLocation[] TEXTURE_PROGRESS = new ResourceLocation[4];

    public VoidVortexModifiedRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);

        for(int i = 0; i < 4; ++i) {
            TEXTURE_PROGRESS[i] = ResourceLocation.fromNamespaceAndPath("cataclysm",
                            "textures/entity/void_vortex/void_vortex_grow_" + i + ".png");
        }
    }

    @Override
    public void render(@NotNull VoidVortexModifiedEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.001D, 0.0D);

        ResourceLocation texture;

        if (entity.getLifespan() < 16) {
            texture = this.getGrowingTexture((int)(entity.getLifespan() * 0.5F % 20.0F));
        } else if (entity.tickCount < 16) {
            texture = this.getGrowingTexture((int)((float)entity.tickCount * 0.5F % 20.0F));
        } else {
            texture = this.getIdleTexture(entity.tickCount % 9);
        }

        poseStack.scale(3.0F, 3.0F, 3.0F);
        renderArc(poseStack, buffer, texture);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderArc(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture) {
        poseStack.pushPose();
        VertexConsumer vertexBuilder = buffer.getBuffer(CMRenderTypes.getfullBright(texture));

        PoseStack.Pose lastPose = poseStack.last();

        drawVertex(lastPose, vertexBuilder, -1, 0, -1, 0.0F, 0.0F, 1, 0, 1, 240);
        drawVertex(lastPose, vertexBuilder, -1, 0, 1, 0.0F, 1.0F, 1, 0, 1, 240);
        drawVertex(lastPose, vertexBuilder, 1, 0, 1, 1.0F, 1.0F, 1, 0, 1, 240);
        drawVertex(lastPose, vertexBuilder, 1, 0, -1, 1.0F, 0.0F, 1, 0, 1, 240);

        poseStack.popPose();
    }

    public @NotNull ResourceLocation getTextureLocation(@NotNull VoidVortexModifiedEntity entity) {
        return TEXTURE_1;
    }

    public void drawVertex(PoseStack.Pose poseStack, VertexConsumer builder, float x, float y, float z,
                           float xUv, float zUv, float xNorm, float yNorm, float zNorm, int light) {
        builder.addVertex(poseStack, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(xUv, zUv).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(poseStack, xNorm, zNorm, yNorm);
    }

    public ResourceLocation getIdleTexture(int age) {
        if (age < 3) {
            return TEXTURE_1;
        } else if (age < 6) {
            return TEXTURE_2;
        } else {
            return age < 10 ? TEXTURE_3 : TEXTURE_4;
        }
    }

    public ResourceLocation getGrowingTexture(int age) {
        return TEXTURE_PROGRESS[Mth.clamp(age / 2, 0, 3)];
    }
}
