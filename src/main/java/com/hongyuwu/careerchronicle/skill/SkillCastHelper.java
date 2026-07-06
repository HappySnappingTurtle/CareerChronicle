package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.config.ModConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SkillCastHelper {
    private SkillCastHelper() {
    }

    public static Vec3 castOrigin(ServerPlayer player, double sideOffset, double forwardOffset, double verticalOffset) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();
        return player.position()
                .add(0.0D, player.getEyeHeight() + verticalOffset, 0.0D)
                .add(look.scale(forwardOffset))
                .add(side.scale(sideOffset));
    }

    public static Vec3 aimDirection(ServerPlayer player, Vec3 origin, double range, float yawOffset) {
        Vec3 eye = player.getEyePosition();
        Vec3 aimPoint = eye.add(player.getLookAngle().normalize().scale(range));
        Vec3 direction = aimPoint.subtract(origin);
        if (direction.lengthSqr() < 0.001D) {
            direction = player.getLookAngle();
        }
        direction = direction.normalize();
        if (Math.abs(yawOffset) > 0.001F) {
            direction = rotateHorizontal(direction, Math.toRadians(yawOffset)).normalize();
        }
        return direction;
    }

    public static Vec3 rotateHorizontal(Vec3 direction, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(
                direction.x * cos - direction.z * sin,
                direction.y,
                direction.x * sin + direction.z * cos
        );
    }

    public static void spawnRing(ServerPlayer player, SimpleParticleType particle, int count, double radius) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int points = scaledParticleCount(count);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            serverLevel.sendParticles(
                    particle,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.12D,
                    player.getZ() + Math.sin(angle) * radius,
                    1, 0.0D, 0.018D, 0.0D, 0.012D
            );
        }
    }

    public static void sendSkillParticles(ServerLevel level, ParticleOptions particle,
                                          double x, double y, double z,
                                          int count, double offsetX, double offsetY, double offsetZ, double speed) {
        int scaled = scaledParticleCount(count);
        if (scaled <= 0) {
            return;
        }
        level.sendParticles(particle, x, y, z, scaled, offsetX, offsetY, offsetZ, speed);
    }

    public static int scaledParticleCount(int baseCount) {
        double multiplier = ModConfig.SKILL_PARTICLE_MULTIPLIER.get();
        if (baseCount <= 0 || multiplier <= 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.round(baseCount * multiplier));
    }

    public static boolean canAffectHostileTarget(Player player, LivingEntity target) {
        if (target == player || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player targetPlayer) {
            return ModConfig.ENABLE_SKILL_PVP.get() && player.canHarmPlayer(targetPlayer);
        }
        return true;
    }

    public static boolean healIfMissing(LivingEntity entity, float amount) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(amount);
            return true;
        }
        return false;
    }
}
