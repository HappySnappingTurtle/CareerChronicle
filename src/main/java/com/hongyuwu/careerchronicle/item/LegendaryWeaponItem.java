package com.hongyuwu.careerchronicle.item;

import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.SkillDef;
import com.hongyuwu.careerchronicle.skill.CareerSkillService;
import com.hongyuwu.careerchronicle.skill.SkillEquipmentRequirements;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class LegendaryWeaponItem extends Item {
    private final ResourceLocation skillId;
    private final WeaponTier tier;
    private final PassiveEffect passive;
    private final String passiveKey;

    public LegendaryWeaponItem(Properties properties, ResourceLocation skillId, WeaponTier tier,
                                PassiveEffect passive, String passiveKey) {
        super(properties);
        this.skillId = skillId;
        this.tier = tier;
        this.passive = passive;
        this.passiveKey = passiveKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (skillId != null && player instanceof ServerPlayer serverPlayer) {
            if (CareerSkillService.useSkill(serverPlayer, skillId)) {
                player.getItemInHand(hand).hurtAndBreak(1, player, p -> p.broadcastBreakEvent(
                        hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!isSelected || !(entity instanceof ServerPlayer player) || level.isClientSide()) return;
        if (player.tickCount % 80 != 0) return;
        if (passive != null) {
            passive.apply(player);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (passive != null && attacker instanceof ServerPlayer player) {
            passive.onHit(player, target);
        }
        stack.hurtAndBreak(1, attacker, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withStyle(tier.color());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("careerchronicle.weapon_tier." + tier.id())
                .withStyle(tier.color()));
        if (passiveKey != null) {
            tooltip.add(Component.translatable(passiveKey).withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (skillId != null) {
            SkillDef skill = CareerRegistry.snapshot().skills().get(skillId);
            String skillName = skill != null ? Component.translatable(skill.displayKey()).getString()
                    : skillId.getPath();
            tooltip.add(Component.translatable("careerchronicle.item.skill_weapon.bound_skill", skillName)
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier == WeaponTier.LEGENDARY;
    }

    @FunctionalInterface
    public interface PassiveEffect {
        void apply(ServerPlayer player);

        default void onHit(ServerPlayer player, LivingEntity target) {}
    }
}
