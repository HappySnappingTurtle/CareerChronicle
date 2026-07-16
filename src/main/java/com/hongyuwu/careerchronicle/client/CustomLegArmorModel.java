package com.hongyuwu.careerchronicle.client;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * Armor-side half of 自定义骨骼引擎-设计文档-护甲跟随腿部弯曲.md §3.1. {@code HumanoidArmorLayer} copies
 * only the six vanilla body parts onto its armor model every frame (see
 * {@code HumanoidModel#copyPropertiesTo}, confirmed by decompiling forge-1.20.1-47.4.20 -- it does
 * {@code rightLeg.copyFrom}/{@code leftLeg.copyFrom} and nothing else), so it never learns about
 * the new shin joint on its own. This class gives the armor model the matching shin bones;
 * {@link CustomLegArmorLayer} is what actually copies the angle across each frame.
 *
 * <p>Mirrors {@link CustomLegPlayerModel}'s thigh/shin split exactly (same part names/pivots, so
 * anything reading {@code HumanoidModel.rightLeg}/{@code leftLeg} still works), except for one
 * deliberate texture difference: vanilla armor texture is the old 64x32 layout where the boot
 * pixels live in the *lower* half of the leg's V-range. Reusing the thigh's {@code texOffs} for
 * the shin (as the stage-1 player skin does, since a skin's leg region has no boots to lose) would
 * make boots sample the *upper* half instead and render invisible. So thigh keeps the original
 * anchor and shin is offset down by the box depth-derived side-face height (dz=4 -> +6 in V) to
 * land on the lower half where boot pixels actually are.
 */
public class CustomLegArmorModel extends HumanoidArmorModel<AbstractClientPlayer> {

    public final ModelPart rightShin;
    public final ModelPart leftShin;

    public CustomLegArmorModel(ModelPart root) {
        super(root);
        this.rightShin = root.getChild("right_leg").getChild("right_shin");
        this.leftShin = root.getChild("left_leg").getChild("left_shin");
    }

    /**
     * Mirrors {@code HumanoidArmorModel.createBodyLayer(CubeDeformation)} (confirmed by
     * decompiling the real 1.20.1 class -- base mesh is
     * {@code HumanoidModel.createMesh(deformation, 0.0F)}, legs use {@code deformation.extend(-0.1F)}
     * and 64x32 UV, both leg parts sample the same {@code texOffs(0, 16)} anchor with
     * {@code .mirror()} on the left leg since armor textures have no independent left-leg UV
     * region), except the leg cuboid is split at the knee the same way {@link CustomLegPlayerModel}
     * splits the body mesh.
     */
    public static MeshDefinition createBodyLayer(CubeDeformation deformation) {
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();

        CubeDeformation legDeformation = deformation.extend(-0.1F);

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, legDeformation),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild("right_shin",
                CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, legDeformation),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, legDeformation),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        leftLeg.addOrReplaceChild("left_shin",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, legDeformation),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        return mesh;
    }
}
