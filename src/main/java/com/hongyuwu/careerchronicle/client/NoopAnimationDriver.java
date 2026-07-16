package com.hongyuwu.careerchronicle.client;

import net.minecraft.world.entity.LivingEntity;

/**
 * Degrade-to-vanilla driver used when a conflicting mod (Epic Fight / FirstPersonMod, see
 * {@link AnimationDriverRegistry}) is detected at {@code FMLClientSetupEvent} time
 * (0.4-09a 设计文档 §三.2). {@link #playAnimation} always returns {@code false}, so
 * {@link AnimFxOp} always falls back to vanilla {@code swing} -- this is the intended,
 * "优雅降级" behavior, not an error state.
 */
final class NoopAnimationDriver implements IAnimationDriver {
    @Override
    public boolean playAnimation(LivingEntity entity, String animId, boolean upperBodyOnly, float speed,
            boolean isBasicAttack) {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
