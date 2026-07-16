package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.network.FxParams;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code anim} fx op (0.4-06 §2.3, upgraded 0.4-09a §三.1).
 *
 * <p>Reads the {@code id} (String, may be absent), {@code upper_body} (boolean, default
 * {@code true}) and {@code speed} (float, default {@code 1.0}) params. If {@code id} is
 * absent, this behaves exactly like the pre-0.4-09a placeholder: unconditional vanilla
 * {@code swing} (skills that haven't been updated with an anim component keep working
 * unchanged). If {@code id} is present, it asks {@link AnimationDriverRegistry#current()}
 * to play that animation; a {@code false} return value, a {@code null}/non-living entity
 * lookup, or the driver throwing all fall back to the same vanilla {@code swing} -- no
 * failure at any layer here is allowed to propagate and break other fx (sound/particles/
 * shake) in the same cast (0.4-06's established "driver failure must not affect other fx"
 * convention).
 */
final class AnimFxOp implements FxOp {
    @Override
    public void play(FxPlayContext ctx, CompoundTag params) {
        Entity entity = ctx.level().getEntity(ctx.casterId());
        resolveAndPlay(entity, params, AnimationDriverRegistry.current());
    }

    /**
     * Package-private so the entity-resolution edge case (B7: caster not found / not living) can
     * be unit tested by passing {@code null} directly, without needing a real {@code ClientLevel}
     * (which needs a Minecraft bootstrap this test environment doesn't have -- mirrors the
     * historical split documented in {@code FxOpRegistryTest}'s class doc).
     */
    static void resolveAndPlay(Entity entity, CompoundTag params, IAnimationDriver driver) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        applyAnimation(params, driver, living, () -> living.swing(InteractionHand.MAIN_HAND));
    }

    /**
     * Package-private core, fully unit-testable without any real/mocked Minecraft entity: a real
     * {@code LivingEntity} can't be constructed OR mocked in plain JUnit here (both need a
     * {@code Bootstrap.bootStrap()} call for registries that this test environment doesn't run --
     * confirmed empirically: Mockito's inline mock maker fails to even instrument
     * {@code LivingEntity} for the same reason). So {@code living} is treated as fully opaque by
     * this method -- it is only ever forwarded to {@code driver.playAnimation(...)}, never
     * dereferenced here -- which means tests can safely pass {@code null} for it as long as their
     * fake driver doesn't dereference it either. The actual swing side effect is performed
     * through the injected {@code swingAction} callback instead of calling
     * {@code living.swing(...)} directly, so "was swing called" stays verifiable without a real
     * entity.
     */
    static void applyAnimation(CompoundTag params, IAnimationDriver driver, LivingEntity living, Runnable swingAction) {
        String animId = FxParams.getString(params, "id");
        if (animId == null) {
            swingAction.run();
            return;
        }
        boolean upperBodyOnly = FxParams.getBoolean(params, "upper_body", true);
        float speed = FxParams.getFloat(params, "speed", 1.0F);
        // 引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7): "basic_attack" is set only
        // by FxDispatcher.dispatchBasicAttack -- every skill-authored fx component in skill JSON
        // omits 'source', so this defaults to false (ClipSource.SKILL) for every pre-A7 caller.
        boolean isBasicAttack = "basic_attack".equals(FxParams.getString(params, "source"));
        boolean played = false;
        try {
            played = driver != null && driver.playAnimation(living, animId, upperBodyOnly, speed, isBasicAttack);
        } catch (RuntimeException e) {
            CareerChronicleMod.LOGGER.warn("AnimFxOp: driver failed to play anim '{}', falling back to swing: {}",
                    animId, e.toString());
        }
        if (!played) {
            swingAction.run();
        }
    }
}
