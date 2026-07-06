package com.hongyuwu.careerchronicle.player;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.config.ModConfig;
import com.hongyuwu.careerchronicle.career.CareerProgressionService;
import com.hongyuwu.careerchronicle.network.NetworkHandler;
import com.hongyuwu.careerchronicle.registry.CareerItems;
import com.hongyuwu.careerchronicle.skill.CareerArrowTags;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID)
public final class CareerPlayerEvents {
    private static final String CAREER_CLONE_BACKUP = "careerchronicleCloneBackup";
    private static final String EXPLORED_BIOMES_TAG = "careerchronicleExploredBiomes";
    private static final String DISCOVERED_STRUCTURES_TAG = "careerchronicleDiscoveredStructures";
    private static final ResourceLocation ELF_RACE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "elf");
    private static final ResourceLocation DWARF_RACE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "dwarf");
    private static final ResourceLocation ORC_RACE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "orc");
    private static final ResourceLocation UNDEAD_RACE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "undead");
    private static final ResourceLocation DEMON_RACE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "demon");
    private static final java.util.UUID ATTR_HP_UUID = java.util.UUID.fromString("c8a1e3b0-7a4f-4c6e-b2d1-f0e9a8c7d6b5");
    private static final java.util.UUID ATTR_DMG_UUID = java.util.UUID.fromString("d9b2f4c1-8b5e-4d7f-c3e2-a1f0b9d8e7c6");
    private static final java.util.UUID ATTR_SPD_UUID = java.util.UUID.fromString("e0c3a5d2-9c6f-4e8a-d4f3-b2a1c0e9f8d7");
    private static final java.util.UUID ATTR_ARM_UUID = java.util.UUID.fromString("f1d4b6e3-0d7a-4f9b-e5a4-c3b2d1f0a9e8");

    private static final ResourceLocation GUIDING_LIGHT_ARROW =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "guiding_light_arrow");
    private static final ResourceLocation PIERCING_VOLLEY =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "piercing_volley");

    private CareerPlayerEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            CareerDataProvider provider = new CareerDataProvider();
            event.addCapability(CareerDataCapability.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag backup = event.getOriginal().getPersistentData().getCompound(CAREER_CLONE_BACKUP);
        if (!backup.isEmpty()) {
            CareerDataAccess.get(event.getEntity()).ifPresent(newData ->
                    newData.deserializePersistentData(backup));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            giveStarterKit(player);
            refreshProgressionAndSync(player);
            scheduleRaceSelectionIfNeeded(player);
        }
    }

    private static void scheduleRaceSelectionIfNeeded(ServerPlayer player) {
        CareerDataAccess.get(player).ifPresent(data -> {
            if (CareerDataNbt.UNSELECTED_RACE.equals(data.getRace())) {
                player.getServer().execute(() -> {
                    NetworkHandler.openCareerScreen(player);
                });
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refreshProgressionAndSync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refreshProgressionAndSync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        CareerDataAccess.get(player).ifPresent(data -> {
            data.getRuntimeState().tickCooldowns();
            updateResources(player, data);
            if (player.tickCount % 100 == 0) {
                applyAttributeModifiers(player, data);
                if (ELF_RACE.equals(data.getRace())) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, true));
                } else if (DWARF_RACE.equals(data.getRace())) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false, true));
                } else if (ORC_RACE.equals(data.getRace())) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, true, false, true));
                } else if (UNDEAD_RACE.equals(data.getRace())) {
                    int light = player.level().getMaxLocalRawBrightness(player.blockPosition());
                    if (light <= 7) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false, true));
                    }
                } else if (DEMON_RACE.equals(data.getRace())) {
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 220, 0, true, false, true));
                }
            }
        });
        if (player.tickCount % 200 == 0) {
            awardBiomeExplorationXp(player);
            awardStructureDiscoveryXp(player);
        }

        int chargedTicks = player.getPersistentData().getInt("careerchronicleChargedShotTicks");
        if (chargedTicks > 0) {
            player.getPersistentData().putInt("careerchronicleChargedShotTicks", chargedTicks - 1);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof ServerPlayer player)) {
            return;
        }

        if (player.getPersistentData().getInt("careerchronicleChargedShotTicks") > 0) {
            arrow.setBaseDamage(arrow.getBaseDamage() + 2.0D);
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.35D));
            player.getPersistentData().remove("careerchronicleChargedShotTicks");
            sendSkillParticles(player, ParticleTypes.CRIT,
                    arrow.getX(), arrow.getY(), arrow.getZ(), 18, 0.15D, 0.15D, 0.15D, 0.02D);
        }

        CareerDataAccess.get(player).ifPresent(data -> {
            if (data.getRuntimeState().consumeNextProjectileModifier() != null) {
                arrow.setSecondsOnFire(8);
                arrow.setBaseDamage(arrow.getBaseDamage() + 1.0D);
                CareerArrowTags.mark(arrow,
                        ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "flame_arrow"),
                        true,
                        false);
                sendSkillParticles(player, ParticleTypes.FLAME,
                        arrow.getX(), arrow.getY(), arrow.getZ(), 20, 0.18D, 0.18D, 0.18D, 0.03D);
                CareerDataAccess.sync(player);
            }
        });
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)
                || !CareerArrowTags.isCareerArrow(arrow)) {
            return;
        }

        ResourceLocation skillId = CareerArrowTags.skillId(arrow);
        if (skillId == null) {
            return;
        }

        if (event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof Player targetPlayer
                && !canSkillPvp(player, targetPlayer)) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            return;
        }

        if (!CareerArrowTags.tryConsumeHit(arrow)) {
            return;
        }

        if (CareerArrowTags.isSnareArrow(arrow) && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        }
        if (GUIDING_LIGHT_ARROW.equals(skillId)
                && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        }
        if (PIERCING_VOLLEY.equals(skillId)
                && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0));
        }

        boolean entityHit = event.getRayTraceResult() instanceof EntityHitResult;
        String fxType = entityHit ? "entity_hit" : "block_hit";
        NetworkHandler.playSkillFx(player, skillId, fxType,
                event.getRayTraceResult().getLocation(),
                event.getRayTraceResult().getLocation());
        if (entityHit && ((EntityHitResult) event.getRayTraceResult()).getEntity() instanceof LivingEntity) {
            CareerProgressionService.awardCareerBehaviorXp(
                    player,
                    CareerProgressionService.RANGED_HIT_XP_SOURCE,
                    4
            );
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getBlockedDamage() <= 0.0F) {
            return;
        }
        CareerProgressionService.awardCareerBehaviorXp(
                player,
                CareerProgressionService.GUARD_BLOCK_XP_SOURCE,
                6
        );
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer dyingPlayer) {
            CareerDataAccess.get(dyingPlayer).ifPresent(data ->
                    dyingPlayer.getPersistentData().put(CAREER_CLONE_BACKUP, data.serializePersistentData()));
        }
        if (!(event.getEntity().getKillCredit() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target instanceof Player) {
            return;
        }
        CareerProgressionService.awardKillXp(player, target.getMaxHealth());
    }

    private static void awardBiomeExplorationXp(ServerPlayer player) {
        player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(key -> {
            if (!key.isFor(Registries.BIOME)) {
                return;
            }
            String biomeId = key.location().toString();
            var explored = player.getPersistentData().getCompound(EXPLORED_BIOMES_TAG);
            if (explored.getBoolean(biomeId)) {
                return;
            }
            explored.putBoolean(biomeId, true);
            player.getPersistentData().put(EXPLORED_BIOMES_TAG, explored);
            CareerProgressionService.awardBiomeExplorationXp(player, key.location().getPath());
        });
    }

    private static void awardStructureDiscoveryXp(ServerPlayer player) {
        var structureRegistry = player.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        CompoundTag discovered = player.getPersistentData().getCompound(DISCOVERED_STRUCTURES_TAG);
        boolean changed = false;
        for (Map.Entry<net.minecraft.world.level.levelgen.structure.Structure, ?> entry :
                player.serverLevel().structureManager().getAllStructuresAt(player.blockPosition()).entrySet()) {
            ResourceLocation structureId = structureRegistry.getKey(entry.getKey());
            if (structureId == null) {
                continue;
            }
            String key = structureId.toString();
            if (discovered.getBoolean(key)) {
                continue;
            }
            discovered.putBoolean(key, true);
            changed = true;
            CareerProgressionService.awardStructureDiscoveryXp(player, structureId.getPath());
        }
        if (changed) {
            player.getPersistentData().put(DISCOVERED_STRUCTURES_TAG, discovered);
        }
    }

    private static void giveStarterKit(ServerPlayer player) {
        giveIfMissing(player, CareerItems.CAREER_MANUAL.get());
    }

    private static void giveIfMissing(ServerPlayer player, Item item) {
        boolean hasManual = player.getInventory().items.stream()
                .anyMatch(stack -> stack.is(item));
        if (hasManual) {
            return;
        }

        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void refreshProgressionAndSync(ServerPlayer player) {
        CareerDataAccess.get(player).ifPresent(data -> {
            CareerProgressionService.refreshGrantedSkills(data);
            initializeResources(data);
        });
        CareerDataAccess.sync(player);
    }

    private static void updateResources(ServerPlayer player, ICareerData data) {
        CareerRuntimeState state = data.getRuntimeState();
        int maxMana = maxMana(data);
        int maxStamina = maxStamina(data);
        boolean shouldSync = false;
        if (state.getMaxMana() != maxMana || state.getMaxStamina() != maxStamina) {
            state.setResourceCaps(maxMana, maxStamina);
            if (state.getMana() <= 0 || state.getStamina() <= 0) {
                state.refillResources();
            }
            shouldSync = true;
        }
        if (player.tickCount % 20 == 0 && state.regenerateResources(1, 1)) {
            shouldSync = true;
        }
        if (shouldSync) {
            CareerDataAccess.sync(player);
        }
    }

    private static void initializeResources(ICareerData data) {
        CareerRuntimeState state = data.getRuntimeState();
        state.setResourceCaps(maxMana(data), maxStamina(data));
        state.refillResources();
    }

    private static int maxMana(ICareerData data) {
        return 80 + Math.max(0, data.getCareerLevel() - 1) * 2;
    }

    private static int maxStamina(ICareerData data) {
        return 80 + Math.max(0, data.getCareerLevel() - 1) * 2;
    }

    private static void applyAttributeModifiers(ServerPlayer player, ICareerData data) {
        int con = data.getAttribute(CareerData.ATTR_CON);
        int str = data.getAttribute(CareerData.ATTR_STR);
        int dex = data.getAttribute(CareerData.ATTR_DEX);

        double hpBonus = (con - CareerData.BASE_ATTRIBUTE) * 2.0;
        applyModifier(player, Attributes.MAX_HEALTH, ATTR_HP_UUID, "career_con_hp", hpBonus);

        double dmgBonus = (str - CareerData.BASE_ATTRIBUTE) * 0.5;
        applyModifier(player, Attributes.ATTACK_DAMAGE, ATTR_DMG_UUID, "career_str_dmg", dmgBonus);

        double spdBonus = (dex - CareerData.BASE_ATTRIBUTE) * 0.008;
        applyModifier(player, Attributes.MOVEMENT_SPEED, ATTR_SPD_UUID, "career_dex_spd", spdBonus);

        double armBonus = (con - CareerData.BASE_ATTRIBUTE) * 0.4;
        applyModifier(player, Attributes.ARMOR, ATTR_ARM_UUID, "career_con_arm", armBonus);
    }

    private static void applyModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                      java.util.UUID uuid, String name, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) {
            if (Math.abs(existing.getAmount() - amount) < 0.001) return;
            instance.removeModifier(uuid);
        }
        if (Math.abs(amount) > 0.001) {
            instance.addTransientModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void sendSkillParticles(ServerPlayer player, net.minecraft.core.particles.ParticleOptions particle,
                                           double x, double y, double z, int count,
                                           double offsetX, double offsetY, double offsetZ, double speed) {
        int scaled = scaledParticleCount(count);
        if (scaled <= 0) {
            return;
        }
        player.serverLevel().sendParticles(particle, x, y, z, scaled, offsetX, offsetY, offsetZ, speed);
    }

    private static int scaledParticleCount(int baseCount) {
        double multiplier = ModConfig.SKILL_PARTICLE_MULTIPLIER.get();
        if (baseCount <= 0 || multiplier <= 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.round(baseCount * multiplier));
    }

    private static boolean canSkillPvp(ServerPlayer player, Player target) {
        return ModConfig.ENABLE_SKILL_PVP.get() && player.canHarmPlayer(target);
    }
}
