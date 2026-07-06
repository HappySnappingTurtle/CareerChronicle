package com.hongyuwu.careerchronicle.career;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.ClassDef;
import com.hongyuwu.careerchronicle.data.FusionDef;
import com.hongyuwu.careerchronicle.data.HiddenUnlockDef;
import com.hongyuwu.careerchronicle.data.RaceDef;
import com.hongyuwu.careerchronicle.data.RepeatRewardDef;
import com.hongyuwu.careerchronicle.data.RegistrySnapshot;
import com.hongyuwu.careerchronicle.data.XpSourceDef;
import com.hongyuwu.careerchronicle.config.ModConfig;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import com.hongyuwu.careerchronicle.player.CareerDataNbt;
import com.hongyuwu.careerchronicle.player.ICareerData;
import com.hongyuwu.careerchronicle.skill.CareerLoadoutService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class CareerProgressionService {
    public static final ResourceLocation KILL_XP_SOURCE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "kill");
    public static final ResourceLocation BIOME_XP_SOURCE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "biome");
    public static final ResourceLocation STRUCTURE_XP_SOURCE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "structure");
    public static final ResourceLocation COMMAND_XP_SOURCE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "command");
    public static final ResourceLocation RANGED_HIT_XP_SOURCE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ranged_hit");
    public static final ResourceLocation HEALING_XP_SOURCE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "healing");
    public static final ResourceLocation GUARD_BLOCK_XP_SOURCE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "guard_block");

    private CareerProgressionService() {
    }

    public static void selectRace(ServerPlayer player, ResourceLocation raceId) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        RaceDef race = registry.races().get(raceId);
        if (race == null) {
            player.sendSystemMessage(Component.translatable("careerchronicle.message.unknown_race", raceId.toString())
                    .withStyle(ChatFormatting.RED));
            return;
        }

        CareerDataAccess.get(player).ifPresent(data -> {
            if (!CareerDataNbt.UNSELECTED_RACE.equals(data.getRace())) {
                player.sendSystemMessage(Component.translatable("careerchronicle.message.race_already_selected")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            data.setRace(race.id());
            CareerDataAccess.sync(player);
            player.sendSystemMessage(Component.translatable("careerchronicle.message.race_selected",
                    Component.translatable(race.displayKey())).withStyle(ChatFormatting.GREEN));
        });
    }

    public static void selectClass(ServerPlayer player, ResourceLocation classId) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        ClassDef careerClass = registry.classes().get(classId);
        if (careerClass == null) {
            player.sendSystemMessage(Component.translatable("careerchronicle.message.unknown_class", classId.toString())
                    .withStyle(ChatFormatting.RED));
            return;
        }

        CareerDataAccess.get(player).ifPresent(data -> {
            if (CareerDataNbt.UNSELECTED_RACE.equals(data.getRace())) {
                player.sendSystemMessage(Component.translatable("careerchronicle.message.select_race_first")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            if (!canUseClass(registry, data, classId, careerClass)) {
                player.sendSystemMessage(Component.translatable("careerchronicle.message.class_not_allowed",
                        Component.translatable(careerClass.displayKey())).withStyle(ChatFormatting.RED));
                return;
            }

            int selectedSegments = data.getClassHistory().size();
            if (!CareerProgressionMath.hasAlphaSegmentSlot(selectedSegments)) {
                player.sendSystemMessage(Component.translatable("careerchronicle.message.class_segments_full",
                        CareerProgressionMath.MAX_ALPHA_SEGMENTS).withStyle(ChatFormatting.RED));
                return;
            }

            int requiredLevel = CareerProgressionMath.requiredLevelForNextSegment(selectedSegments);
            if (data.getCareerLevel() < requiredLevel) {
                player.sendSystemMessage(Component.translatable("careerchronicle.message.class_segment_locked",
                        CareerProgressionMath.nextSegmentIndex(selectedSegments), requiredLevel)
                        .withStyle(ChatFormatting.RED));
                return;
            }

            data.addClassHistory(careerClass.id());
            if (ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "human").equals(data.getRace())) {
                data.addCareerXp(scaleCareerXp(25));
            }
            applyPendingLevelUps(data);
            for (ResourceLocation skillId : careerClass.grantsSkills()) {
                data.unlockSkill(skillId);
            }
            unlockRepeatRewards(data, registry);
            unlockMatchingFusions(data, registry);
            boolean hiddenChanged = unlockMatchingHiddenUnlocks(data, registry);
            autoFillLoadout(data);
            giveClassWeapon(player, careerClass.id());
            CareerDataAccess.sync(player);
            player.sendSystemMessage(Component.translatable("careerchronicle.message.class_selected",
                    Component.translatable(careerClass.displayKey())).withStyle(ChatFormatting.GREEN));
            if (hiddenChanged) {
                player.displayClientMessage(Component.translatable("careerchronicle.message.hidden_clue_revealed")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        });
    }

    public static boolean refreshGrantedSkills(ICareerData data) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        boolean changed = false;

        Set<ResourceLocation> validSkills = new LinkedHashSet<>();
        for (ResourceLocation classId : data.getClassHistory()) {
            ClassDef careerClass = registry.classes().get(classId);
            if (careerClass == null) {
                continue;
            }
            for (ResourceLocation skillId : careerClass.grantsSkills()) {
                if (registry.skills().containsKey(skillId)) {
                    validSkills.add(skillId);
                }
            }
        }

        collectRepeatRewards(data, registry, validSkills);
        collectMatchingFusions(data, registry, validSkills);
        for (ResourceLocation skillId : validSkills) {
            changed |= data.unlockSkill(skillId);
        }
        changed |= data.retainUnlockedSkills(validSkills);
        changed |= data.getRuntimeState().retainCooldowns(validSkills);
        changed |= unlockMatchingHiddenUnlocks(data, registry);
        if (changed || shouldRefreshLoadout(data)) {
            pruneAndFillLoadout(data);
            changed = true;
        }
        return changed;
    }

    private static boolean unlockMatchingHiddenUnlocks(ICareerData data, RegistrySnapshot registry) {
        boolean changed = false;
        HistoryScores scores = historyScores(data.getClassHistory(), registry);
        for (HiddenUnlockDef hiddenUnlock : registry.hiddenUnlocks().values()) {
            if (!matches(scores.classCounts(), hiddenUnlock.requiredClassCounts())
                    || !matches(scores.tagScores(), hiddenUnlock.requiredTagScores())) {
                continue;
            }
            if (!data.getHiddenFlags().contains(hiddenUnlock.unlockFlag())) {
                data.setHiddenFlag(hiddenUnlock.unlockFlag(), true);
                CareerChronicleMod.LOGGER.info("Revealed hidden career clue {} as flag {}",
                        hiddenUnlock.id(), hiddenUnlock.unlockFlag());
                changed = true;
            }
        }
        return changed;
    }

    private static boolean canUseClass(
            RegistrySnapshot registry,
            ICareerData data,
            ResourceLocation classId,
            ClassDef careerClass
    ) {
        if (careerClass.hidden()) {
            return careerClass.unlockFlag() != null && data.getHiddenFlags().contains(careerClass.unlockFlag());
        }
        RaceDef race = registry.races().get(data.getRace());
        if (race != null && !race.allowedClasses().isEmpty() && !race.allowedClasses().contains(classId)) {
            return false;
        }
        return careerClass.meetsAttributeRequirements(data::getAttribute);
    }

    private static HistoryScores historyScores(List<ResourceLocation> classHistory, RegistrySnapshot registry) {
        Map<ResourceLocation, Integer> classCounts = new LinkedHashMap<>();
        Map<ResourceLocation, Integer> tagScores = new LinkedHashMap<>();
        for (ResourceLocation classId : classHistory) {
            classCounts.merge(classId, 1, Integer::sum);
            ClassDef careerClass = registry.classes().get(classId);
            if (careerClass == null) {
                continue;
            }
            for (ResourceLocation tag : careerClass.tags()) {
                tagScores.merge(tag, 1, Integer::sum);
            }
        }
        return new HistoryScores(classCounts, tagScores);
    }

    private static boolean matches(Map<ResourceLocation, Integer> scores, Map<ResourceLocation, Integer> requirements) {
        for (Map.Entry<ResourceLocation, Integer> requirement : requirements.entrySet()) {
            if (scores.getOrDefault(requirement.getKey(), 0) < requirement.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static void awardCareerXp(ServerPlayer player, int amount, Component source) {
        int scaledAmount = scaleCareerXp(amount);
        if (scaledAmount <= 0) {
            return;
        }
        CareerDataAccess.get(player).ifPresent(data -> {
            if (CareerDataNbt.UNSELECTED_RACE.equals(data.getRace())) {
                return;
            }
            int oldLevel = data.getCareerLevel();
            data.addCareerXp(scaledAmount);
            applyPendingLevelUps(data);
            CareerDataAccess.sync(player);
            Component message = Component.translatable("careerchronicle.message.career_xp_gain", scaledAmount, source)
                    .withStyle(ChatFormatting.AQUA);
            if (data.getCareerLevel() > oldLevel) {
                message = Component.translatable("careerchronicle.message.career_level_up",
                        data.getCareerLevel(), scaledAmount, source).withStyle(ChatFormatting.GOLD);
            }
            if (CareerProgressionMath.hasPendingSegmentChoice(data.getClassHistory().size(), data.getCareerLevel())) {
                message = Component.translatable("careerchronicle.message.class_segment_ready",
                        CareerProgressionMath.nextSegmentIndex(data.getClassHistory().size()))
                        .withStyle(ChatFormatting.GOLD);
            }
            player.displayClientMessage(message, true);
        });
    }

    private static int scaleCareerXp(int amount) {
        double multiplier = ModConfig.CAREER_XP_MULTIPLIER.get();
        if (amount <= 0 || multiplier <= 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.round(amount * multiplier));
    }

    public static void awardCareerXpFromSource(ServerPlayer player, ResourceLocation sourceId, int fallbackAmount) {
        XpSourceDef source = xpSource(sourceId);
        awardCareerXp(player, Math.max(0, fallbackAmount), sourceComponent(sourceId, source));
    }

    public static void awardCareerBehaviorXp(ServerPlayer player, ResourceLocation sourceId, int fallbackAmount) {
        XpSourceDef source = xpSource(sourceId);
        int amount = source == null ? Math.max(0, fallbackAmount) : source.amountForHealth(0.0F);
        amount = applyAntiFarm(player, sourceId.getPath(), amount, 8, 30);
        awardCareerXp(player, amount, sourceComponent(sourceId, source));
    }

    public static void awardKillXp(ServerPlayer player, float targetMaxHealth) {
        XpSourceDef source = xpSource(KILL_XP_SOURCE);
        int fallback = Math.max(6, Math.min(65, Math.round(Math.max(0.0F, targetMaxHealth) * 0.55F)));
        int amount = source == null ? fallback : source.amountForHealth(targetMaxHealth);
        amount = applyAntiFarm(player, "kill", amount, 10, 60);
        awardCareerXp(player, amount, sourceComponent(KILL_XP_SOURCE, source));
    }

    private static int applyAntiFarm(ServerPlayer player, String category, int amount, int maxEventsPerWindow, int windowSeconds) {
        if (amount <= 0) return 0;
        var data = player.getPersistentData();
        String countKey = "ccAntiFarm_" + category + "_count";
        String timeKey = "ccAntiFarm_" + category + "_time";
        long now = player.level().getGameTime();
        long windowTicks = windowSeconds * 20L;
        long lastReset = data.getLong(timeKey);
        int count = data.getInt(countKey);
        if (now - lastReset > windowTicks) {
            count = 0;
            data.putLong(timeKey, now);
        }
        count++;
        data.putInt(countKey, count);
        if (count > maxEventsPerWindow * 2) {
            return 0;
        }
        if (count > maxEventsPerWindow) {
            return amount / 2;
        }
        return amount;
    }

    public static void awardBiomeExplorationXp(ServerPlayer player, String biomeName) {
        XpSourceDef source = xpSource(BIOME_XP_SOURCE);
        int amount = source == null ? 35 : source.amountForHealth(0.0F);
        MutableComponent sourceText = source == null
                ? Component.translatable("careerchronicle.xp_source.biome", biomeName)
                : Component.translatable(source.displayKey(), biomeName);
        awardCareerXp(player, amount, sourceText.withStyle(ChatFormatting.GRAY));
    }

    public static void awardStructureDiscoveryXp(ServerPlayer player, String structureName) {
        XpSourceDef source = xpSource(STRUCTURE_XP_SOURCE);
        int amount = source == null ? 55 : source.amountForHealth(0.0F);
        MutableComponent sourceText = source == null
                ? Component.translatable("careerchronicle.xp_source.structure", structureName)
                : Component.translatable(source.displayKey(), structureName);
        awardCareerXp(player, amount, sourceText.withStyle(ChatFormatting.GRAY));
    }

    private static XpSourceDef xpSource(ResourceLocation sourceId) {
        return CareerRegistry.snapshot().xpSources().get(sourceId);
    }

    private static Component sourceComponent(ResourceLocation sourceId, XpSourceDef source) {
        String key = source == null
                ? "careerchronicle.xp_source." + sourceId.getPath()
                : source.displayKey();
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }

    private static void applyPendingLevelUps(ICareerData data) {
        int levelCap = CareerProgressionMath.levelCapForSelectedSegments(data.getClassHistory().size());
        int oldLevel = data.getCareerLevel();
        while (data.getCareerLevel() < levelCap
                && data.getCareerXp() >= CareerProgressionMath.xpForNextLevel(data.getCareerLevel())) {
            int needed = CareerProgressionMath.xpForNextLevel(data.getCareerLevel());
            data.setCareerXp(data.getCareerXp() - needed);
            data.setCareerLevel(data.getCareerLevel() + 1);
        }
        int levelsGained = data.getCareerLevel() - oldLevel;
        if (levelsGained > 0) {
            data.setUnspentAttributePoints(data.getUnspentAttributePoints()
                    + levelsGained * com.hongyuwu.careerchronicle.player.CareerData.POINTS_PER_LEVEL);
        }
        if (CareerProgressionMath.hasPendingSegmentChoice(data.getClassHistory().size(), data.getCareerLevel())) {
            data.setCareerXp(Math.min(data.getCareerXp(), CareerProgressionMath.xpForNextLevel(data.getCareerLevel())));
        }
    }

    private static boolean shouldRefreshLoadout(ICareerData data) {
        int targetSize = Math.min(CareerLoadoutService.ACTIVE_SLOT_COUNT, data.getUnlockedSkills().size());
        if (data.getSkillLoadout().size() < targetSize) {
            return true;
        }
        for (ResourceLocation skillId : data.getSkillLoadout()) {
            if (!data.getUnlockedSkills().contains(skillId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean unlockMatchingFusions(ICareerData data, RegistrySnapshot registry) {
        boolean changed = false;
        HistoryScores scores = historyScores(data.getClassHistory(), registry);

        for (FusionDef fusion : registry.fusions().values()) {
            if (!matches(scores.classCounts(), fusion.requiredClassCounts())
                    || !matches(scores.tagScores(), fusion.requiredTagScores())) {
                continue;
            }
            if (data.unlockSkill(fusion.unlockSkill())) {
                CareerChronicleMod.LOGGER.info("Unlocked fusion skill {} from {}", fusion.unlockSkill(), fusion.id());
                changed = true;
            }
        }
        return changed;
    }

    private static void collectMatchingFusions(
            ICareerData data,
            RegistrySnapshot registry,
            Set<ResourceLocation> validSkills
    ) {
        HistoryScores scores = historyScores(data.getClassHistory(), registry);

        for (FusionDef fusion : registry.fusions().values()) {
            if (!matches(scores.classCounts(), fusion.requiredClassCounts())
                    || !matches(scores.tagScores(), fusion.requiredTagScores())) {
                continue;
            }
            if (registry.skills().containsKey(fusion.unlockSkill())) {
                validSkills.add(fusion.unlockSkill());
            }
        }
    }

    private static boolean unlockRepeatRewards(ICareerData data, RegistrySnapshot registry) {
        boolean changed = false;
        Map<ResourceLocation, Integer> classCounts = new LinkedHashMap<>();
        for (ResourceLocation classId : data.getClassHistory()) {
            classCounts.merge(classId, 1, Integer::sum);
        }

        for (Map.Entry<ResourceLocation, Integer> entry : classCounts.entrySet()) {
            ClassDef careerClass = registry.classes().get(entry.getKey());
            if (careerClass == null) {
                continue;
            }
            for (RepeatRewardDef reward : careerClass.repeatRewards()) {
                if (entry.getValue() >= reward.requiredCount() && data.unlockSkill(reward.unlockSkill())) {
                    CareerChronicleMod.LOGGER.info("Unlocked repeat reward skill {} from {} x{}",
                            reward.unlockSkill(), careerClass.id(), reward.requiredCount());
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static void collectRepeatRewards(
            ICareerData data,
            RegistrySnapshot registry,
            Set<ResourceLocation> validSkills
    ) {
        Map<ResourceLocation, Integer> classCounts = new LinkedHashMap<>();
        for (ResourceLocation classId : data.getClassHistory()) {
            classCounts.merge(classId, 1, Integer::sum);
        }

        for (Map.Entry<ResourceLocation, Integer> entry : classCounts.entrySet()) {
            ClassDef careerClass = registry.classes().get(entry.getKey());
            if (careerClass == null) {
                continue;
            }
            for (RepeatRewardDef reward : careerClass.repeatRewards()) {
                if (entry.getValue() >= reward.requiredCount()
                        && registry.skills().containsKey(reward.unlockSkill())) {
                    validSkills.add(reward.unlockSkill());
                }
            }
        }
    }

    private static void giveClassWeapon(ServerPlayer player, ResourceLocation classId) {
        String path = classId.getPath();
        net.minecraft.world.item.Item weapon = switch (path) {
            case "fire_mage" -> com.hongyuwu.careerchronicle.registry.CareerItems.EMBER_STAFF.get();
            case "archer" -> com.hongyuwu.careerchronicle.registry.CareerItems.CHRONICLE_RECURVE.get();
            case "warrior" -> com.hongyuwu.careerchronicle.registry.CareerItems.RUNIC_BLADE.get();
            case "priest" -> com.hongyuwu.careerchronicle.registry.CareerItems.SUNLIT_SIGIL.get();
            case "ice_mage" -> com.hongyuwu.careerchronicle.registry.CareerItems.FROST_STAFF.get();
            case "rogue" -> com.hongyuwu.careerchronicle.registry.CareerItems.SHADOW_DAGGER.get();
            case "guardian" -> com.hongyuwu.careerchronicle.registry.CareerItems.GUARDIAN_SHIELD.get();
            case "necromancer" -> com.hongyuwu.careerchronicle.registry.CareerItems.DARK_SCEPTER.get();
            case "lich" -> com.hongyuwu.careerchronicle.registry.CareerItems.DARK_SCEPTER.get();
            case "death_knight" -> com.hongyuwu.careerchronicle.registry.CareerItems.RUNIC_BLADE.get();
            default -> null;
        };
        if (weapon != null) {
            boolean has = player.getInventory().items.stream().anyMatch(s -> s.is(weapon));
            if (!has) {
                net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(weapon);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
    }

    private static void autoFillLoadout(ICareerData data) {
        List<ResourceLocation> loadout = new ArrayList<>();
        for (ResourceLocation skillId : data.getUnlockedSkills()) {
            loadout.add(skillId);
            if (loadout.size() >= CareerLoadoutService.ACTIVE_SLOT_COUNT) {
                break;
            }
        }
        data.setSkillLoadout(loadout);
    }

    private static void pruneAndFillLoadout(ICareerData data) {
        List<ResourceLocation> loadout = new ArrayList<>();
        Set<ResourceLocation> used = new LinkedHashSet<>();
        for (ResourceLocation skillId : data.getSkillLoadout()) {
            if (data.getUnlockedSkills().contains(skillId) && used.add(skillId)) {
                loadout.add(skillId);
            }
            if (loadout.size() >= CareerLoadoutService.ACTIVE_SLOT_COUNT) {
                data.setSkillLoadout(loadout);
                return;
            }
        }
        for (ResourceLocation skillId : data.getUnlockedSkills()) {
            if (used.add(skillId)) {
                loadout.add(skillId);
            }
            if (loadout.size() >= CareerLoadoutService.ACTIVE_SLOT_COUNT) {
                break;
            }
        }
        data.setSkillLoadout(loadout);
    }

    private record HistoryScores(
            Map<ResourceLocation, Integer> classCounts,
            Map<ResourceLocation, Integer> tagScores
    ) {
    }
}
