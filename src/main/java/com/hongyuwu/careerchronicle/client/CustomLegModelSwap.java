package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Stage-1 proof of concept (自定义骨骼引擎-设计文档-手臂腿部关节扩展.md §3.1/§3.2, D 组单元测试用例文档):
 * bakes {@link CustomLegPlayerModel} from a brand-new {@link ModelLayerLocation} (never
 * {@code ModelLayers.PLAYER} -- reusing that key crashes the game at startup, confirmed by
 * reading {@code LayerDefinitions.createRoots()}: every registered layer, vanilla and modded
 * alike, is merged into one {@code ImmutableMap.Builder} and finished with
 * {@code buildOrThrow()}), then reflectively swaps it into the running {@code PlayerRenderer}
 * instances' {@code protected M model;} field (declared on {@code LivingEntityRenderer}).
 *
 * <p><b>Failure policy (D1/D2):</b> any reflection failure (missing field, wrong type, a future
 * Forge/MC version renaming the field, etc.) is caught, logged at ERROR, and left as a no-op --
 * the affected player renderer keeps its vanilla model and the game behaves exactly as if this
 * class were absent. This mirrors the 0.4-09a lesson (a mods.toml version-string typo took the
 * whole mod down): a new, unproven dependency point must never be able to crash
 * {@code FMLClientSetupEvent}/{@code EntityRenderersEvent.AddLayers} or block mod loading.
 */
public final class CustomLegModelSwap {

