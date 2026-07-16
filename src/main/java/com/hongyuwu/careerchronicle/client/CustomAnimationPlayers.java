package com.hongyuwu.careerchronicle.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阶段3-任务3-设计文档-与原版行走动画共存.md §二: per-entity registry of {@link CustomAnimationPlayer}s,
 * plus the per-frame "apply the active animation, or reset the shin to neutral" rule that makes
 * 方案1 correct.
 *
 * <p><b>Why the neutral-reset is mandatory, not a nice-to-have</b>: {@link CustomLegPlayerModel}
 * is a singleton reused across every player of one skin type (confirmed in
 * {@link CustomLegModelSwap}'s own comments) -- the same model object renders player A, then
 * player B, then player C, one after another, each frame. If nothing explicitly resets or
 * re-applies the shin pose before each individual player is drawn, one player's bent-knee pose
 * would visually leak onto every other player rendered with the same model instance afterward.
 * So every single render call must land in exactly one of "apply this entity's own animation" or
 * "reset to neutral" -- there is no safe third option of "leave it as it was."
 *
 * <p><b>阶段3-任务6 bugfix:</b> {@link #applyOrReset} is no longer invoked from a
 * {@code RenderPlayerEvent.Pre} handler -- that event fires *before* {@code setupAnim()}
 * (confirmed by decompiling {@code PlayerRenderer.render()}, see
 * {@link CustomLegPlayerModel#setupAnim}'s doc for the full story), so anything it wrote to
 * vanilla-recomputed bones was silently overwritten every frame. {@link CustomLegPlayerModel}
 * itself now calls {@link #applyOrReset} from inside its {@code setupAnim()} override, the only
 * point in the render pipeline where a write actually survives to the final frame.
 */
public final class CustomAnimationPlayers {

    private static final Map<UUID, CustomAnimationPlayer> PLAYERS = new ConcurrentHashMap<>();

    private CustomAnimationPlayers() {
    }

    /** Read-only lookup -- never creates an entry. Used by the render hook so entities that have
     * never played a custom animation don't accumulate a registry entry just from being rendered. */
    public static CustomAnimationPlayer getIfPresent(UUID entityId) {
        return PLAYERS.get(entityId);
    }

    /** Used by 阶段3-任务4's driver when it actually starts playing something -- only entities
     * that try to play at least one animation ever get an entry. */
    public static CustomAnimationPlayer getOrCreate(UUID entityId) {
        return PLAYERS.computeIfAbsent(entityId, id -> new CustomAnimationPlayer());
    }

    /** Advances every registered player by one game tick, then drops entries that are no longer
     * playing (finished non-looping clip, or never started) -- keeps the map from growing
     * unbounded as players log in/out or simply never cast anything relevant again. Call once per
     * client game tick (not per render frame). */
    public static void tickAll() {
        PLAYERS.entrySet().removeIf(entry -> {
            CustomAnimationPlayer player = entry.getValue();
            player.tick();
            return !player.isPlaying();
        });
    }

    /**
     * 阶段3-任务3-单元测试用例文档 A组. Resets the shin bones to their baked neutral pose *first,
     * unconditionally*, then applies the active animation (if any) on top -- if that animation has
     * its own shin track, {@link CustomAnimationPlayer#applyTo} overwrites the reset with the real
     * pose; if it doesn't (e.g. an upper-body-only cast), the reset from this method stands. This
     * order is required, not stylistic: {@link CustomAnimationPlayer#applyTo} leaves untracked
     * bones completely alone, so an upper-body-only animation on player B would otherwise inherit
     * whatever bent shin pose player A's full-body animation left on the *same shared model
     * instance* moments earlier in the same frame (see this class's own doc comment for why the
     * model is shared). Resetting first closes that gap for every caller, not just the "nothing is
     * playing" case.
     *
     * <p><b>引擎审计修复 任务A / A1 (表现引擎全面审计报告_2026-07-15.md A1):</b> uses
     * {@code ModelPart.resetPose()} (all six channels: xRot/yRot/zRot *and* x/y/z) rather than just
     * zeroing {@code xRot} -- defense-in-depth now that {@link Keyframe}/{@link BonePose} no longer
     * carry a positional channel at all (so nothing in this engine writes shin's x/y/z anymore),
     * but cheap insurance against any future regression re-introducing one. Also restores
     * {@code rightLeg.x}/{@code leftLeg.x} to their baked pivot every frame: decompiling vanilla
     * {@code HumanoidModel.setupAnim()} confirmed it recomputes every other vanilla-bone channel
     * touched by this engine (arm x/y/z, leg y/z, body/head rotation) but never leg.x -- so if
     * anything ever writes it (this engine no longer does, but a future bone or a conflicting mod
     * might), there is otherwise no per-frame correction and the corruption would be permanent on
     * this shared model instance until client restart.
     *
     * <p>Thigh/arm/body/head *rotation* is deliberately never touched here -- vanilla
     * {@code setupAnim()} already computed a correct, fresh-per-entity rotation for them this frame
     * before this hook ever runs (see 设计文档 §一), so there both is no leak risk for them and
     * touching them here would fight with vanilla's own animation instead of complementing it.
     */
    static void applyOrReset(CustomAnimationPlayer player, CustomLegPlayerModel model, float partialTick) {
        model.rightShin.resetPose();
        model.leftShin.resetPose();
        model.rightLeg.x = model.rightLeg.getInitialPose().x;
        model.leftLeg.x = model.leftLeg.getInitialPose().x;
        if (player != null && player.isPlaying()) {
            player.applyTo(model, partialTick);
        }
    }

    /** 引擎审计修复 任务A / A2 (表现引擎全面审计报告_2026-07-15.md A2): drops every registered
     * player immediately, instead of waiting for {@link #tickAll}'s per-entry "still playing" check
     * to eventually catch up -- called on logout/world-unload (see {@code CareerClientEvents}) so a
     * clip that was mid-playback (or, before A2's {@code speed<=0} rejection existed, one that could
     * never finish) doesn't survive into the next world under the same player UUID. */
    static void clear() {
        PLAYERS.clear();
    }
}
