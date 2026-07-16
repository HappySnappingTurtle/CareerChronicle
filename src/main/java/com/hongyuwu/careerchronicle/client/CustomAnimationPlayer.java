package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 阶段3-任务2-设计文档-核心播放引擎.md. Manages "which clip is playing and how far into it" for one
 * entity, and applies its sampled pose onto a {@link CustomLegPlayerModel} each frame. Does not
 * itself do any interpolation math -- that's {@link AnimationClip#sample}; this class is purely
 * playback state + the write-into-ModelPart step.
 *
 * <p>Not thread-safe; one instance per animated entity, owned and ticked by whatever holds it
 * (阶段3-任务4's driver will own a {@code Map<UUID, CustomAnimationPlayer>}).
 */
public final class CustomAnimationPlayer {

    private static final float DEGREES_TO_RADIANS = (float) Math.PI / 180F;

    /**
     * 引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7): which system asked for the
     * currently-playing clip, so {@link #play(AnimationClip, float, ClipSource)} can arbitrate
     * between the two call sites that both ultimately reach this class ({@code AnimFxOp}, for both
     * skill {@code cast} fx and {@code BasicAttackAnimationEvents}' basic-attack fx -- see
     * {@code FxDispatcher.dispatchBasicAttack}'s own doc for why they share one wire path).
     */
    public enum ClipSource { SKILL, BASIC_ATTACK }

    private AnimationClip clip;
    private float elapsedTicks;
    private float speed = 1F;
    private ClipSource currentSource = ClipSource.SKILL;

    /** Hard-cuts to a new clip at normal speed, sourced as {@link ClipSource#SKILL} (existing
     * callers -- the JUnit suite predating A7 -- never needed to distinguish source, so this
     * overload keeps their exact pre-A7 behavior: always cuts in, never arbitrated away). No
     * cross-fade/blend -- see 设计文档 §三 for why a hard cut is the deliberate choice here.
     *
     * @return {@code true} if playback actually started (always true for this overload; mirrors
     *         {@link #play(AnimationClip, float)}'s return type so callers can treat both the
     *         same way). */
    public boolean play(AnimationClip newClip) {
        return play(newClip, 1F);
    }

    /** Same as {@link #play(AnimationClip)}, but advances the timeline at {@code speed}× per game
     * tick (阶段3-任务4-设计文档-接入驱动与移除旧库.md §二: forwarded from the {@code anim} fx
     * component's {@code speed} param, unlike the player-animation-lib driver this replaces,
     * which couldn't support it -- our own engine can for free). Sourced as {@link ClipSource#SKILL}
     * -- see {@link #play(AnimationClip)}'s doc for why.
     *
     * @return {@code true} if playback started, {@code false} if {@code speed} was rejected. */
    public boolean play(AnimationClip newClip, float speed) {
        return play(newClip, speed, ClipSource.SKILL);
    }

    /**
     * 引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7): source-aware entry point --
     * a {@link ClipSource#BASIC_ATTACK} clip is rejected outright while a {@link ClipSource#SKILL}
     * clip is currently playing (skill cast animations "win"; a basic attack landing mid-cast
     * simply doesn't get its own animation this swing, same as any other rejected fx param). A
     * {@code SKILL} clip always cuts in regardless of what's currently playing -- unlike the
     * BASIC_ATTACK->BASIC_ATTACK and SKILL->SKILL cases (unchanged hard-cut, same as before A7),
     * this is the one new arbitration rule.
     *
     * <p>引擎审计修复 任务A / A2 (表现引擎全面审计报告_2026-07-15.md A2): {@code speed <= 0} is
     * rejected instead of started -- {@code speed == 0} would freeze {@link #elapsedTicks} forever
     * (a non-looping clip's {@link #isPlaying} never becomes false, so the entry in
     * {@code CustomAnimationPlayers}' registry would never be cleaned up, and the entity would be
     * frozen in its first frame's pose indefinitely) and negative speed would run the timeline
     * backwards, neither of which any legitimate {@code anim} fx component data should produce.
     *
     * @return {@code true} if playback started, {@code false} if {@code speed} was rejected, or if
     *         a {@code BASIC_ATTACK} clip lost the arbitration against an in-progress {@code SKILL}
     *         clip (the previous clip, if any, keeps playing unchanged in both rejection cases --
     *         same "don't disturb what's already working" policy as any other rejected fx param). */
    public boolean play(AnimationClip newClip, float speed, ClipSource source) {
        if (speed <= 0F) {
            CareerChronicleMod.LOGGER.warn(
                    "[CareerChronicle] CustomAnimationPlayer: rejected play() of '{}' with non-positive speed {}",
                    newClip == null ? "null" : newClip.id(), speed);
            return false;
        }
        if (source == ClipSource.BASIC_ATTACK && isPlaying() && currentSource == ClipSource.SKILL) {
            return false;
        }
        this.clip = newClip;
        this.elapsedTicks = 0F;
        this.speed = speed;
        this.currentSource = source;
        return true;
    }

    /** Advances playback by one game tick, scaled by the current clip's speed multiplier. No-op
     * if nothing is playing. */
    public void tick() {
        if (clip != null) {
            elapsedTicks += speed;
        }
    }

    /** {@code true} while the current clip still has ticks left to play (always {@code true} for
     * a looping clip once started). {@code false} before any {@link #play} call, or once a
     * non-looping clip has run past its {@code duration_ticks}. Callers deciding whether to keep
     * calling {@link #applyTo} (e.g. 阶段3-任务3's per-tick hook) should stop once this is
     * {@code false} -- {@link #applyTo} itself keeps working past the end (clamped to the last
     * keyframe by {@link AnimationClip#sample}) so it's never unsafe to call, just no longer the
     * caller's job to keep calling. */
    public boolean isPlaying() {
        return clip != null && (clip.loop() || elapsedTicks < clip.durationTicks());
    }

    /**
     * Writes the current pose onto {@code model}. Only bones that have a track in the playing
     * clip are touched -- {@link Bone}s absent from {@code clip.tracks()} are left completely
     * alone (not zeroed, not read), which is what makes "upper-body-only" animations not fight
     * with vanilla's own per-frame leg-swing computation (阶段3-任务3's concern, not this class's).
     *
     * @param partialTick sub-tick render interpolation fraction ({@code [0, 1)}), added to the
     *                     integer {@link #elapsedTicks} for smooth animation between game ticks.
     */
    public void applyTo(CustomLegPlayerModel model, float partialTick) {
        if (clip == null) {
            return;
        }
        float time = elapsedTicks + partialTick;
        for (Bone bone : Bone.values()) {
            clip.sample(bone, time).ifPresent(pose -> {
                writeToPart(resolve(model, bone), pose);
                ModelPart overlay = resolveOverlay(model, bone);
                if (overlay != null) {
                    writeToPart(overlay, pose);
                }
            });
        }
    }

    /**
     * 引擎审计修复 任务A / 决策D1 (表现引擎全面审计报告_2026-07-15.md A1): rotation only -- this
     * used to also unconditionally write {@code part.x/y/z}, which forced every tracked bone's
     * pivot to (0,0,0) every frame (since no shipped clip ever declared a position, {@link BonePose}
     * always defaulted them to 0). See {@link Keyframe}'s doc for the full story and why the fix is
     * "remove the channel" rather than "fix the default".
     */
    private static void writeToPart(ModelPart part, BonePose pose) {
        part.xRot = pose.pitch() * DEGREES_TO_RADIANS;
        part.yRot = pose.yaw() * DEGREES_TO_RADIANS;
        part.zRot = pose.roll() * DEGREES_TO_RADIANS;
    }

    private static ModelPart resolve(CustomLegPlayerModel model, Bone bone) {
        return switch (bone) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
            case RIGHT_SHIN -> model.rightShin;
            case LEFT_SHIN -> model.leftShin;
        };
    }

    /**
     * 阶段3-任务6 bugfix: {@code PlayerModel.setupAnim()} does {@code rightPants.copyFrom(rightLeg)}
     * (and the equivalent for leftPants/leftSleeve/rightSleeve/jacket) exactly once, inside its own
     * single call -- confirmed by decompiling {@code PlayerModel.java:94-98}. Since
     * {@code RenderPlayerEvent.Pre} (where {@link #applyTo} runs) fires after that call already
     * happened, writing only the inner bone (e.g. {@code rightLeg}) leaves the *outer* skin layer
     * (e.g. {@code rightPants} -- the fully-opaque part actually visible on most skins) frozen at
     * vanilla's stand/walk pose, completely hiding whatever the inner bone just did. Every bone
     * with such an outer layer must get the same pose written to both. Shin has no vanilla-defined
     * outer layer at all (confirmed: it doesn't exist in vanilla's part tree), so it needs no
     * counterpart -- this is exactly why the shin-only knee-bend was visible in earlier validation
     * while whole-thigh rotations (this bug) were not.
     */
    private static ModelPart resolveOverlay(CustomLegPlayerModel model, Bone bone) {
        return switch (bone) {
            case BODY -> model.jacket;
            case RIGHT_ARM -> model.rightSleeve;
            case LEFT_ARM -> model.leftSleeve;
            case RIGHT_LEG -> model.rightPants;
            case LEFT_LEG -> model.leftPants;
            case HEAD, RIGHT_SHIN, LEFT_SHIN -> null;
        };
    }
}
