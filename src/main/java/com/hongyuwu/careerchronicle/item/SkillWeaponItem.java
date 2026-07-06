package com.hongyuwu.careerchronicle.item;

import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.SkillDef;
import com.hongyuwu.careerchronicle.skill.CareerSkillService;
import com.hongyuwu.careerchronicle.skill.SkillEquipmentRequirements;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class SkillWeaponItem extends Item {
    private final ResourceLocation skillId;

    public SkillWeaponItem(Properties properties, ResourceLocation skillId) {
        super(properties);
        this.skillId = skillId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            boolean used = CareerSkillService.useSkill(serverPlayer, skillId);
            if (used) {
                SkillDef skill = CareerRegistry.snapshot().skills().get(skillId);
                int itemCooldown = skill == null ? 20 : Math.max(8, skill.cooldownTicks() / 2);
                player.getCooldowns().addCooldown(this, itemCooldown);
                if (stack.isDamageableItem()) {
                    stack.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(
                            hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        SkillDef skill = CareerRegistry.snapshot().skills().get(skillId);
        Component skillName = skill == null
                ? Component.translatable("careerchronicle.skill." + skillId.getPath())
                : Component.translatable(skill.displayKey());
        tooltip.add(Component.translatable("careerchronicle.item.skill_weapon.bound_skill", skillName)
                .withStyle(ChatFormatting.GOLD));
        if (skill != null) {
            tooltip.add(Component.translatable("careerchronicle.item.skill_weapon.cast_info",
                    formatSeconds(skill.cooldownTicks()), resourceText(skill)).withStyle(ChatFormatting.GRAY));
            if (skill.requirements().hasEquipmentTags()) {
                tooltip.add(Component.translatable("careerchronicle.item.skill_weapon.equipment",
                        SkillEquipmentRequirements.requirementText(skill)).withStyle(ChatFormatting.GRAY));
            }
        }
        tooltip.add(Component.translatable("careerchronicle.item.skill_weapon.use_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    private Component resourceText(SkillDef skill) {
        if (skill.resourceCost() <= 0 || "none".equals(skill.resource())) {
            return Component.translatable("careerchronicle.resource.none");
        }
        return Component.translatable("careerchronicle.ui.skill_resource_cost",
                skill.resourceCost(), Component.translatable("careerchronicle.resource." + skill.resource()));
    }

    private String formatSeconds(int cooldownTicks) {
        return String.format(Locale.ROOT, "%.1f", cooldownTicks / 20.0F);
    }
}
