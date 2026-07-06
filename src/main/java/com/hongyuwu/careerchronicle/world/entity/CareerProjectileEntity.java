package com.hongyuwu.careerchronicle.world.entity;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.config.ModConfig;
import com.hongyuwu.careerchronicle.network.NetworkHandler;
import com.hongyuwu.careerchronicle.registry.CareerEntities;
import com.hongyuwu.careerchronicle.registry.CareerItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CareerProjectileEntity extends ThrowableItemProjectile {
    private static final String KEY_SKILL_ID = "skillId";
    private static final String KEY_DAMAGE = "damage";
    private static final String KEY_FIRE_SECONDS = "fireSeconds";
    private static final ResourceLocation DEFAULT_SKILL =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fireball");

    private ResourceLocation skillId = DEFAULT_SKILL;
    private float damage = 6.0F;
    private int fireSeconds = 4;
    private boolean hitConsumed;

    public CareerProjectileEntity(EntityType<? extends CareerProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public CareerProjectileEntity(Level level, LivingEntity owner, ResourceLocation skillId, float damage, int fireSeconds) {
        super(CareerEntities.CAREER_PROJECTILE.get(), owner, level);
        this.skillId = skillId == null ? DEFAULT_SKILL : skillId;
        this.damage = Math.max(0.0F, damage);
        this.fireSeconds = Math.max(0, fireSeconds);
    }

    public ResourceLocation skillId() {
        return skillId;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
            int count = scaledParticleCount(2);
            if (count > 0) {
                serverLevel.sendParticles(trailParticle(),
                        getX(), getY(), getZ(), count, 0.04D, 0.04D, 0.04D, 0.01D);
            }
        }
    }

    private net.minecraft.core.particles.ParticleOptions trailParticle() {
        String path = skillId.getPath();
        if (path.contains("frost") || path.contains("ice") || path.contains("blizzard") || path.contains("glacial") || path.contains("zero")) {
            return ParticleTypes.SNOWFLAKE;
        }
        if (path.contains("soul") || path.contains("bone") || path.contains("death") || path.contains("lich") || path.contains("undead")) {
            return ParticleTypes.SMOKE;
        }
        return ParticleTypes.SMALL_FLAME;
    }

    @Override
    protected Item getDefaultItem() {
        return CareerItems.EMBER_CORE.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (hitConsumed) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target instanceof Player targetPlayer
                && owner instanceof ServerPlayer ownerPlayer
                && !canSkillPvp(ownerPlayer, targetPlayer)) {
            return;
        }
        super.onHitEntity(result);
        if (owner instanceof LivingEntity livingOwner) {
            target.hurt(damageSources().mobProjectile(this, livingOwner), damage);
        } else {
            target.hurt(damageSources().magic(), damage);
        }
        if (fireSeconds > 0) {
            target.setSecondsOnFire(fireSeconds);
        }
    }

    private static boolean canSkillPvp(ServerPlayer owner, Player target) {
        return ModConfig.ENABLE_SKILL_PVP.get() && owner.canHarmPlayer(target);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
    }

    @Override
    protected void onHit(HitResult result) {
        if (hitConsumed) {
            discard();
            return;
        }
        hitConsumed = true;
        super.onHit(result);
        if (level() instanceof ServerLevel && getOwner() instanceof ServerPlayer owner) {
            Vec3 hit = result.getLocation();
            String fxType = result instanceof EntityHitResult ? "entity_hit" : "block_hit";
            NetworkHandler.playSkillFx(owner, skillId, fxType, hit, hit);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(KEY_SKILL_ID, skillId.toString());
        tag.putFloat(KEY_DAMAGE, damage);
        tag.putInt(KEY_FIRE_SECONDS, fireSeconds);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString(KEY_SKILL_ID));
        skillId = parsed == null ? DEFAULT_SKILL : parsed;
        damage = Math.max(0.0F, tag.getFloat(KEY_DAMAGE));
        fireSeconds = Math.max(0, tag.getInt(KEY_FIRE_SECONDS));
    }

    private static int scaledParticleCount(int baseCount) {
        double multiplier = ModConfig.SKILL_PARTICLE_MULTIPLIER.get();
        if (baseCount <= 0 || multiplier <= 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.round(baseCount * multiplier));
    }
}
