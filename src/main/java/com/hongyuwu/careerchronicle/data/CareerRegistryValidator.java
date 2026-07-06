package com.hongyuwu.careerchronicle.data;

import com.hongyuwu.careerchronicle.skill.SkillExecutorRegistry;
import com.hongyuwu.careerchronicle.skill.effect.EffectOpRegistry;
import com.google.gson.JsonObject;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

final class CareerRegistryValidator {
    private CareerRegistryValidator() {
    }

    static void validate(
            Map<ResourceLocation, RaceDef> races,
            Map<ResourceLocation, ClassDef> classes,
            Map<ResourceLocation, SkillDef> skills,
            Map<ResourceLocation, FusionDef> fusions,
            Map<ResourceLocation, HiddenUnlockDef> hiddenUnlocks,
            Map<ResourceLocation, XpSourceDef> xpSources
    ) {
        for (RaceDef race : races.values()) {
            for (ResourceLocation classId : race.allowedClasses()) {
                require(classes.containsKey(classId), "Race " + race.id() + " references missing class " + classId);
            }
        }

        for (ClassDef careerClass : classes.values()) {
            for (ResourceLocation skillId : careerClass.grantsSkills()) {
                require(skills.containsKey(skillId), "Class " + careerClass.id() + " references missing skill " + skillId);
            }
            for (RepeatRewardDef reward : careerClass.repeatRewards()) {
                require(skills.containsKey(reward.unlockSkill()),
                        "Class " + careerClass.id() + " repeat reward references missing skill " + reward.unlockSkill());
            }
            require(!careerClass.hidden() || careerClass.unlockFlag() != null,
                    "Hidden class " + careerClass.id() + " must declare unlock_flag");
        }

        for (SkillDef skill : skills.values()) {
            if (skill.hasComponentEffects()) {
                for (JsonObject effect : skill.effects()) {
                    require(effect.has("op") && effect.get("op").isJsonPrimitive(),
                            "Skill " + skill.id() + " effect missing 'op' field");
                    if (effect.has("op")) {
                        String opName = effect.get("op").getAsString();
                        require(EffectOpRegistry.exists(opName),
                                "Skill " + skill.id() + " references unknown effect op '" + opName + "'");
                    }
                }
            } else {
                require(skill.executor() != null && SkillExecutorRegistry.exists(skill.executor()),
                        "Skill " + skill.id() + " references missing executor " + skill.executor());
            }
            for (ResourceLocation equipmentTag : skill.requirements().equipmentTags()) {
                require(equipmentTag != null,
                        "Skill " + skill.id() + " declares invalid equipment tag");
            }
        }

        for (FusionDef fusion : fusions.values()) {
            require(skills.containsKey(fusion.unlockSkill()),
                    "Fusion " + fusion.id() + " references missing unlock skill " + fusion.unlockSkill());
            require(!fusion.requiredClassCounts().isEmpty(),
                    "Fusion " + fusion.id() + " must declare required_class_counts");
            for (ResourceLocation classId : fusion.requiredClassCounts().keySet()) {
                require(classes.containsKey(classId), "Fusion " + fusion.id() + " references missing class " + classId);
            }
        }

        for (HiddenUnlockDef hiddenUnlock : hiddenUnlocks.values()) {
            require(!hiddenUnlock.requiredClassCounts().isEmpty() || !hiddenUnlock.requiredTagScores().isEmpty(),
                    "Hidden unlock " + hiddenUnlock.id() + " must declare at least one condition");
            for (ResourceLocation classId : hiddenUnlock.requiredClassCounts().keySet()) {
                require(classes.containsKey(classId),
                        "Hidden unlock " + hiddenUnlock.id() + " references missing class " + classId);
            }
        }

        for (XpSourceDef xpSource : xpSources.values()) {
            require(!xpSource.displayKey().isBlank(),
                    "XP source " + xpSource.id() + " must declare display_key");
            require(xpSource.baseAmount() > 0 || xpSource.healthMultiplier() > 0.0D || xpSource.minAmount() > 0,
                    "XP source " + xpSource.id() + " must award positive XP");
            require(xpSource.maxAmount() == 0 || xpSource.maxAmount() >= xpSource.minAmount(),
                    "XP source " + xpSource.id() + " max_amount must be >= min_amount or 0");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new RegistryValidationException(message);
        }
    }
}
