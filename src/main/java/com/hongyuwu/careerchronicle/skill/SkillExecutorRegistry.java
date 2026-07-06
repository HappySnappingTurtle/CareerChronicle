package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.config.ModConfig;
import com.hongyuwu.careerchronicle.network.NetworkHandler;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;
import com.hongyuwu.careerchronicle.data.SkillDef;
import com.hongyuwu.careerchronicle.player.ICareerData;

public final class SkillExecutorRegistry {
    public static final ResourceLocation NEXT_ARROW_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "next_arrow_modifier");
    public static final ResourceLocation EAGLE_EYE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "eagle_eye");
    public static final ResourceLocation SHADOW_STRIKE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "shadow_strike");
    public static final ResourceLocation SMOKE_BOMB =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "smoke_bomb");
    public static final ResourceLocation PROVOKE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "provoke");
    public static final ResourceLocation SOUL_DRAIN =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "soul_drain");
    public static final ResourceLocation DEATH_COIL =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "death_coil");
    public static final ResourceLocation LICH_FORM =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "lich_form");
    public static final ResourceLocation FROST_ARROW_FUSION =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "frost_arrow");
    public static final ResourceLocation DEATH_STRIKE_FUSION =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "death_strike");

    private static final Set<ResourceLocation> KNOWN_EXECUTORS = Set.of(
            NEXT_ARROW_MODIFIER,
            EAGLE_EYE,
            SHADOW_STRIKE,
            SMOKE_BOMB,
            PROVOKE,
            SOUL_DRAIN,
            DEATH_COIL,
            LICH_FORM,
            FROST_ARROW_FUSION,
            DEATH_STRIKE_FUSION
    );

    private SkillExecutorRegistry() {
    }

    public static boolean exists(ResourceLocation executorId) {
        return KNOWN_EXECUTORS.contains(executorId);
    }

    public static boolean execute(ServerPlayer player, ICareerData data, SkillDef skill) {
        ResourceLocation executorId = skill.executor();
        if (NEXT_ARROW_MODIFIER.equals(executorId)) {
            shootArrow(player, 0.0F, 0.38D, 3.0D, true, ParticleTypes.FLAME, 3.0F, skill.id());
            return true;
        }
        if (EAGLE_EYE.equals(executorId)) {
            return eagleEye(player);
        }
        if (SHADOW_STRIKE.equals(executorId)) {
            return shadowStrike(player);
        }
        if (SMOKE_BOMB.equals(executorId)) {
            return smokeBomb(player);
        }
        if (PROVOKE.equals(executorId)) {
            return provoke(player);
        }
        if (SOUL_DRAIN.equals(executorId)) {
            return soulDrain(player);
        }
        if (DEATH_COIL.equals(executorId)) {
            return deathCoil(player);
        }
        if (LICH_FORM.equals(executorId)) {
            return lichForm(player);
        }
        if (FROST_ARROW_FUSION.equals(executorId)) {
            return nextArrowModifierFrost(player, skill);
        }
        if (DEATH_STRIKE_FUSION.equals(executorId)) {
            return deathStrikeFusion(player);
        }

        player.sendSystemMessage(Component.translatable("careerchronicle.message.executor_missing", executorId.toString())
                .withStyle(ChatFormatting.RED));
        return false;
    }

    private static boolean eagleEye(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, true, true));
        player.getPersistentData().putInt("careerchronicleChargedShotTicks", 240);
        Vec3 origin = SkillCastHelper.castOrigin(player, 0.42D, 0.5D, -0.62D);
        NetworkHandler.playSkillFx(player, EAGLE_EYE, "cast", origin, origin.add(player.getLookAngle().scale(3.0D)));
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_QUICK_CHARGE_3, player.getSoundSource(),
                0.45F, 1.55F);
        return true;
    }

    private static boolean shadowStrike(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        Vec3 look = player.getLookAngle();
        player.push(look.x * 0.8, 0.1, look.z * 0.8);
        player.hurtMarked = true;
        LivingEntity nearest = findNearestHostile(serverLevel, player, 4.0);
        if (nearest != null) {
            nearest.hurt(player.damageSources().playerAttack(player), 8.0F);
            NetworkHandler.playSkillFx(player, SHADOW_STRIKE, "entity_hit",
                    nearest.position().add(0, nearest.getBbHeight() / 2, 0),
                    nearest.position().add(0, nearest.getBbHeight() / 2, 0));
        }
        Vec3 origin = player.position().add(0, 0.12, 0);
        NetworkHandler.playSkillFx(player, SHADOW_STRIKE, "cast", origin, origin.add(look.scale(2)));
        return true;
    }

    private static boolean smokeBomb(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, true, true));
        FxBudget fxBudget = FxBudget.targetHits();
        for (LivingEntity target : serverLevel.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(4.0), t -> SkillCastHelper.canAffectHostileTarget(player, t))) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            playTargetHitFx(fxBudget, player, SMOKE_BOMB, target);
        }
        Vec3 origin = player.position().add(0, 0.12, 0);
        NetworkHandler.playSkillFx(player, SMOKE_BOMB, "cast", origin, origin);
        return true;
    }

    private static boolean provoke(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        FxBudget fxBudget = FxBudget.targetHits();
        for (LivingEntity target : serverLevel.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(6.0), t -> SkillCastHelper.canAffectHostileTarget(player, t))) {
            if (target instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(player);
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            playTargetHitFx(fxBudget, player, PROVOKE, target);
        }
        Vec3 origin = player.position().add(0, 0.12, 0);
        NetworkHandler.playSkillFx(player, PROVOKE, "cast", origin, origin);
        return true;
    }

    private static boolean soulDrain(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        LivingEntity nearest = findNearestHostile(serverLevel, player, 8.0);
        if (nearest != null) {
            nearest.hurt(player.damageSources().magic(), 5.0F);
            player.heal(4.0F);
            NetworkHandler.playSkillFx(player, SOUL_DRAIN, "entity_hit",
                    nearest.position().add(0, nearest.getBbHeight() / 2, 0),
                    player.position().add(0, player.getEyeHeight() / 2, 0));
        }
        Vec3 origin = player.position().add(0, 0.12, 0);
        NetworkHandler.playSkillFx(player, SOUL_DRAIN, "cast", origin, origin);
        return true;
    }

    private static boolean deathCoil(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        LivingEntity nearest = findNearestHostile(serverLevel, player, 10.0);
        if (nearest != null) {
            nearest.hurt(player.damageSources().magic(), 7.0F);
            nearest.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            player.heal(5.0F);
            NetworkHandler.playSkillFx(player, DEATH_COIL, "entity_hit",
                    nearest.position().add(0, nearest.getBbHeight() / 2, 0),
                    player.position().add(0, player.getEyeHeight() / 2, 0));
        }
        Vec3 origin = player.position().add(0, 0.12, 0);
        NetworkHandler.playSkillFx(player, DEATH_COIL, "cast", origin, origin);
        return true;
    }

    private static boolean lichForm(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0, false, true, true));
        Vec3 origin = player.position().add(0, 0.12, 0);
        NetworkHandler.playSkillFx(player, LICH_FORM, "cast", origin, origin);
        return true;
    }

    private static boolean nextArrowModifierFrost(ServerPlayer player, SkillDef skill) {
        CareerDataAccess.get(player).ifPresent(data -> data.getRuntimeState().setNextProjectileModifier(skill.id()));
        CareerDataAccess.sync(player);
        Vec3 o = player.position().add(0, player.getEyeHeight() - 0.15, 0);
        NetworkHandler.playSkillFx(player, FROST_ARROW_FUSION, "cast", o, o.add(player.getLookAngle().scale(2)));
        return true;
    }

    private static boolean deathStrikeFusion(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        LivingEntity nearest = findNearestHostile(serverLevel, player, 4.0);
        if (nearest != null) {
            nearest.hurt(player.damageSources().playerAttack(player), 10.0F);
            nearest.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
            player.heal(3.0F);
            NetworkHandler.playSkillFx(player, DEATH_STRIKE_FUSION, "entity_hit",
                    nearest.position().add(0, nearest.getBbHeight() / 2, 0),
                    nearest.position().add(0, nearest.getBbHeight() / 2, 0));
        }
        Vec3 o = player.position().add(0, 0.12, 0);
        NetworkHandler.playSkillFx(player, DEATH_STRIKE_FUSION, "cast", o, o);
        return true;
    }

    private static LivingEntity findNearestHostile(ServerLevel level, ServerPlayer player, double range) {
        LivingEntity nearest = null;
        double nearestDist = range;
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(range), t -> SkillCastHelper.canAffectHostileTarget(player, t))) {
            double dist = player.distanceTo(target);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = target;
            }
        }
        return nearest;
    }

    private static void playTargetHitFx(FxBudget fxBudget, ServerPlayer player, ResourceLocation skillId, LivingEntity target) {
        if (!fxBudget.tryConsume()) {
            return;
        }
        Vec3 center = target.position().add(0.0D, Math.min(1.0D, target.getBbHeight() * 0.55D), 0.0D);
        NetworkHandler.playSkillFx(player, skillId, "entity_hit", center, center);
    }

    private static final class FxBudget {
        private int remaining;

        private FxBudget(int remaining) {
            this.remaining = Math.max(0, remaining);
        }

        private static FxBudget targetHits() {
            return new FxBudget(ModConfig.MAX_TARGET_HIT_FX_PER_SKILL_CAST.get());
        }

        private boolean tryConsume() {
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }
    }

    private static void shootArrow(ServerPlayer player, float yawOffset, double sideOffset, double damageBonus,
                                   boolean fire, SimpleParticleType particle, float velocity, ResourceLocation skillId) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Arrow arrow = new Arrow(player.level(), player);
        double sideMagnitude = Math.min(0.7D, Math.abs(sideOffset));
        double forwardOffset = 0.58D + sideMagnitude * 0.16D;
        double verticalOffset = -0.56D + sideMagnitude * 0.1D;
        Vec3 origin = SkillCastHelper.castOrigin(player, sideOffset, forwardOffset, verticalOffset);
        Vec3 direction = SkillCastHelper.aimDirection(player, origin, 42.0D, yawOffset);
        arrow.setPos(origin.x, origin.y, origin.z);
        arrow.setBaseDamage(arrow.getBaseDamage() + damageBonus);
        if (fire) {
            arrow.setSecondsOnFire(8);
        }
        CareerArrowTags.mark(arrow, skillId, fire, false);
        arrow.shoot(direction.x, direction.y, direction.z, velocity, 0.16F);
        arrow.setCritArrow(true);
        player.level().addFreshEntity(arrow);
        Vec3 fxTarget = origin.add(arrow.getDeltaMovement().normalize().scale(2.7D));
        NetworkHandler.playSkillFx(player, skillId, "cast", origin, fxTarget);
        SkillCastHelper.sendSkillParticles(serverLevel, ParticleTypes.END_ROD,
                origin.x, origin.y, origin.z, 2, 0.035D, 0.035D, 0.035D, 0.004D);
        Vec3 trail = arrow.getDeltaMovement().normalize();
        int trailPoints = SkillCastHelper.scaledParticleCount(5);
        for (int i = 0; i < trailPoints; i++) {
            Vec3 point = origin.add(trail.scale(i * 0.34D));
            serverLevel.sendParticles(particle, point.x, point.y, point.z, 1, 0.025D, 0.025D, 0.025D, 0.006D);
        }
    }
}
