package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.world.entity.CareerProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class CareerProjectileRenderer extends EntityRenderer<CareerProjectileEntity> {
    private static final ResourceLocation FIREBALL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID,
                    "textures/entity/career_projectile/fireball.png");
    private static final ResourceLocation FROST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID,
                    "textures/entity/career_projectile/frost_shard.png");
    private static final ResourceLocation DARK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID,
                    "textures/entity/career_projectile/dark_bolt.png");
    private static final float HALF_SIZE = 0.28F;

    public CareerProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CareerProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        float pulse = 0.95F + Mth.sin((entity.tickCount + partialTick) * 0.35F) * 0.08F;
        poseStack.scale(pulse, pulse, pulse);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
        vertex(consumer, pose, packedLight, -HALF_SIZE, -HALF_SIZE, 0.0F, 1.0F);
        vertex(consumer, pose, packedLight, HALF_SIZE, -HALF_SIZE, 1.0F, 1.0F);
        vertex(consumer, pose, packedLight, HALF_SIZE, HALF_SIZE, 1.0F, 0.0F);
        vertex(consumer, pose, packedLight, -HALF_SIZE, HALF_SIZE, 0.0F, 0.0F);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CareerProjectileEntity entity) {
        ResourceLocation skillId = entity.skillId();
        if (skillId != null) {
            String path = skillId.getPath();
            if (path.contains("frost") || path.contains("ice") || path.contains("blizzard") || path.contains("glacial") || path.contains("zero")) {
                return FROST_TEXTURE;
            }
            if (path.contains("soul") || path.contains("bone") || path.contains("death") || path.contains("lich") || path.contains("undead")) {
                return DARK_TEXTURE;
            }
        }
        return FIREBALL_TEXTURE;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight,
                               float x, float y, float u, float v) {
        consumer.vertex(pose.pose(), x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
