package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.data.SkillDef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SkillEquipmentRequirements {
    private SkillEquipmentRequirements() {
    }

    public static boolean hasRequiredEquipment(Player player, SkillDef skill) {
        if (skill == null || !skill.requirements().hasEquipmentTags()) {
            return true;
        }
        if (player == null) {
            return false;
        }
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        for (ResourceLocation tagId : skill.requirements().equipmentTags()) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
            if (mainHand.is(tag) || offHand.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public static Component requirementText(SkillDef skill) {
        if (skill == null || !skill.requirements().hasEquipmentTags()) {
            return Component.literal("-");
        }
        List<ResourceLocation> tags = new ArrayList<>(skill.requirements().equipmentTags());
        tags.sort(Comparator.comparing(ResourceLocation::toString));

        MutableComponent text = Component.empty();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                text.append(Component.literal(", "));
            }
            text.append(tagName(tags.get(i)));
        }
        return text;
    }

    public static Component tagName(ResourceLocation tagId) {
        return Component.translatable(tagTranslationKey(tagId));
    }

    public static String tagTranslationKey(ResourceLocation tagId) {
        return "careerchronicle.equipment_tag." + tagId.getNamespace() + "." + tagId.getPath();
    }
}
