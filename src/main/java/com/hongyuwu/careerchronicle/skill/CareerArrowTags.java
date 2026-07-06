package com.hongyuwu.careerchronicle.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

public final class CareerArrowTags {
    private static final String KEY_SKILL_ID = "careerchronicleSkillId";
    private static final String KEY_FIRE = "careerchronicleFire";
    private static final String KEY_SNARE = "careerchronicleSnare";
    private static final String KEY_HIT_CONSUMED = "careerchronicleHitConsumed";

    private CareerArrowTags() {
    }

    public static void mark(AbstractArrow arrow, ResourceLocation skillId, boolean fire, boolean snare) {
        CompoundTag data = arrow.getPersistentData();
        data.putString(KEY_SKILL_ID, skillId.toString());
        data.putBoolean(KEY_FIRE, fire);
        data.putBoolean(KEY_SNARE, snare);
    }

    public static ResourceLocation skillId(AbstractArrow arrow) {
        String value = arrow.getPersistentData().getString(KEY_SKILL_ID);
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        return parsed == null ? null : parsed;
    }

    public static boolean isCareerArrow(AbstractArrow arrow) {
        return skillId(arrow) != null;
    }

    public static boolean isFireArrow(AbstractArrow arrow) {
        return arrow.getPersistentData().getBoolean(KEY_FIRE);
    }

    public static boolean isSnareArrow(AbstractArrow arrow) {
        return arrow.getPersistentData().getBoolean(KEY_SNARE);
    }

    public static boolean tryConsumeHit(AbstractArrow arrow) {
        CompoundTag data = arrow.getPersistentData();
        if (data.getBoolean(KEY_HIT_CONSUMED)) {
            return false;
        }
        data.putBoolean(KEY_HIT_CONSUMED, true);
        return true;
    }
}
