package com.hongyuwu.careerchronicle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.ModelManager;

/**
 * 自定义骨骼引擎-设计文档-护甲跟随腿部弯曲.md §3.2. Drop-in replacement for the vanilla
 * {@code HumanoidArmorLayer} that, before delegating to the vanilla render pipeline, copies the
 * body model's shin pose onto both armor models. Safe ordering: {@code super.render()} internally
 * calls {@code copyPropertiesTo}, which only touches the six vanilla body parts (head/hat/body/
 * arms/legs) and has no knowledge of the shin child bones, so the pose written here survives.
 *
 * <p>{@code innerModel}/{@code outerModel} are private on the vanilla superclass -- this class
 * keeps its own references (assigned from the same constructor arguments passed to {@code super},
 * no reflection needed) purely so {@link #render} can reach their shin bones.
 */
public class CustomLegArmorLayer extends HumanoidArmorLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, CustomLegArmorModel> {

    private final CustomLegArmorModel innerModel;
    private final CustomLegArmorModel outerModel;

    public CustomLegArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                CustomLegArmorModel innerModel, CustomLegArmorModel outerModel,
                                ModelManager modelManager) {
        super(parent, innerModel, outerModel, modelManager);
        this.innerModel = innerModel;
        this.outerModel = outerModel;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                        float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                        float netHeadYaw, float headPitch) {
        // getParentModel()'s *declared* type is PlayerModel<AbstractClientPlayer> (fixed by
        // PlayerRenderer's own class signature) -- it's only a CustomLegPlayerModel at runtime if
        // the body-model swap in CustomLegModelSwap also succeeded. If that swap failed (or this
        // layer somehow ended up on a renderer whose body model was never swapped), skip the sync
        // and fall through to vanilla behavior: armor stays rigid, exactly as if this class were
        // the stock HumanoidArmorLayer.
        HumanoidModel<AbstractClientPlayer> parentModel = getParentModel();
        if (parentModel instanceof CustomLegPlayerModel legModel) {
            copyShin(legModel.rightShin, innerModel.rightShin, outerModel.rightShin);
            copyShin(legModel.leftShin, innerModel.leftShin, outerModel.leftShin);
        }
        super.render(poseStack, buffer, packedLight, player, limbSwing, limbSwingAmount, partialTick,
                ageInTicks, netHeadYaw, headPitch);
    }

    private static void copyShin(ModelPart source, ModelPart innerTarget, ModelPart outerTarget) {
        innerTarget.copyFrom(source);
        outerTarget.copyFrom(source);
    }
}
