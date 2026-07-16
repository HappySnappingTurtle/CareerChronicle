package com.hongyuwu.careerchronicle.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * Stage-1 proof of concept for the custom skeleton engine (自定义骨骼引擎-设计文档-手臂腿部关节扩展.md
 * §五 开发顺序 step 1). Adds a knee joint to the player model by splitting the vanilla, single
 * rigid "leg" cuboid into a "thigh" (kept on the existing {@code right_leg}/{@code left_leg}
 * part names/pivots so everything that reads {@link net.minecraft.client.model.HumanoidModel}'s
 * {@code rightLeg}/{@code leftLeg} fields keeps working unmodified) plus a new child
 * {@code right_shin}/{@code left_shin} bone pivoting at the knee. Head/hat/body/arms/sleeves/
 * pants/jacket/ear/cloak are untouched -- byte-for-byte the same construction as vanilla
 * {@link PlayerModel#createMesh}.
 *
 * <p><b>Why this extends {@code PlayerModel} and not just {@code HumanoidModel}</b> (confirmed
 * by decompiling the actual 1.20.1 official-mapped classes with ForgeFlower against
 * {@code forge-1.20.1-47.4.20_mapped_official_1.20.1.jar}, not assumed): {@code PlayerRenderer}
 * extends {@code LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>},
 * and its own methods access {@code this.model} typed as {@code PlayerModel} directly (e.g.
 * {@code renderRightHand} does {@code (this.model).rightSleeve}, {@code setModelProperties} does
 * {@code PlayerModel<AbstractClientPlayer> playermodel = this.getModel();}). Those accesses
 * compile to an implicit {@code checkcast} to {@code PlayerModel} at each call site (the field's
 * erased declared type in the {@code LivingEntityRenderer} superclass is just
 * {@code EntityModel}). A model that is only a {@code HumanoidModel} -- not a {@code PlayerModel}
 * -- would pass the raw field assignment but throw {@code ClassCastException} the moment
 * {@code PlayerRenderer.render()}/{@code setModelProperties()} runs, i.e. every frame. So the
 * swapped-in model must be a real {@code PlayerModel} subtype, which is why this class extends
 * it and builds the full vanilla mesh (jacket/pants/sleeves/ear/cloak included) rather than a
 * stripped-down {@code HumanoidModel}-only skeleton.
 */
public class CustomLegPlayerModel extends PlayerModel<AbstractClientPlayer> {

    /** New child bone pivoting at the right knee. Untouched by any vanilla animation code. */
    public final ModelPart rightShin;
    /** New child bone pivoting at the left knee. Untouched by any vanilla animation code. */
    public final ModelPart leftShin;

    /**
     * 阶段3-任务6 bugfix: this frame's {@code partialTick}, stamped by
     * {@link CustomLegModelSwap#onRenderPlayerPre} (which still fires on {@code RenderPlayerEvent.Pre}
     * -- before {@link #setupAnim} runs, see that class's updated doc -- so this is the only way to
     * hand {@code partialTick} to {@link #setupAnim}, whose vanilla-fixed signature doesn't carry it).
     * Read once per {@link #setupAnim} call, immediately after being written; never stale across
     * entities because both the write and the read happen inside the same single-threaded render
     * pass for whichever entity is currently being processed.
     */
    public float pendingPartialTick;

    public CustomLegPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
        this.rightShin = root.getChild("right_leg").getChild("right_shin");
        this.leftShin = root.getChild("left_leg").getChild("left_shin");
    }

    /**
     * 阶段3-任务6 bugfix (root cause of "animated bones render as if nothing happened"): confirmed
     * by decompiling {@code PlayerRenderer.render()} that {@code RenderPlayerEvent.Pre} posts
     * *before* {@code super.render()} -- which is what actually calls {@code this.model.setupAnim
     * (...)} -- so anything written to vanilla bones (rightArm/leftArm/rightLeg/leftLeg/body, and
     * their outer-layer counterparts) from a {@code RenderPlayerEvent.Pre} handler gets silently
     * overwritten the instant this method runs afterward. There is no Forge event between
     * {@code setupAnim()} and the actual {@code renderToBuffer()} draw call either (confirmed by
     * decompiling {@code LivingEntityRenderer.render()}), so overriding this method -- and applying
     * our pose *after* the {@code super.setupAnim()} call below -- is the only place in the whole
     * render pipeline where a write actually survives to the final frame. Shin bones were never
     * affected by this bug (vanilla's own {@code setupAnim()} doesn't know they exist, so writing
     * them from {@code RenderPlayerEvent.Pre} was never at risk of being overwritten) -- which is
     * exactly why the knee-bend was visible in earlier validation while whole-thigh/whole-arm
     * animation data was not.
     */
    @Override
    public void setupAnim(AbstractClientPlayer entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        CustomAnimationPlayer player = CustomAnimationPlayers.getIfPresent(entity.getUUID());
        CustomAnimationPlayers.applyOrReset(player, this, pendingPartialTick);
        applyDebugBoneAxes();
    }

    /** 阶段3-任务6 诊断: applies {@code /careerbone}'s per-(bone, axis) overrides, same
     * post-super.setupAnim() timing requirement as the real animation data above. Written directly
     * (not via {@code pitch/yaw/roll} degrees->radians on a {@link BonePose}) since debug axes are
     * set/queried independently, not as a single already-combined pose. */
    private void applyDebugBoneAxes() {
        for (java.util.Map.Entry<Bone, float[]> entry : CustomLegModelSwap.debugBoneAxes().entrySet()) {
            float[] axes = entry.getValue();
            if (axes[0] == 0F && axes[1] == 0F && axes[2] == 0F) {
                continue;
            }
            ModelPart part = switch (entry.getKey()) {
                case HEAD -> this.head;
                case BODY -> this.body;
                case RIGHT_ARM -> this.rightArm;
                case LEFT_ARM -> this.leftArm;
                case RIGHT_LEG -> this.rightLeg;
                case LEFT_LEG -> this.leftLeg;
                case RIGHT_SHIN -> this.rightShin;
                case LEFT_SHIN -> this.leftShin;
            };
            part.xRot = axes[0];
            part.yRot = axes[1];
            part.zRot = axes[2];
            ModelPart overlay = switch (entry.getKey()) {
                case BODY -> this.jacket;
                case RIGHT_ARM -> this.rightSleeve;
                case LEFT_ARM -> this.leftSleeve;
                case RIGHT_LEG -> this.rightPants;
                case LEFT_LEG -> this.leftPants;
                default -> null;
            };
            if (overlay != null) {
                overlay.xRot = axes[0];
                overlay.yRot = axes[1];
                overlay.zRot = axes[2];
            }
        }
    }

    /**
     * Mirrors {@code LayerDefinitions.createRoots()}'s own construction of
     * {@code ModelLayers.PLAYER}/{@code PLAYER_SLIM}
     * ({@code LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64)}),
     * except the leg cuboid is split at the knee. Registered under a brand-new
     * {@code ModelLayerLocation} (see {@link CustomLegModelSwap}) -- never under
     * {@code ModelLayers.PLAYER} itself, since {@code LayerDefinitions.createRoots()} merges every
     * registered layer into one {@code ImmutableMap.Builder} and calls {@code buildOrThrow()};
     * a duplicate key there crashes the game at startup (confirmed by reading that method).
     */
    public static LayerDefinition createBodyLayer(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        PartDefinition root = mesh.getRoot();

        // Vanilla HumanoidModel.createMesh() builds "right_leg" as one rigid 4x12x4 cuboid
        // pivoting at the hip, offset (-1.9, 12, 0); PlayerModel.createMesh() leaves that as-is
        // and only re-defines "left_leg" (non-mirrored, its own dedicated new-format skin UV
        // region at texOffs(16, 48)). Both numbers below are copied verbatim from the decompiled
        // source, not approximated.
        //
        // Split: the thigh keeps the existing part name/pivot (so HumanoidModel.rightLeg/leftLeg
        // still resolve correctly for every bit of vanilla code that reads them) but only spans
        // the top half of the old cuboid (local y 0..6, i.e. hip to knee). A new child bone
        // ("right_shin"/"left_shin") pivots at the knee (local y 6, i.e. 6 units below the hip
        // pivot in the parent's own local space) and spans the bottom half (local y 0..6 in its
        // own frame, i.e. knee to foot) -- together they reconstruct the original 12-unit leg
        // length exactly.
        //
        // Texture: stage 1 deliberately reuses the same texOffs anchor for both thigh and shin
        // (both segments sample the same slice of the original leg texture region) instead of
        // splitting the UV precisely -- per 自定义骨骼引擎-设计文档 §3.3 this is an accepted
        // "roughly lines up, no obvious misalignment" approximation for this stage; a later stage
        // can split the UV rectangle for a pixel-accurate look.
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild("right_shin",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        leftLeg.addOrReplaceChild("left_shin",
                CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
