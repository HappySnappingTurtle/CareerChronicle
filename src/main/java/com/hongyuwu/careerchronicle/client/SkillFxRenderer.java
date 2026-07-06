package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SkillFxRenderer {
    private static final ResourceLocation FIREBALL =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fireball");
    private static final ResourceLocation CHARGED_SHOT =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "charged_shot");
    private static final ResourceLocation SCATTER_SHOT =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "scatter_shot");
    private static final ResourceLocation SNARE_SHOT =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "snare_shot");
    private static final ResourceLocation FLAME_ARROW =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "flame_arrow");
    private static final ResourceLocation LUNGE_STRIKE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "lunge_strike");
    private static final ResourceLocation GUARD_STANCE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "guard_stance");
    private static final ResourceLocation GROUND_SLAM =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ground_slam");
    private static final ResourceLocation MEND =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "mend");
    private static final ResourceLocation HOLY_NOVA =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "holy_nova");
    private static final ResourceLocation BLESSING =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "blessing");
    private static final ResourceLocation BLAZING_CHARGE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "blazing_charge");
    private static final ResourceLocation SUNFIRE_AEGIS =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "sunfire_aegis");
    private static final ResourceLocation PIERCING_VOLLEY =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "piercing_volley");
    private static final ResourceLocation GUIDING_LIGHT_ARROW =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "guiding_light_arrow");
    private static final ResourceLocation CONSECRATED_SLAM =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "consecrated_slam");
    private static final ResourceLocation INFERNO_FOCUS =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "inferno_focus");
    private static final ResourceLocation EAGLE_EYE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "eagle_eye");
    private static final ResourceLocation IRON_VANGUARD =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "iron_vanguard");
    private static final ResourceLocation SERAPHIC_GRACE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "seraphic_grace");
    private static final ResourceLocation METEOR_RITE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "meteor_rite");
    private static final ResourceLocation STORM_MARKSMAN =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "storm_marksman");
    private static final ResourceLocation UNYIELDING_COLOSSUS =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "unyielding_colossus");
    private static final ResourceLocation SANCTUARY_DESCENT =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "sanctuary_descent");
    private static final ResourceLocation ASHEN_BULWARK =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ashen_bulwark");
    private static final ResourceLocation EMBER_BURST =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ember_burst");
    private static final ResourceLocation FLAME_STEP =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "flame_step");
    private static final Vec3 GROUND_FX_OFFSET = new Vec3(0.0D, -0.08D, 0.0D);

    private SkillFxRenderer() {
    }

    public static void play(ResourceLocation skillId, String fxType, Vec3 origin, Vec3 target, double particleMultiplier) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        double particles = Math.max(0.0D, Math.min(2.0D, particleMultiplier));

        if ("hit".equals(fxType)) {
            playHit(level, skillId, origin, particles);
            CameraShakeManager.trigger(1.5F, 6);
            return;
        }
        if ("entity_hit".equals(fxType)) {
            playEntityHit(level, skillId, origin, false, particles);
            CameraShakeManager.trigger(0.8F, 4);
            return;
        }
        if ("ally_hit".equals(fxType)) {
            playEntityHit(level, skillId, origin, true, particles);
            return;
        }
        if ("block_hit".equals(fxType)) {
            playBlockHit(level, skillId, origin, particles);
            return;
        }

        if (FIREBALL.equals(skillId)) {
            playFireball(level, origin, target, particles);
            return;
        }
        if (FLAME_ARROW.equals(skillId)) {
            playArrowTrail(level, origin, target, ParticleTypes.FLAME, particles);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                    0.55F, 1.25F, false);
            return;
        }
        if (CHARGED_SHOT.equals(skillId)) {
            playArrowTrail(level, origin, target, ParticleTypes.ELECTRIC_SPARK, particles);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.CROSSBOW_QUICK_CHARGE_3, SoundSource.PLAYERS,
                    0.45F, 1.35F, false);
            return;
        }
        if (SCATTER_SHOT.equals(skillId)) {
            playArrowTrail(level, origin, target, ParticleTypes.CRIT, particles);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS,
                    0.38F, 1.55F, false);
            return;
        }
        if (SNARE_SHOT.equals(skillId)) {
            playArrowTrail(level, origin, target, ParticleTypes.HAPPY_VILLAGER, particles);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS,
                    0.42F, 0.9F, false);
            return;
        }
        if (LUNGE_STRIKE.equals(skillId)) {
            playLunge(level, origin, target, particles);
            return;
        }
        if (GUARD_STANCE.equals(skillId)) {
            playGroundRing(level, lowOrigin(origin), ParticleTypes.CRIT, 1.05D, scaledCount(14, particles), 0.014D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS,
                    0.42F, 0.8F, false);
            return;
        }
        if (GROUND_SLAM.equals(skillId)) {
            playGroundRing(level, lowOrigin(origin), ParticleTypes.POOF, 2.2D, scaledCount(18, particles), 0.03D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                    0.32F, 1.35F, false);
            return;
        }
        if (MEND.equals(skillId)) {
            playMend(level, origin, particles);
            return;
        }
        if (HOLY_NOVA.equals(skillId)) {
            playGroundRing(level, lowOrigin(origin), ParticleTypes.END_ROD, 2.4D, scaledCount(16, particles), 0.022D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                    0.42F, 1.25F, false);
            return;
        }
        if (BLESSING.equals(skillId)) {
            playBlessing(level, origin, particles);
            return;
        }
        if (BLAZING_CHARGE.equals(skillId)) {
            playBlazingCharge(level, origin, target, particles);
            return;
        }
        if (SUNFIRE_AEGIS.equals(skillId)) {
            playSunfireAegis(level, origin, particles);
            return;
        }
        if (PIERCING_VOLLEY.equals(skillId)) {
            playArrowTrail(level, origin, target, ParticleTypes.ELECTRIC_SPARK, particles);
            trace(level, origin.add(0.0D, 0.06D, 0.0D), target, ParticleTypes.CRIT, scaledCount(5, particles), 0.012D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS,
                    0.44F, 1.1F, false);
            return;
        }
        if (GUIDING_LIGHT_ARROW.equals(skillId)) {
            playArrowTrail(level, origin, target, ParticleTypes.END_ROD, particles);
            trace(level, origin.add(0.0D, 0.08D, 0.0D), target, ParticleTypes.ENCHANT, scaledCount(4, particles), 0.01D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    0.36F, 1.65F, false);
            return;
        }
        if (CONSECRATED_SLAM.equals(skillId)) {
            playGroundRing(level, lowOrigin(origin), ParticleTypes.END_ROD, 2.65D, scaledCount(16, particles), 0.022D);
            playGroundRing(level, lowOrigin(origin), ParticleTypes.POOF, 1.55D, scaledCount(10, particles), 0.03D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
                    0.42F, 1.35F, false);
            return;
        }
        if (INFERNO_FOCUS.equals(skillId)) {
            playGroundRing(level, lowOrigin(origin), ParticleTypes.FLAME, 1.45D, scaledCount(16, particles), 0.02D);
            playGroundRing(level, lowOrigin(origin), ParticleTypes.SMALL_FLAME, 0.75D, scaledCount(8, particles), 0.014D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                    0.5F, 0.75F, false);
            return;
        }
        if (EAGLE_EYE.equals(skillId)) {
            trace(level, origin, target, ParticleTypes.ELECTRIC_SPARK, scaledCount(5, particles), 0.012D);
            playGroundRing(level, lowOrigin(origin), ParticleTypes.CRIT, 0.7D, scaledCount(8, particles), 0.012D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.CROSSBOW_QUICK_CHARGE_3, SoundSource.PLAYERS,
                    0.38F, 1.6F, false);
            return;
        }
        if (IRON_VANGUARD.equals(skillId)) {
            playGroundRing(level, lowOrigin(origin), ParticleTypes.CRIT, 1.35D, scaledCount(16, particles), 0.014D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS,
                    0.46F, 0.72F, false);
            return;
        }
        if (SERAPHIC_GRACE.equals(skillId)) {
            playGroundRing(level, lowOrigin(origin), ParticleTypes.HEART, 1.65D, scaledCount(10, particles), 0.014D);
            playGroundRing(level, lowOrigin(origin), ParticleTypes.END_ROD, 2.15D, scaledCount(16, particles), 0.018D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                    0.42F, 1.55F, false);
            return;
        }
        if (METEOR_RITE.equals(skillId)) {
            Vec3 ground = lowOrigin(origin);
            playGroundRing(level, ground, ParticleTypes.FLAME, 2.8D, scaledCount(16, particles), 0.024D);
            playGroundRing(level, ground, ParticleTypes.LAVA, 1.2D, scaledCount(4, particles), 0.014D);
            int flareCount = scaledCount(6, particles);
            for (int i = 0; i < flareCount; i++) {
                double angle = Math.PI * 2.0D * i / flareCount;
                double radius = 0.35D + (i % 4) * 0.28D;
                level.addParticle(ParticleTypes.FLAME,
                        ground.x + Math.cos(angle) * radius,
                        ground.y + 0.08D,
                        ground.z + Math.sin(angle) * radius,
                        0.0D,
                        0.04D,
                        0.0D);
            }
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                    0.36F, 1.15F, false);
            return;
        }
        if (STORM_MARKSMAN.equals(skillId)) {
            playArrowTrail(level, origin, target, ParticleTypes.ELECTRIC_SPARK, particles);
            trace(level, origin.add(0.0D, 0.08D, 0.0D), target, ParticleTypes.CRIT, scaledCount(5, particles), 0.012D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.CROSSBOW_QUICK_CHARGE_3, SoundSource.PLAYERS,
                    0.4F, 1.7F, false);
            return;
        }
        if (UNYIELDING_COLOSSUS.equals(skillId)) {
            playGroundRing(level, lowOrigin(origin), ParticleTypes.POOF, 2.0D, scaledCount(16, particles), 0.035D);
            playGroundRing(level, lowOrigin(origin), ParticleTypes.CRIT, 1.2D, scaledCount(10, particles), 0.014D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                    0.34F, 0.72F, false);
            return;
        }
        if (SANCTUARY_DESCENT.equals(skillId)) {
            Vec3 ground = lowOrigin(origin);
            playGroundRing(level, ground, ParticleTypes.END_ROD, 2.8D, scaledCount(14, particles), 0.022D);
            playGroundRing(level, ground, ParticleTypes.HEART, 1.35D, scaledCount(6, particles), 0.014D);
            int moteCount = scaledCount(6, particles);
            for (int i = 0; i < moteCount; i++) {
                double angle = Math.PI * 2.0D * i / moteCount;
                level.addParticle(ParticleTypes.ENCHANT,
                        ground.x + Math.cos(angle) * 0.75D,
                        ground.y + 0.18D + (i % 3) * 0.06D,
                        ground.z + Math.sin(angle) * 0.75D,
                        -Math.cos(angle) * 0.025D,
                        0.03D,
                        -Math.sin(angle) * 0.025D);
            }
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
                    0.42F, 1.35F, false);
            return;
        }
        if (ASHEN_BULWARK.equals(skillId)) {
            playGroundRing(level, lowOrigin(origin), ParticleTypes.FLAME, 1.75D, scaledCount(18, particles), 0.022D);
            playGroundRing(level, lowOrigin(origin), ParticleTypes.CRIT, 1.05D, scaledCount(12, particles), 0.014D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS,
                    0.46F, 0.78F, false);
            return;
        }
        if (EMBER_BURST.equals(skillId)) {
            playEmberBurst(level, origin, particles);
            return;
        }
        if (FLAME_STEP.equals(skillId)) {
            playFlameStep(level, origin, target, particles);
            return;
        }

        ElementCategory cat = elementCategory(skillId);
        playCategoryCast(level, origin, target, cat, particles);
    }

    private enum ElementCategory { FIRE, ICE, HOLY, DARK, PHYSICAL, STEALTH, GENERIC }

    private static ElementCategory elementCategory(ResourceLocation skillId) {
        if (skillId == null) return ElementCategory.GENERIC;
        String path = skillId.getPath();
        if (path.contains("fire") || path.contains("flame") || path.contains("ember")
                || path.contains("inferno") || path.contains("meteor") || path.contains("blaz")
                || path.contains("hellfire") || path.contains("thermal") || path.contains("sunfire")) {
            return ElementCategory.FIRE;
        }
        if (path.contains("frost") || path.contains("ice") || path.contains("blizzard")
                || path.contains("glacial") || path.contains("frozen") || path.contains("zero")
                || path.contains("permafrost") || path.contains("crystal")) {
            return ElementCategory.ICE;
        }
        if (path.contains("holy") || path.contains("sacred") || path.contains("blessing")
                || path.contains("mend") || path.contains("sanctuary") || path.contains("seraphic")
                || path.contains("consecrat") || path.contains("guiding") || path.contains("twilight")
                || path.contains("silent_prayer")) {
            return ElementCategory.HOLY;
        }
        if (path.contains("soul") || path.contains("death") || path.contains("bone")
                || path.contains("undead") || path.contains("lich") || path.contains("necro")
                || path.contains("spectral") || path.contains("reaper") || path.contains("coil")) {
            return ElementCategory.DARK;
        }
        if (path.contains("shadow") || path.contains("smoke") || path.contains("phantom")
                || path.contains("assassin") || path.contains("stealth")) {
            return ElementCategory.STEALTH;
        }
        if (path.contains("shield") || path.contains("guard") || path.contains("slam")
                || path.contains("strike") || path.contains("lunge") || path.contains("blade")
                || path.contains("volley") || path.contains("provoke") || path.contains("fortress")
                || path.contains("sentinel") || path.contains("surprise") || path.contains("bulwark")
                || path.contains("breaker") || path.contains("vanguard") || path.contains("colossus")) {
            return ElementCategory.PHYSICAL;
        }
        return ElementCategory.GENERIC;
    }

    private static void playCategoryCast(Level level, Vec3 origin, Vec3 target, ElementCategory cat, double particles) {
        ParticleOptions particle;
        SoundEvent sound;
        float volume = 0.35F;
        float pitch = 1.2F;
        switch (cat) {
            case FIRE -> {
                particle = ParticleTypes.FLAME;
                sound = SoundEvents.FIRECHARGE_USE;
                pitch = 1.35F;
            }
            case ICE -> {
                particle = ParticleTypes.SNOWFLAKE;
                sound = SoundEvents.GLASS_BREAK;
                pitch = 1.6F;
            }
            case HOLY -> {
                particle = ParticleTypes.END_ROD;
                sound = SoundEvents.AMETHYST_BLOCK_CHIME;
                pitch = 1.5F;
            }
            case DARK -> {
                particle = ParticleTypes.SOUL_FIRE_FLAME;
                sound = SoundEvents.WITHER_SHOOT;
                volume = 0.22F;
                pitch = 1.7F;
            }
            case STEALTH -> {
                particle = ParticleTypes.SMOKE;
                sound = SoundEvents.ENDERMAN_TELEPORT;
                volume = 0.2F;
                pitch = 1.8F;
            }
            case PHYSICAL -> {
                particle = ParticleTypes.CRIT;
                sound = SoundEvents.PLAYER_ATTACK_SWEEP;
                pitch = 1.15F;
            }
            default -> {
                particle = ParticleTypes.END_ROD;
                sound = SoundEvents.EXPERIENCE_ORB_PICKUP;
                volume = 0.2F;
            }
        }
        playCastSpark(level, origin, particle, particles);
        level.playLocalSound(origin.x, origin.y, origin.z, sound, SoundSource.PLAYERS, volume, pitch, false);
        CameraShakeManager.trigger(0.5F, 3);
    }

    private static void playHit(Level level, ResourceLocation skillId, Vec3 origin, double particles) {
        if (SNARE_SHOT.equals(skillId)) {
            playSnareHit(level, origin, particles);
            return;
        }

        ParticleOptions main = hitParticle(skillId);
        int count = scaledCount(8, particles);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double speed = 0.07D + (i % 3) * 0.015D;
            level.addParticle(
                    main,
                    origin.x,
                    origin.y,
                    origin.z,
                    Math.cos(angle) * speed,
                    0.025D,
                    Math.sin(angle) * speed
            );
        }
        int shockRing = scaledCount(12, particles);
        for (int i = 0; i < shockRing; i++) {
            double angle = Math.PI * 2.0D * i / shockRing;
            double speed = 0.15D;
            level.addParticle(ParticleTypes.POOF,
                    origin.x, origin.y + 0.3D, origin.z,
                    Math.cos(angle) * speed, 0.02D, Math.sin(angle) * speed);
        }
        ElementCategory cat = elementCategory(skillId);
        switch (cat) {
            case FIRE -> { HitFlashOverlay.triggerFire(); CameraShakeManager.trigger(2.0F, 8); }
            case ICE -> { HitFlashOverlay.triggerIce(); CameraShakeManager.trigger(1.5F, 6); }
            case DARK -> { HitFlashOverlay.triggerDark(); CameraShakeManager.trigger(1.8F, 7); }
            case HOLY -> { HitFlashOverlay.triggerHoly(); CameraShakeManager.trigger(1.2F, 5); }
            default -> { HitFlashOverlay.triggerWhite(); CameraShakeManager.trigger(1.5F, 6); }
        }
        if (FIREBALL.equals(skillId) || FLAME_ARROW.equals(skillId)) {
            for (int i = 0; i < scaledCount(2, particles); i++) {
                level.addParticle(ParticleTypes.LAVA, origin.x, origin.y, origin.z, 0.0D, 0.02D, 0.0D);
            }
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                    0.32F, 1.75F, false);
        } else {
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS,
                    0.28F, 1.55F, false);
        }
    }

    private static ParticleOptions hitParticle(ResourceLocation skillId) {
        ElementCategory cat = elementCategory(skillId);
        return switch (cat) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case HOLY -> ParticleTypes.END_ROD;
            case DARK -> ParticleTypes.SOUL_FIRE_FLAME;
            case STEALTH -> ParticleTypes.SMOKE;
            case PHYSICAL -> ParticleTypes.CRIT;
            default -> ParticleTypes.END_ROD;
        };
    }

    private static void playEntityHit(Level level, ResourceLocation skillId, Vec3 origin, boolean ally, double particles) {
        ParticleOptions particle = ally ? allyParticle(skillId) : hitParticle(skillId);
        int count = scaledCount(ally ? 5 : 6, particles);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double y = 0.22D + (i % 2) * 0.08D;
            level.addParticle(
                    particle,
                    origin.x + Math.cos(angle) * 0.28D,
                    origin.y + y,
                    origin.z + Math.sin(angle) * 0.28D,
                    Math.cos(angle) * 0.018D,
                    ally ? 0.026D : 0.018D,
                    Math.sin(angle) * 0.018D
            );
        }
        if (!ally) {
            int shockCount = scaledCount(8, particles);
            for (int i = 0; i < shockCount; i++) {
                double angle = Math.PI * 2.0D * i / shockCount;
                double speed = 0.12D + (i % 3) * 0.04D;
                level.addParticle(ParticleTypes.CRIT,
                        origin.x, origin.y + 0.5D, origin.z,
                        Math.cos(angle) * speed, 0.04D, Math.sin(angle) * speed);
            }
            ElementCategory cat = elementCategory(skillId);
            switch (cat) {
                case FIRE -> HitFlashOverlay.triggerFire();
                case ICE -> HitFlashOverlay.triggerIce();
                case DARK -> HitFlashOverlay.triggerDark();
                case HOLY -> HitFlashOverlay.triggerHoly();
                default -> HitFlashOverlay.triggerWhite();
            }
        }
        SoundEvent sound = ally ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.PLAYER_ATTACK_SWEEP;
        float pitch = ally ? 1.65F : 1.15F;
        level.playLocalSound(origin.x, origin.y, origin.z, sound, SoundSource.PLAYERS, 0.18F, pitch, false);
    }

    private static void playBlockHit(Level level, ResourceLocation skillId, Vec3 origin, double particles) {
        ParticleOptions particle = hitParticle(skillId);
        int count = scaledCount(5, particles);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double speed = 0.04D + (i % 2) * 0.012D;
            level.addParticle(
                    particle,
                    origin.x,
                    origin.y + 0.1D,
                    origin.z,
                    Math.cos(angle) * speed,
                    0.035D,
                    Math.sin(angle) * speed
            );
        }
        if (FIREBALL.equals(skillId) || FLAME_ARROW.equals(skillId)) {
            level.addParticle(ParticleTypes.LAVA, origin.x, origin.y, origin.z, 0.0D, 0.015D, 0.0D);
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                    0.25F, 1.5F, false);
        } else {
            level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.ARROW_HIT, SoundSource.PLAYERS,
                    0.22F, 1.4F, false);
        }
    }

    private static ParticleOptions allyParticle(ResourceLocation skillId) {
        if (MEND.equals(skillId) || HOLY_NOVA.equals(skillId) || SERAPHIC_GRACE.equals(skillId)
                || SANCTUARY_DESCENT.equals(skillId)) {
            return ParticleTypes.HEART;
        }
        if (SUNFIRE_AEGIS.equals(skillId) || BLESSING.equals(skillId) || CONSECRATED_SLAM.equals(skillId)) {
            return ParticleTypes.END_ROD;
        }
        return ParticleTypes.ENCHANT;
    }

    private static void playEmberBurst(Level level, Vec3 origin, double particles) {
        Vec3 ground = lowOrigin(origin);
        playGroundRing(level, ground, ParticleTypes.FLAME, 3.0D, scaledCount(18, particles), 0.04D);
        playGroundRing(level, ground, ParticleTypes.SMALL_FLAME, 1.8D, scaledCount(10, particles), 0.02D);
        int sparks = scaledCount(6, particles);
        for (int i = 0; i < sparks; i++) {
            double angle = Math.PI * 2.0D * i / sparks;
            level.addParticle(ParticleTypes.LAVA,
                    ground.x + Math.cos(angle) * 1.2D,
                    ground.y + 0.15D,
                    ground.z + Math.sin(angle) * 1.2D,
                    Math.cos(angle) * 0.008D,
                    0.025D,
                    Math.sin(angle) * 0.008D);
        }
        level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                0.5F, 0.75F, false);
        level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                0.22F, 1.8F, false);
    }

    private static void playFlameStep(Level level, Vec3 origin, Vec3 target, double particles) {
        Vec3 direction = target.subtract(origin);
        double dist = direction.length();
        if (dist < 0.01D) {
            direction = new Vec3(1.0D, 0.0D, 0.0D);
            dist = 1.0D;
        }
        Vec3 norm = direction.scale(1.0D / dist);
        int trailCount = scaledCount(8, particles);
        for (int i = 0; i < trailCount; i++) {
            double t = i / (double) trailCount;
            double px = origin.x + norm.x * t * 2.0D;
            double pz = origin.z + norm.z * t * 2.0D;
            level.addParticle(ParticleTypes.SMALL_FLAME,
                    px + (Math.random() - 0.5D) * 0.3D,
                    origin.y - 0.6D + Math.random() * 0.15D,
                    pz + (Math.random() - 0.5D) * 0.3D,
                    -norm.x * 0.01D,
                    0.015D,
                    -norm.z * 0.01D);
        }
        level.addParticle(ParticleTypes.FLAME,
                origin.x, origin.y - 0.4D, origin.z,
                norm.x * 0.06D, 0.02D, norm.z * 0.06D);
        level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                0.35F, 1.65F, false);
    }

    private static void playSnareHit(Level level, Vec3 origin, double particles) {
        int count = scaledCount(10, particles);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double radius = 0.55D;
            level.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    origin.x + Math.cos(angle) * radius,
                    origin.y + 0.08D,
                    origin.z + Math.sin(angle) * radius,
                    0.0D,
                    0.025D,
                    0.0D
            );
        }
        level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.CHAIN_HIT, SoundSource.PLAYERS,
                0.36F, 1.25F, false);
    }

    private static void playFireball(Level level, Vec3 origin, Vec3 target, double particles) {
        playCastSpark(level, origin, ParticleTypes.FLAME, particles);
        trace(level, origin, target, ParticleTypes.FLAME, scaledCount(5, particles), 0.018D);
        trace(level, origin, target, ParticleTypes.SMALL_FLAME, scaledCount(3, particles), 0.01D);
        int burstCount = scaledCount(6, particles);
        for (int i = 0; i < burstCount; i++) {
            double angle = Math.PI * 2.0D * i / burstCount;
            level.addParticle(ParticleTypes.FLAME,
                    origin.x + Math.cos(angle) * 0.5D,
                    origin.y,
                    origin.z + Math.sin(angle) * 0.5D,
                    -Math.cos(angle) * 0.06D,
                    0.01D,
                    -Math.sin(angle) * 0.06D);
        }
        level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                0.65F, 0.85F, false);
        CameraShakeManager.trigger(1.0F, 5);
    }

    private static void playArrowTrail(Level level, Vec3 origin, Vec3 target, ParticleOptions particle, double particles) {
        playCastSpark(level, origin, ParticleTypes.END_ROD, particles);
        trace(level, origin, target, particle, scaledCount(4, particles), 0.006D);
    }

    private static void playLunge(Level level, Vec3 origin, Vec3 target, double particles) {
        trace(level, origin, target, ParticleTypes.CRIT, scaledCount(5, particles), 0.014D);
        trace(level, origin.add(0.0D, 0.04D, 0.0D), target, ParticleTypes.SWEEP_ATTACK, scaledCount(1, particles), 0.0D);
        level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.45F, 1.05F, false);
    }

    private static void playMend(Level level, Vec3 origin, double particles) {
        Vec3 ground = lowOrigin(origin);
        int count = scaledCount(8, particles);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            level.addParticle(ParticleTypes.HEART,
                    ground.x + Math.cos(angle) * 0.45D,
                    ground.y + 0.16D + (i % 2) * 0.06D,
                    ground.z + Math.sin(angle) * 0.45D,
                    0.0D, 0.035D, 0.0D);
        }
        level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                0.42F, 1.45F, false);
    }

    private static void playBlessing(Level level, Vec3 origin, double particles) {
        Vec3 ground = lowOrigin(origin);
        int count = scaledCount(10, particles);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            level.addParticle(ParticleTypes.ENCHANT,
                    ground.x + Math.cos(angle) * 0.62D,
                    ground.y + 0.22D,
                    ground.z + Math.sin(angle) * 0.62D,
                    -Math.cos(angle) * 0.02D,
                    0.026D,
                    -Math.sin(angle) * 0.02D);
        }
        level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,
                0.38F, 1.2F, false);
    }

    private static void playBlazingCharge(Level level, Vec3 origin, Vec3 target, double particles) {
        trace(level, origin, target, ParticleTypes.FLAME, scaledCount(5, particles), 0.018D);
        trace(level, origin.add(0.0D, 0.06D, 0.0D), target, ParticleTypes.SWEEP_ATTACK, scaledCount(1, particles), 0.0D);
        trace(level, origin, target, ParticleTypes.SMALL_FLAME, scaledCount(3, particles), 0.01D);
        level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.48F, 0.92F, false);
    }

    private static void playSunfireAegis(Level level, Vec3 origin, double particles) {
        Vec3 ground = lowOrigin(origin);
        playGroundRing(level, ground, ParticleTypes.FLAME, 1.2D, scaledCount(12, particles), 0.016D);
        playGroundRing(level, ground, ParticleTypes.END_ROD, 1.75D, scaledCount(14, particles), 0.014D);
        int count = scaledCount(6, particles);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            level.addParticle(ParticleTypes.ENCHANT,
                    ground.x + Math.cos(angle) * 0.55D,
                    ground.y + 0.18D + (i % 3) * 0.06D,
                    ground.z + Math.sin(angle) * 0.55D,
                    -Math.cos(angle) * 0.016D,
                    0.026D,
                    -Math.sin(angle) * 0.016D);
        }
        level.playLocalSound(origin.x, origin.y, origin.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS,
                0.42F, 1.15F, false);
    }

    private static void playGroundRing(Level level, Vec3 origin, ParticleOptions particle, double radius, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            level.addParticle(
                    particle,
                    origin.x + Math.cos(angle) * radius,
                    origin.y + 0.03D,
                    origin.z + Math.sin(angle) * radius,
                    Math.cos(angle) * speed,
                    0.012D,
                    Math.sin(angle) * speed
            );
        }
    }

    private static void playCastSpark(Level level, Vec3 origin, ParticleOptions particle, double particles) {
        Vec3 ground = lowOrigin(origin);
        int ringCount = scaledCount(10, particles);
        double radius = 0.8D;
        for (int i = 0; i < ringCount; i++) {
            double angle = Math.PI * 2.0D * i / ringCount;
            level.addParticle(ParticleTypes.ENCHANT,
                    ground.x + Math.cos(angle) * radius,
                    ground.y + 0.05D,
                    ground.z + Math.sin(angle) * radius,
                    -Math.cos(angle) * 0.015D,
                    0.035D,
                    -Math.sin(angle) * 0.015D);
        }
        int sparkCount = scaledCount(4, particles);
        for (int i = 0; i < sparkCount; i++) {
            double angle = Math.PI * 2.0D * i / sparkCount;
            level.addParticle(particle,
                    origin.x + Math.cos(angle) * 0.15D,
                    origin.y - 0.2D,
                    origin.z + Math.sin(angle) * 0.15D,
                    Math.cos(angle) * 0.008D,
                    0.04D + (i % 2) * 0.02D,
                    Math.sin(angle) * 0.008D);
        }
    }

    private static void trace(Level level, Vec3 origin, Vec3 target, ParticleOptions particle, int steps, double speed) {
        if (steps <= 0) {
            return;
        }
        Vec3 delta = target.subtract(origin);
        if (delta.lengthSqr() < 0.01D) {
            delta = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 step = delta.scale(1.0D / Math.max(1, steps));
        Vec3 motion = delta.normalize().scale(speed);
        for (int i = 1; i <= steps; i++) {
            Vec3 point = origin.add(step.scale(i));
            level.addParticle(particle, point.x, point.y, point.z, motion.x, motion.y, motion.z);
        }
    }

    private static int scaledCount(int baseCount, double multiplier) {
        if (baseCount <= 0 || multiplier <= 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.round(baseCount * multiplier));
    }

    private static Vec3 lowOrigin(Vec3 origin) {
        return origin.add(GROUND_FX_OFFSET);
    }
}
