package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.SkillDef;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import com.hongyuwu.careerchronicle.player.ICareerData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class CareerLoadoutService {
    public static final int ACTIVE_SLOT_COUNT = 4;
    public static final int PASSIVE_SLOT_COUNT = 4;
    public static final int SLOT_TYPE_ACTIVE = 0;
    public static final int SLOT_TYPE_ULTIMATE = 1;
    public static final int SLOT_TYPE_RACE = 2;

    private CareerLoadoutService() {
    }

    public static void setSlot(ServerPlayer player, int slotType, int slot, ResourceLocation skillId) {
        switch (slotType) {
            case SLOT_TYPE_ACTIVE -> setActiveSlot(player, slot, skillId);
            case SLOT_TYPE_ULTIMATE -> setUltimateSlot(player, skillId);
            case SLOT_TYPE_RACE -> setRaceSlot(player, skillId);
            default -> player.displayClientMessage(Component.translatable("careerchronicle.message.invalid_skill_slot")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    public static void setUltimateSlot(ServerPlayer player, ResourceLocation skillId) {
        CareerDataAccess.get(player).ifPresent(data -> {
            if (skillId != null && !validateSkill(player, data, skillId, "ultimate")) {
                return;
            }
            data.setUltimateSlot(skillId);
            CareerDataAccess.sync(player);
            player.displayClientMessage(Component.translatable("careerchronicle.message.loadout_updated")
                    .withStyle(ChatFormatting.AQUA), true);
        });
    }

    public static void setRaceSlot(ServerPlayer player, ResourceLocation skillId) {
        CareerDataAccess.get(player).ifPresent(data -> {
            if (skillId != null && !validateSkill(player, data, skillId, "race")) {
                return;
            }
            data.setRaceSlot(skillId);
            CareerDataAccess.sync(player);
            player.displayClientMessage(Component.translatable("careerchronicle.message.loadout_updated")
                    .withStyle(ChatFormatting.AQUA), true);
        });
    }

    private static boolean validateSkill(ServerPlayer player, ICareerData data, ResourceLocation skillId, String expectedType) {
        if (!data.getUnlockedSkills().contains(skillId)) {
            player.displayClientMessage(Component.translatable("careerchronicle.message.skill_locked",
                    Component.translatable("careerchronicle.skill." + skillId.getPath())).withStyle(ChatFormatting.RED), true);
            return false;
        }
        SkillDef skill = CareerRegistry.snapshot().skills().get(skillId);
        if (skill == null) {
            player.displayClientMessage(Component.translatable("careerchronicle.message.unknown_skill", skillId.toString())
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!expectedType.equals(skill.type())) {
            player.displayClientMessage(Component.translatable("careerchronicle.message.invalid_skill_slot")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    public static void setActiveSlot(ServerPlayer player, int slot, ResourceLocation skillId) {
        if (slot < 0 || slot >= ACTIVE_SLOT_COUNT) {
            player.displayClientMessage(Component.translatable("careerchronicle.message.invalid_skill_slot")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        CareerDataAccess.get(player).ifPresent(data -> {
            if (skillId != null && !data.getUnlockedSkills().contains(skillId)) {
                player.displayClientMessage(Component.translatable("careerchronicle.message.skill_locked",
                        Component.translatable("careerchronicle.skill." + skillId.getPath())).withStyle(ChatFormatting.RED), true);
                return;
            }
            SkillDef skill = skillId == null ? null : CareerRegistry.snapshot().skills().get(skillId);
            if (skillId != null && skill == null) {
                player.displayClientMessage(Component.translatable("careerchronicle.message.unknown_skill", skillId.toString())
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            if (skillId != null && !SkillEquipmentRequirements.hasRequiredEquipment(player, skill)) {
                player.displayClientMessage(Component.translatable("careerchronicle.message.skill_equipment_missing",
                        SkillEquipmentRequirements.requirementText(skill)).withStyle(ChatFormatting.YELLOW), true);
                return;
            }

            List<ResourceLocation> loadout = normalizedLoadout(data);
            loadout.set(slot, skillId);
            if (skillId != null) {
                removeDuplicates(loadout, slot, skillId);
            }
            data.setSkillLoadout(trimTrailingEmpty(loadout));
            CareerDataAccess.sync(player);
            player.displayClientMessage(Component.translatable("careerchronicle.message.loadout_updated")
                    .withStyle(ChatFormatting.AQUA), true);
        });
    }

    private static List<ResourceLocation> normalizedLoadout(ICareerData data) {
        List<ResourceLocation> loadout = new ArrayList<>(data.getSkillLoadout());
        while (loadout.size() < ACTIVE_SLOT_COUNT) {
            loadout.add(null);
        }
        if (loadout.size() > ACTIVE_SLOT_COUNT) {
            return new ArrayList<>(loadout.subList(0, ACTIVE_SLOT_COUNT));
        }
        return loadout;
    }

    private static void removeDuplicates(List<ResourceLocation> loadout, int keptSlot, ResourceLocation skillId) {
        for (int i = 0; i < loadout.size(); i++) {
            if (i != keptSlot && skillId.equals(loadout.get(i))) {
                loadout.set(i, null);
            }
        }
    }

    private static List<ResourceLocation> trimTrailingEmpty(List<ResourceLocation> loadout) {
        int end = loadout.size();
        while (end > 0 && loadout.get(end - 1) == null) {
            end--;
        }
        return new ArrayList<>(loadout.subList(0, end));
    }
}