    static final ModelLayerLocation CUSTOM_PLAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "custom_player"), "main");
    static final ModelLayerLocation CUSTOM_PLAYER_SLIM = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "custom_player_slim"), "main");

    // Only two keys, not four: confirmed by decompiling LayerDefinitions.createRoots() that
    // ModelLayers.PLAYER_SLIM_INNER_ARMOR/PLAYER_SLIM_OUTER_ARMOR are registered against the exact
    // same LayerDefinition instances as their non-slim counterparts -- vanilla armor geometry does
    // not have a slim variant (no sleeves to taper), so one inner + one outer definition covers
    // both the default and slim player renderers.
    static final ModelLayerLocation CUSTOM_ARMOR_INNER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "custom_player_armor_inner"), "main");
    static final ModelLayerLocation CUSTOM_ARMOR_OUTER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "custom_player_armor_outer"), "main");

    // Kept so the stage-1 debug command (below) can push a fixed angle straight onto the exact
    // model instance now wired into the live PlayerRenderer -- these are the *only* model
    // instances for their renderer (Minecraft reuses one model object per renderer for every
    // entity of that type), so writing to rightShin/leftShin here is visible immediately without
    // any further plumbing.
    private static volatile CustomLegPlayerModel defaultModel;
    private static volatile CustomLegPlayerModel slimModel;
    // 阶段3-任务3: which renderer instance each swapped-in model belongs to, so the per-frame
    // animation hook (CustomAnimationPlayers.onRenderPlayerPre) can resolve "which of our two
    // model instances is this specific render call using" via a cheap identity check -- no
    // reflection needed at render time (only the one-time swap in trySwap needs it).
    private static volatile LivingEntityRenderer<?, ?> defaultRenderer;
    private static volatile LivingEntityRenderer<?, ?> slimRenderer;
    private static volatile boolean swapSucceeded;
    private static volatile boolean defaultSwapSucceeded;
    private static volatile boolean slimSwapSucceeded;
    private static volatile boolean armorSwapSucceeded;
    private static volatile boolean debugAngleActive;
    // 阶段3-任务6 诊断: generic "which axis of which vanilla bone is set to what debug angle"
    // table, so /careerbone can probe any bone/axis combination without a bespoke setter per
    // bone. Unlike rightShin (vanilla never touches it, see setShinPitchDegrees' javadoc), these
    // are all bones vanilla setupAnim() recomputes every frame -- a one-shot write would be
    // silently overwritten the very next frame, so this must be re-read and re-applied every
    // single setupAnim() call (see CustomLegPlayerModel#setupAnim), same reasoning as
    // CustomAnimationPlayers' own per-frame re-application.
    private static final java.util.Map<Bone, float[]> DEBUG_BONE_AXES = new java.util.concurrent.ConcurrentHashMap<>();

    private CustomLegModelSwap() {
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CUSTOM_PLAYER, () -> CustomLegPlayerModel.createBodyLayer(false));
        event.registerLayerDefinition(CUSTOM_PLAYER_SLIM, () -> CustomLegPlayerModel.createBodyLayer(true));
        // CustomLegArmorModel.createBodyLayer returns a MeshDefinition (mirrors vanilla
        // HumanoidArmorModel.createBodyLayer's own signature) -- LayerDefinitions.createRoots()
        // wraps that the same way for every vanilla armor layer: LayerDefinition.create(mesh, 64, 32).
        event.registerLayerDefinition(CUSTOM_ARMOR_INNER, () ->
                net.minecraft.client.model.geom.builders.LayerDefinition.create(
                        CustomLegArmorModel.createBodyLayer(LayerDefinitions.INNER_ARMOR_DEFORMATION), 64, 32));
        event.registerLayerDefinition(CUSTOM_ARMOR_OUTER, () ->
                net.minecraft.client.model.geom.builders.LayerDefinition.create(
                        CustomLegArmorModel.createBodyLayer(LayerDefinitions.OUTER_ARMOR_DEFORMATION), 64, 32));
    }

    public static void swapPlayerModels(EntityRenderersEvent.AddLayers event) {
        boolean okDefault = trySwap(event, "default", CUSTOM_PLAYER, false);
        boolean okSlim = trySwap(event, "slim", CUSTOM_PLAYER_SLIM, true);
        defaultSwapSucceeded = okDefault;
        slimSwapSucceeded = okSlim;
        swapSucceeded = okDefault || okSlim;
        if (swapSucceeded) {
            CareerChronicleMod.LOGGER.info(
                    "[CareerChronicle] CustomLegPlayerModel reflective swap succeeded (default={}, slim={}).",
                    okDefault, okSlim);
        } else {
            CareerChronicleMod.LOGGER.error(
                    "[CareerChronicle] CustomLegPlayerModel reflective swap failed for both player skins; "
                            + "PlayerRenderer keeps the vanilla model, game continues normally.");
        }

        boolean okArmorDefault = trySwapArmorLayer(event, "default");
        boolean okArmorSlim = trySwapArmorLayer(event, "slim");
        armorSwapSucceeded = okArmorDefault || okArmorSlim;
        if (armorSwapSucceeded) {
            CareerChronicleMod.LOGGER.info(
                    "[CareerChronicle] CustomLegArmorLayer swap succeeded (default={}, slim={}).",
                    okArmorDefault, okArmorSlim);
        } else {
            CareerChronicleMod.LOGGER.error(
                    "[CareerChronicle] CustomLegArmorLayer swap failed for both player skins; "
                            + "equipped armor keeps the vanilla rigid layer, game continues normally.");
        }
    }

    @SuppressWarnings("deprecation") // EntityRenderersEvent.AddLayers#getSkin is Forge-deprecated but still the
                                      // supported way to reach the LivingEntityRenderer for a given skin in 1.20.1.
    private static boolean trySwap(EntityRenderersEvent.AddLayers event, String skinName,
                                    ModelLayerLocation layer, boolean slim) {
        try {
            LivingEntityRenderer<?, ?> renderer = event.getSkin(skinName);
            if (renderer == null) {
                CareerChronicleMod.LOGGER.error(
                        "[CareerChronicle] No player renderer registered for skin '{}'; skipping leg-joint model swap.",
                        skinName);
                return false;
            }

            ModelPart root = event.getContext().bakeLayer(layer);
            CustomLegPlayerModel model = new CustomLegPlayerModel(root, slim);

            Field modelField = findModelField(renderer.getClass());
            modelField.setAccessible(true);
            modelField.set(renderer, model);

            if (slim) {
                slimModel = model;
                slimRenderer = renderer;
            } else {
                defaultModel = model;
                defaultRenderer = renderer;
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            CareerChronicleMod.LOGGER.error(
                    "[CareerChronicle] Failed to swap in CustomLegPlayerModel for skin '{}' -- "
                            + "falling back to vanilla player model.", skinName, e);
            return false;
        }
    }

    /** Walks up from the renderer's runtime class since {@code model} is declared on the
     * {@code LivingEntityRenderer} superclass, not on {@code PlayerRenderer} itself --
     * {@code getDeclaredField} only looks at the exact class it's called on. */
    private static Field findModelField(Class<?> rendererClass) throws NoSuchFieldException {
        return findField(rendererClass, "model");
    }

    /** Same walk-up-to-superclass need as {@link #findModelField}, for the
     * {@code protected final List<RenderLayer<T, M>> layers} field (also declared on
     * {@code LivingEntityRenderer}, confirmed by decompiling the real class -- not assumed). */
    private static Field findLayersField(Class<?> rendererClass) throws NoSuchFieldException {
        return findField(rendererClass, "layers");
    }

    private static Field findField(Class<?> rendererClass, String name) throws NoSuchFieldException {
        Class<?> current = rendererClass;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("'" + name + "' field not found on " + rendererClass + " or any superclass");
    }

    /**
     * Replaces the vanilla {@code HumanoidArmorLayer} instance inside the renderer's
     * {@code layers} list with a {@link CustomLegArmorLayer} in place ({@code list.set(i, ...)}).
     * Reads the list via reflection (same justification as {@link #trySwap}: {@code layers} is
     * {@code protected} on a superclass in a different package, so plain field access from this
     * class isn't legal Java without either subclassing or reflection) but only mutates the list's
     * *contents*, not the field reference itself -- gentler than the body-model swap, which
     * overwrites the field.
     */
    @SuppressWarnings("deprecation") // EntityRenderersEvent.AddLayers#getSkin, see trySwap's own note.
    private static boolean trySwapArmorLayer(EntityRenderersEvent.AddLayers event, String skinName) {
        try {
            LivingEntityRenderer<?, ?> renderer = event.getSkin(skinName);
            if (renderer == null) {
                CareerChronicleMod.LOGGER.error(
                        "[CareerChronicle] No player renderer registered for skin '{}'; skipping armor layer swap.",
                        skinName);
                return false;
            }

            ModelPart innerRoot = event.getContext().bakeLayer(CUSTOM_ARMOR_INNER);
            ModelPart outerRoot = event.getContext().bakeLayer(CUSTOM_ARMOR_OUTER);
            CustomLegArmorModel inner = new CustomLegArmorModel(innerRoot);
            CustomLegArmorModel outer = new CustomLegArmorModel(outerRoot);

            Field layersField = findLayersField(renderer.getClass());
            layersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> layers =
                    (List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>>) layersField.get(renderer);

            int index = -1;
            for (int i = 0; i < layers.size(); i++) {
                if (layers.get(i) instanceof HumanoidArmorLayer) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                CareerChronicleMod.LOGGER.error(
                        "[CareerChronicle] No HumanoidArmorLayer found on renderer for skin '{}'; skipping armor layer swap.",
                        skinName);
                return false;
            }

            @SuppressWarnings("unchecked")
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent =
                    (RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) renderer;
            layers.set(index, new CustomLegArmorLayer(parent, inner, outer, event.getContext().getModelManager()));
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            CareerChronicleMod.LOGGER.error(
                    "[CareerChronicle] Failed to swap in CustomLegArmorLayer for skin '{}' -- "
                            + "falling back to vanilla armor layer.", skinName, e);
            return false;
        }
    }

    public static boolean isSwapSucceeded() {
        return swapSucceeded;
    }

    /** 引擎审计修复 任务A / A4 (表现引擎全面审计报告_2026-07-15.md A4): per-skin swap result, so
     * {@link CustomSkeletonAnimationDriver#playAnimation} can gate on exactly the variant a given
     * player is actually rendered with -- the OR'd {@link #isSwapSucceeded()} would wrongly let a
     * default-skin player "succeed" purely because some *other* player's slim swap happened to
     * work, even though this player's own default-model swap failed. */
    public static boolean isDefaultSwapSucceeded() {
        return defaultSwapSucceeded;
    }

    /** @see #isDefaultSwapSucceeded() */
    public static boolean isSlimSwapSucceeded() {
        return slimSwapSucceeded;
    }

    /** Test-only: {@link #swapPlayerModels} only ever runs from a real
     * {@code EntityRenderersEvent.AddLayers}, which plain JUnit cannot construct -- this lets
     * {@code CustomSkeletonAnimationDriverTest} exercise both swap-outcome branches of
     * {@code CustomSkeletonAnimationDriver.swapSucceededForModelName} directly. */
    static void setSwapSucceededForTesting(boolean defaultOk, boolean slimOk) {
        defaultSwapSucceeded = defaultOk;
        slimSwapSucceeded = slimOk;
        swapSucceeded = defaultOk || slimOk;
    }

    public static boolean isArmorSwapSucceeded() {
        return armorSwapSucceeded;
    }

    /**
     * 阶段3-任务3-设计文档-与原版行走动画共存.md §二: resolves the {@link CustomLegPlayerModel} instance
     * a given (already-rendering) {@code renderer} is using, via identity comparison against the
     * two renderers captured in {@link #trySwap} -- not reflection, since this runs once per
     * rendered player per frame and identity comparison is effectively free.
     *
     * @return the matching model, or {@code null} if {@code renderer} is neither of our two
     *         swapped renderers (e.g. the swap failed entirely, or this is some other entity's
     *         renderer -- both are legitimate "nothing to do here" cases for the caller).
     */
    public static CustomLegPlayerModel modelForRenderer(LivingEntityRenderer<?, ?> renderer) {
        if (renderer == defaultRenderer) {
            return defaultModel;
        }
        if (renderer == slimRenderer) {
            return slimModel;
        }
        return null;
    }

    /**
     * Debug-only (自定义骨骼引擎-单元测试用例文档 E 组 阶段1: "能通过 debug 手段...肉眼确认膝盖处确实有折角").
     * Sets a fixed pitch on both shin bones of whichever swapped-in model(s) exist. Nothing in
     * vanilla code ever touches {@code rightShin}/{@code leftShin}, so the angle sticks across
     * frames until changed again -- no per-tick re-application needed.
     *
     * <p><b>Found during stage-1 in-game verification, not anticipated by the design doc:</b> the
     * vanilla "pants" overlay layer ({@code PlayerModel.leftPants}/{@code rightPants} -- the
     * second skin layer every player skin has, separate from equipped leggings/boots armor) is
     * its own rigid, single, un-split cuboid that {@code PlayerModel.setupAnim()} makes follow
     * only the *thigh* every frame (a plain {@code copyFrom(leftLeg)}/{@code copyFrom(rightLeg)},
     * unaware the shin joint exists) -- exactly the same "doesn't know about the new joint"
     * limitation the design doc §3.3 already calls out for equipped armor, just one layer earlier
     * than expected, and present even with no armor worn at all. On skins that paint non-transparent
     * pixels there it visually masks the shin bend. Since this is purely a debug-visibility
     * concern (not a claim that the real animation engine needs to do this), {@link #onRenderPlayerPre}
     * force-hides just that overlay while a non-zero debug angle is active, so the actual bent
     * mesh underneath is what gets eyeballed.
     *
     * <p>This is a throwaway validation tool for stage 1. It will be replaced by the real JSON
     * animation playback engine in a later stage (自定义骨骼引擎-设计文档 §五 step 3).
     *
     * @return {@code true} if at least one swapped-in model was found and updated.
     */
    public static boolean setShinPitchDegrees(float degrees) {
        boolean any = false;
        float radians = degrees * ((float) Math.PI / 180F);
        CustomLegPlayerModel d = defaultModel;
        if (d != null) {
            d.rightShin.xRot = radians;
            d.leftShin.xRot = radians;
            any = true;
        }
        CustomLegPlayerModel s = slimModel;
        if (s != null) {
            s.rightShin.xRot = radians;
            s.leftShin.xRot = radians;
            any = true;
        }
        debugAngleActive = degrees != 0F;
        return any;
    }

    /** 阶段3-任务6 诊断: generic per-(bone, axis) debug override -- isolates "what does a given
     * rotation on a given vanilla bone actually look like from the default third-person camera"
     * from any animation-clip timing/interpolation variable, so the real pitch/yaw/roll semantics
     * of each bone can be probed empirically instead of assumed. {@code axis} is {@code 'p'}
     * (pitch/xRot), {@code 'y'} (yaw/yRot), or {@code 'r'} (roll/zRot). Unlike shin, this must be
     * re-applied every frame -- this setter only stores the value; the actual write happens in
     * {@link CustomLegPlayerModel#setupAnim}, via {@link #debugBoneAxes()}. */
    public static boolean setDebugBoneAxis(Bone bone, char axis, float degrees) {
        float radians = degrees * ((float) Math.PI / 180F);
        float[] axes = DEBUG_BONE_AXES.computeIfAbsent(bone, b -> new float[3]);
        switch (axis) {
            case 'p' -> axes[0] = radians;
            case 'y' -> axes[1] = radians;
            case 'r' -> axes[2] = radians;
            default -> throw new IllegalArgumentException("Unknown axis '" + axis + "', expected p/y/r");
        }
        return defaultModel != null || slimModel != null;
    }

    public static void clearDebugBoneAxes() {
        DEBUG_BONE_AXES.clear();
    }

    /** Package-private accessor so {@link CustomLegPlayerModel#setupAnim} -- the only point after
     * vanilla's own pose computation where a write actually survives to the final frame -- can
     * apply the debug angles set by {@link #setDebugBoneAxis}. */
    static java.util.Map<Bone, float[]> debugBoneAxes() {
        return DEBUG_BONE_AXES;
    }

    /**
     * Debug-only companion to {@link #setShinPitchDegrees}: {@code PlayerRenderer.render()} calls
     * {@code setModelProperties()} (which unconditionally re-shows the pants overlay every single
     * render call, same as it would for a normal player) *before* posting
     * {@link RenderPlayerEvent.Pre} -- so re-hiding it here, once per render call, cleanly wins
     * with no flicker. No-ops (and does not need to "un-hide" anything) once the angle is reset
     * back to 0 via {@code /careershin reset}, since {@code setModelProperties()} itself will
     * naturally restore normal visibility on the next frame once this stops overriding it.
     */
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        // 阶段3-任务6 bugfix: RenderPlayerEvent.Pre fires *before* PlayerRenderer.render() calls
        // super.render() (which is what actually invokes model.setupAnim()) -- confirmed by
        // decompiling PlayerRenderer.render(). Any vanilla-bone write attempted here (rightLeg,
        // rightArm, etc.) is silently overwritten the instant setupAnim() runs afterward, so the
        // only thing this handler may still safely do is (a) stamp this frame's partialTick onto
        // the model for CustomLegPlayerModel.setupAnim() to consume (setupAnim's fixed vanilla
        // signature has no partialTick parameter of its own), and (b) touch bones vanilla never
        // recomputes (shin's debug-visibility pants-hide below). The actual debug leg-pitch write
        // moved to CustomLegPlayerModel.setupAnim() itself, the only point in the whole render
        // pipeline after setupAnim() and before the draw call.
        CustomLegPlayerModel model = modelForRenderer(event.getRenderer());
        if (model != null) {
            model.pendingPartialTick = event.getPartialTick();
        }
        if (!debugAngleActive) {
            return;
        }
        hidePantsOverlay(defaultModel);
        hidePantsOverlay(slimModel);
    }

    private static void hidePantsOverlay(CustomLegPlayerModel model) {
        if (model == null) {
            return;
        }
        model.leftPants.visible = false;
        model.rightPants.visible = false;
    }

    /** Registers the temporary, client-only {@code /careershin} debug command (own dispatcher via
     * {@link RegisterClientCommandsEvent}, so it never touches the server thread or the existing
     * server-side {@code /career} command tree in {@code CareerCommands}). */
    public static void registerDebugCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("careershin")
                .then(Commands.literal("set")
                        .then(Commands.argument("degrees", FloatArgumentType.floatArg(-180F, 180F))
                                .executes(context -> {
                                    float degrees = FloatArgumentType.getFloat(context, "degrees");
                                    boolean applied = setShinPitchDegrees(degrees);
                                    context.getSource().sendSuccess(() -> Component.literal(applied
                                            ? "[CareerChronicle] rightShin/leftShin xRot set to " + degrees + " degrees."
                                            : "[CareerChronicle] Custom leg model is not active "
                                                    + "(swap failed or hasn't run yet) -- nothing changed."), false);
                                    return applied ? 1 : 0;
                                })))
                .then(Commands.literal("reset")
                        .executes(context -> {
                            boolean applied = setShinPitchDegrees(0F);
                            context.getSource().sendSuccess(() ->
                                    Component.literal("[CareerChronicle] shin angle reset to 0."), false);
                            return applied ? 1 : 0;
                        })));
        // 阶段3-任务6 诊断: generic /careerbone <bone> <p|y|r> <degrees> so any bone/axis
        // combination's real rotation semantics can be probed empirically, instead of assumed
        // (see CustomLegModelSwap#setDebugBoneAxis's doc for why this replaced the earlier
        // rightLeg-only /careerleg command).
        dispatcher.register(Commands.literal("careerbone")
                .then(Commands.literal("set")
                        .then(Commands.argument("bone", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .then(Commands.argument("axis", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .then(Commands.argument("degrees", FloatArgumentType.floatArg(-180F, 180F))
                                                .executes(context -> {
                                                    String boneName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "bone");
                                                    String axisName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "axis");
                                                    float degrees = FloatArgumentType.getFloat(context, "degrees");
                                                    Bone bone = Bone.fromJsonKey(boneName);
                                                    if (bone == null) {
                                                        context.getSource().sendFailure(Component.literal(
                                                                "[CareerChronicle] Unknown bone '" + boneName + "'."));
                                                        return 0;
                                                    }
                                                    char axis = axisName.isEmpty() ? ' ' : Character.toLowerCase(axisName.charAt(0));
                                                    boolean applied;
                                                    try {
                                                        applied = setDebugBoneAxis(bone, axis, degrees);
                                                    } catch (IllegalArgumentException e) {
                                                        context.getSource().sendFailure(Component.literal(
                                                                "[CareerChronicle] " + e.getMessage()));
                                                        return 0;
                                                    }
                                                    context.getSource().sendSuccess(() -> Component.literal(applied
                                                            ? "[CareerChronicle] " + boneName + "." + axis + " set to " + degrees + " degrees."
                                                            : "[CareerChronicle] Custom leg model is not active "
                                                                    + "(swap failed or hasn't run yet) -- nothing changed."), false);
                                                    return applied ? 1 : 0;
                                                })))))
                .then(Commands.literal("reset")
                        .executes(context -> {
                            clearDebugBoneAxes();
                            context.getSource().sendSuccess(() ->
                                    Component.literal("[CareerChronicle] all bone debug overrides cleared."), false);
                            return 1;
                        })));
    }
}
