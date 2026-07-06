package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.SkillDef;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import com.hongyuwu.careerchronicle.player.ICareerData;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectPipeline;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class CareerSkillService {
    private CareerSkillService() {
    }

    public static boolean useSkill(ServerPlayer player, ResourceLocation skillId) {
        if (!player.isAlive() || player.isSpectator() || player.isDeadOrDying()) {
            return false;
        }

        SkillDef skill = CareerRegistry.snapshot().skills().get(skillId);
        if (skill == null) {
            actionBar(player, Component.translatable("careerchronicle.message.unknown_skill", skillId.toString())
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        AtomicBoolean used = new AtomicBoolean(false);
        CareerDataAccess.get(player).ifPresent(data -> used.set(useKnownSkill(player, data, skill)));
        return used.get();
    }

    private static boolean useKnownSkill(ServerPlayer player, ICareerData data, SkillDef skill) {
        if (!data.getUnlockedSkills().contains(skill.id())) {
            actionBar(player, Component.translatable("careerchronicle.message.skill_locked",
                    Component.translatable(skill.displayKey())).withStyle(ChatFormatting.RED));
            return false;
        }

        if (!isInLoadout(data, skill.id())) {
            actionBar(player, Component.translatable("careerchronicle.message.skill_not_equipped",
                    Component.translatable(skill.displayKey())).withStyle(ChatFormatting.RED));
            return false;
        }

        int cooldown = data.getRuntimeState().getCooldownTicks().getOrDefault(skill.id(), 0);
        if (cooldown > 0) {
            actionBar(player, Component.translatable("careerchronicle.message.skill_cooling", cooldown / 20 + 1)
                    .withStyle(ChatFormatting.YELLOW));
            playFailSound(player);
            return false;
        }

        if (!isCastableSkill(skill)) {
            actionBar(player, Component.translatable("careerchronicle.message.skill_not_castable",
                    Component.translatable(skill.displayKey())).withStyle(ChatFormatting.RED));
            return false;
        }

        if (!SkillEquipmentRequirements.hasRequiredEquipment(player, skill)) {
            actionBar(player, Component.translatable("careerchronicle.message.skill_equipment_missing",
                    SkillEquipmentRequirements.requirementText(skill)).withStyle(ChatFormatting.YELLOW));
            return false;
        }

        if (!data.getRuntimeState().hasResource(skill.resource(), skill.resourceCost())) {
            actionBar(player, Component.translatable("careerchronicle.message.skill_resource_missing",
                    resourceName(skill.resource()), skill.resourceCost()).withStyle(ChatFormatting.YELLOW));
            playFailSound(player);
            return false;
        }

        boolean executed;
        if (skill.hasComponentEffects()) {
            int skillLevel = SkillLevelService.levelOf(data, skill);
            EffectContext ctx = EffectContext.castContext(player, skillLevel, skill.id());
            executed = EffectPipeline.execute(ctx, skill.effects());
        } else {
            executed = SkillExecutorRegistry.execute(player, data, skill);
        }

        if (executed) {
            data.getRuntimeState().consumeResource(skill.resource(), skill.resourceCost());
            data.getRuntimeState().setCooldown(skill.id(), skill.cooldownTicks());
            CareerDataAccess.sync(player);
            return true;
        }
        return false;
    }

    private static boolean isCastableSkill(SkillDef skill) {
        return "active".equals(skill.type()) || "fusion".equals(skill.type())
                || "hidden".equals(skill.type()) || "ultimate".equals(skill.type())
                || "race".equals(skill.type());
    }

    private static Component resourceName(String resource) {
        if (resource == null || resource.isBlank() || "none".equals(resource)) {
            return Component.translatable("careerchronicle.resource.none");
        }
        return Component.translatable("careerchronicle.resource." + resource);
    }

    private static boolean isInLoadout(ICareerData data, ResourceLocation skillId) {
        if (data.getSkillLoadout().contains(skillId)) {
            return true;
        }
        if (skillId.equals(data.getUltimateSlot())) {
            return true;
        }
        return skillId.equals(data.getRaceSlot());
    }

    private static void actionBar(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }

    private static void playFailSound(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.25F, 0.6F);
    }
}
