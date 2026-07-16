package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import java.util.Set;
import java.util.function.Function;

/**
 * Chooses which {@link IAnimationDriver} the {@code anim} fx op talks to.
 *
 * <p>At {@code FMLClientSetupEvent} time, probes for mods known to ship their own
 * third-person player animation system that would fight with ours (Epic Fight,
 * FirstPersonMod) and degrades to {@link NoopAnimationDriver} (vanilla swing only) if
 * either is present; otherwise selects {@link CustomSkeletonAnimationDriver} (阶段3-任务4-设计文档-
 * 接入驱动与移除旧库.md §二 -- replaces the former player-animation-lib-backed
 * {@code PlayerAnimatorDriver}, deleted along with that dependency). This is an optional,
 * non-fatal degrade -- detecting a conflicting mod never throws or blocks mod loading.
 *
 * <p>The modid probe is injected as a {@code Function<String, Boolean>} (rather than this
 * class calling {@code net.minecraftforge.fml.ModList.get()::isLoaded} directly) so the
 * selection logic can be exercised in plain JUnit without a real Forge {@code ModList}
 * (0.4-09a 单元测试用例文档 A 组). Production wiring passes {@code ModList.get()::isLoaded}
 * from {@code CareerClientEvents}.
 */
public final class AnimationDriverRegistry {

    /**
     * Mods known to ship their own third-person player animation system that would fight
     * with ours. Spelling matters here -- a typo would silently disable the whole
     * conflict-detection path (0.4-09a 设计文档 §三: "拼写逐个核实" is called out as a risk).
     */
    static final Set<String> CONFLICTING_MODIDS = Set.of("epicfight", "firstpersonmod");

    private static IAnimationDriver instance;

    private AnimationDriverRegistry() {
    }

    /**
     * Selects and stores the active driver. Idempotent: safe to call more than once (mirrors
     * {@code FxOpRegistry.registerBuiltins()}'s guard) -- a second call is a no-op and does not
     * re-invoke the probe.
     *
     * @param modidLoadedProbe returns {@code true} if the given modid is currently loaded.
     */
    public static void init(Function<String, Boolean> modidLoadedProbe) {
        if (instance != null) {
            return;
        }
        boolean conflict = false;
        for (String modid : CONFLICTING_MODIDS) {
            if (Boolean.TRUE.equals(modidLoadedProbe.apply(modid))) {
                conflict = true;
                CareerChronicleMod.LOGGER.info(
                        "[CareerChronicle] Detected {} — CareerChronicle skeletal cast animations disabled, falling back to vanilla swing.",
                        modid);
            }
        }
        instance = conflict ? new NoopAnimationDriver() : new CustomSkeletonAnimationDriver();
    }

    /**
     * The currently active driver. Never {@code null}: if {@link #init} hasn't run yet this
     * defensively returns a fresh {@link NoopAnimationDriver} rather than risk a
     * {@code NullPointerException} in {@link AnimFxOp} -- production always calls
     * {@link #init} during {@code FMLClientSetupEvent}, so this only matters for
     * out-of-order test/init edge cases.
     */
    public static IAnimationDriver current() {
        return instance != null ? instance : new NoopAnimationDriver();
    }

    public static void clearForTesting() {
        instance = null;
    }
}
