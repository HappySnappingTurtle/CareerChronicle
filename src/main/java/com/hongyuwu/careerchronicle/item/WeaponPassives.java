package com.hongyuwu.careerchronicle.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class WeaponPassives {
    private WeaponPassives() {}

    // --- Staff passives ---
    public static final LegendaryWeaponItem.PassiveEffect INFERNAL_DOMINION = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
        }
        @Override public void onHit(ServerPlayer player, LivingEntity target) {
            target.setSecondsOnFire(6);
            target.hurt(player.damageSources().magic(), 4.0F);
        }
    };

    public static final LegendaryWeaponItem.PassiveEffect ABSOLUTE_FROST = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {}
        @Override public void onHit(ServerPlayer player, LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, true, true));
        }
    };

    public static final LegendaryWeaponItem.PassiveEffect SOUL_HARVEST = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {}
        @Override public void onHit(ServerPlayer player, LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
            player.heal(3.0F);
        }
    };

    // --- Bow passives ---
    public static final LegendaryWeaponItem.PassiveEffect WIND_WALKER = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, true, false, true));
        }
    };

    public static final LegendaryWeaponItem.PassiveEffect PREDATOR_MARK = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, true));
        }
        @Override public void onHit(ServerPlayer player, LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        }
    };

    // --- Sword passives ---
    public static final LegendaryWeaponItem.PassiveEffect BERSERKER_RAGE = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {
            if (player.getHealth() < player.getMaxHealth() * 0.3F) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 2, true, false, true));
            }
        }
        @Override public void onHit(ServerPlayer player, LivingEntity target) {
            target.knockback(0.6, player.getX() - target.getX(), player.getZ() - target.getZ());
        }
    };

    public static final LegendaryWeaponItem.PassiveEffect DEATH_SENTENCE = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {}
        @Override public void onHit(ServerPlayer player, LivingEntity target) {
            if (target.getHealth() < target.getMaxHealth() * 0.2F) {
                target.hurt(player.damageSources().playerAttack(player), 20.0F);
            }
        }
    };

    // --- Sigil passives ---
    public static final LegendaryWeaponItem.PassiveEffect DIVINE_GRACE = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
        }
    };

    public static final LegendaryWeaponItem.PassiveEffect MARTYR_BLESSING = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
        }
    };

    // --- Dagger passives ---
    public static final LegendaryWeaponItem.PassiveEffect SHADOW_STEP = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, true, false, true));
        }
        @Override public void onHit(ServerPlayer player, LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
        }
    };

    // --- Mace passives ---
    public static final LegendaryWeaponItem.PassiveEffect EARTHQUAKE = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, true, false, true));
        }
        @Override public void onHit(ServerPlayer player, LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            target.knockback(0.8, player.getX() - target.getX(), player.getZ() - target.getZ());
        }
    };

    // --- Scepter passives ---
    public static final LegendaryWeaponItem.PassiveEffect LIFE_DRAIN = new LegendaryWeaponItem.PassiveEffect() {
        @Override public void apply(ServerPlayer player) {}
        @Override public void onHit(ServerPlayer player, LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));
            player.heal(5.0F);
        }
    };
}
