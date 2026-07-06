package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.ClassDef;
import com.hongyuwu.careerchronicle.data.RegistrySnapshot;
import com.hongyuwu.careerchronicle.data.SkillDef;
import com.hongyuwu.careerchronicle.data.UpgradeRule;
import com.hongyuwu.careerchronicle.player.ICareerData;
import net.minecraft.resources.ResourceLocation;

public final class SkillLevelService {
    private SkillLevelService() {
    }

    public static int levelOf(ICareerData data, SkillDef skill) {
        if (skill.upgrade() == null || UpgradeRule.NONE.equals(skill.upgrade())) {
            return 1;
        }
        return levelOf(data, skill.upgrade());
    }

    public static int levelOf(ICareerData data, UpgradeRule rule) {
        if (rule == null || "none".equals(rule.source())) {
            return 1;
        }
        int score = resolveScore(data, rule.source());
        int level = Math.max(1, score);
        return Math.min(rule.maxLevel(), level);
    }

    static int resolveScore(ICareerData data, String source) {
        if (source.startsWith("tag:")) {
            String tagPath = source.substring(4);
            ResourceLocation tagId = ResourceLocation.fromNamespaceAndPath(
                    CareerChronicleMod.MOD_ID, tagPath);
            return tagScoreFromHistory(data, tagId);
        }
        if (source.startsWith("class:")) {
            String classPath = source.substring(6);
            ResourceLocation classId = ResourceLocation.fromNamespaceAndPath(
                    CareerChronicleMod.MOD_ID, classPath);
            return classCountFromHistory(data, classId);
        }
        return 1;
    }

    static int tagScoreFromHistory(ICareerData data, ResourceLocation tagId) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        int score = 0;
        for (ResourceLocation classId : data.getClassHistory()) {
            ClassDef classDef = registry.classes().get(classId);
            if (classDef != null && classDef.tags().contains(tagId)) {
                score++;
            }
        }
        return score;
    }

    static int classCountFromHistory(ICareerData data, ResourceLocation classId) {
        int count = 0;
        for (ResourceLocation id : data.getClassHistory()) {
            if (id.equals(classId)) {
                count++;
            }
        }
        return count;
    }
}
