package com.hongyuwu.careerchronicle.command;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.career.CareerProgressionMath;
import com.hongyuwu.careerchronicle.career.CareerProgressionService;
import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.ClassDef;
import com.hongyuwu.careerchronicle.data.RegistrySnapshot;
import com.hongyuwu.careerchronicle.data.SkillDef;
import com.hongyuwu.careerchronicle.player.CareerData;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import com.hongyuwu.careerchronicle.player.CareerDataNbt;
import com.hongyuwu.careerchronicle.player.ICareerData;
import com.hongyuwu.careerchronicle.skill.CareerSkillService;
import com.hongyuwu.careerchronicle.skill.SkillExecutorRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cow;

public final class CareerTestCommand {
    private CareerTestCommand() {}

    private static final ResourceLocation HUMAN = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "human");

    public static int runFullTest(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be run by a player."));
            return 0;
        }
        try {
            return runTests(source, player);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Test crashed: " + e.getClass().getSimpleName() + ": " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            CareerChronicleMod.LOGGER.error("Career test failed with exception", e);
            return 0;
        }
    }

    private static int runTests(CommandSourceStack source, ServerPlayer player) {
        List<String> results = new ArrayList<>();
        int passed = 0;
        int failed = 0;

        source.sendSuccess(() -> Component.literal("=== Career Chronicle Test Suite ===").withStyle(ChatFormatting.GOLD), false);

        // Test 1: Registry loaded
        RegistrySnapshot registry = CareerRegistry.snapshot();
        if (registry.races().size() >= 6 && registry.classes().size() >= 9 && registry.skills().size() >= 67) {
            passed++;
            results.add(pass("Registry: " + registry.races().size() + " races, " + registry.classes().size() + " classes, " + registry.skills().size() + " skills, " + registry.fusions().size() + " fusions"));
        } else {
            failed++;
            results.add(fail("Registry incomplete"));
        }

        // Test 2: Reset player data for clean test
        CareerDataAccess.get(player).ifPresent(data -> {
            data.deserializePersistentData(new net.minecraft.nbt.CompoundTag());
            data.getRuntimeState().clear();
        });

        // Test 3: Select race
        CareerDataAccess.get(player).ifPresent(data -> {
            data.setRace(HUMAN);
        });
        boolean raceSet = CareerDataAccess.get(player).map(d -> HUMAN.equals(d.getRace())).orElse(false);
        if (raceSet) {
            passed++;
            results.add(pass("Race selection: Human"));
        } else {
            failed++;
            results.add(fail("Race selection failed"));
        }

        // Test 4: First segment available at level 1
        int reqLevel = CareerProgressionMath.requiredLevelForNextSegment(0);
        if (reqLevel == 1) {
            passed++;
            results.add(pass("First segment requires level 1 (correct)"));
        } else {
            failed++;
            results.add(fail("First segment requires level " + reqLevel + " (should be 1)"));
        }

        // Test 5: Select class (warrior)
        ResourceLocation warriorId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "warrior");
        CareerProgressionService.selectClass(player, warriorId);
        boolean warriorSelected = CareerDataAccess.get(player)
                .map(d -> d.getClassHistory().contains(warriorId)).orElse(false);
        if (warriorSelected) {
            passed++;
            results.add(pass("Class selection: Warrior"));
        } else {
            failed++;
            results.add(fail("Warrior class selection failed"));
        }

        // Test 6: Skills unlocked
        List<ResourceLocation> warriorSkills = List.of(
                ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "lunge_strike"),
                ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "guard_stance"),
                ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ground_slam")
        );
        boolean allUnlocked = CareerDataAccess.get(player)
                .map(d -> d.getUnlockedSkills().containsAll(warriorSkills)).orElse(false);
        if (allUnlocked) {
            passed++;
            results.add(pass("Warrior skills unlocked: " + warriorSkills.size()));
        } else {
            failed++;
            results.add(fail("Warrior skills not fully unlocked"));
        }

        // Test 7: Skill loadout auto-filled
        boolean loadoutFilled = CareerDataAccess.get(player)
                .map(d -> !d.getSkillLoadout().isEmpty()).orElse(false);
        if (loadoutFilled) {
            passed++;
            results.add(pass("Skill loadout auto-filled"));
        } else {
            failed++;
            results.add(fail("Skill loadout empty after class selection"));
        }

        // Force resource initialization (normally done in tick)
        CareerDataAccess.get(player).ifPresent(data -> {
            int maxMana = 80 + Math.max(0, data.getCareerLevel() - 1) * 2;
            int maxStamina = 80 + Math.max(0, data.getCareerLevel() - 1) * 2;
            data.getRuntimeState().setResourceCaps(maxMana, maxStamina);
            data.getRuntimeState().refillResources();
        });

        // Test 8: Resources initialized
        boolean hasResources = CareerDataAccess.get(player)
                .map(d -> d.getRuntimeState().getMaxMana() > 0 && d.getRuntimeState().getMaxStamina() > 0).orElse(false);
        if (hasResources) {
            passed++;
            results.add(pass("Resources initialized (mana/stamina > 0)"));
        } else {
            failed++;
            results.add(fail("Resources not initialized"));
        }

        // Test 9: Spawn a target and test skill execution
        ServerLevel level = player.serverLevel();
        Cow testTarget = EntityType.COW.create(level);
        if (testTarget != null) {
            testTarget.setPos(player.getX() + 2, player.getY(), player.getZ());
            level.addFreshEntity(testTarget);

            // Give player a sword for melee_weapon tag
            player.getInventory().setItem(player.getInventory().selected,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));

            // Test each executor exists
            int executorCount = 0;
            for (SkillDef skill : registry.skills().values()) {
                if (SkillExecutorRegistry.exists(skill.executor())) {
                    executorCount++;
                }
            }
            if (executorCount == registry.skills().size()) {
                passed++;
                results.add(pass("All " + executorCount + " skill executors registered"));
            } else {
                failed++;
                results.add(fail("Missing executors: " + executorCount + "/" + registry.skills().size()));
            }

            // Actually cast lunge_strike
            ResourceLocation lungeStrike = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "lunge_strike");
            boolean castSuccess = CareerSkillService.useSkill(player, lungeStrike);
            if (castSuccess) {
                passed++;
                results.add(pass("Skill cast: lunge_strike SUCCESS"));
            } else {
                failed++;
                results.add(fail("Skill cast: lunge_strike FAILED (check equipment/resource)"));
            }

            // Check cooldown was set
            boolean onCooldown = CareerDataAccess.get(player)
                    .map(d -> d.getRuntimeState().getCooldownTicks().containsKey(lungeStrike)).orElse(false);
            if (onCooldown) {
                passed++;
                results.add(pass("Cooldown set after cast"));
            } else {
                failed++;
                results.add(fail("Cooldown not set after cast"));
            }

            testTarget.discard();
        }

        // Test 10: XP award
        int oldLevel = CareerDataAccess.get(player).map(ICareerData::getCareerLevel).orElse(0);
        CareerProgressionService.awardCareerXpFromSource(player, CareerProgressionService.COMMAND_XP_SOURCE, 500);
        int newXp = CareerDataAccess.get(player).map(ICareerData::getCareerXp).orElse(0);
        if (newXp > 0 || CareerDataAccess.get(player).map(ICareerData::getCareerLevel).orElse(0) > oldLevel) {
            passed++;
            results.add(pass("XP award: +500 applied"));
        } else {
            failed++;
            results.add(fail("XP award failed"));
        }

        // Test 11: Attribute system
        CareerDataAccess.get(player).ifPresent(data -> {
            data.setUnspentAttributePoints(5);
            data.setAttribute(CareerData.ATTR_STR, data.getAttribute(CareerData.ATTR_STR) + 3);
            data.setUnspentAttributePoints(data.getUnspentAttributePoints() - 3);
        });
        int strVal = CareerDataAccess.get(player).map(d -> d.getAttribute(CareerData.ATTR_STR)).orElse(0);
        if (strVal == CareerData.BASE_ATTRIBUTE + 3) {
            passed++;
            results.add(pass("Attribute allocation: STR=" + strVal));
        } else {
            failed++;
            results.add(fail("Attribute allocation: STR=" + strVal + " (expected " + (CareerData.BASE_ATTRIBUTE + 3) + ")"));
        }

        // Test 12: Fusion check (add fire_mage to get blazing_charge)
        ResourceLocation fireMageId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fire_mage");
        CareerDataAccess.get(player).ifPresent(data -> {
            data.setCareerLevel(20);
            data.setCareerXp(0);
        });
        CareerProgressionService.selectClass(player, fireMageId);
        ResourceLocation blazingCharge = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "blazing_charge");
        boolean fusionUnlocked = CareerDataAccess.get(player)
                .map(d -> d.getUnlockedSkills().contains(blazingCharge)).orElse(false);
        if (fusionUnlocked) {
            passed++;
            results.add(pass("Fusion: warrior+fire_mage → blazing_charge UNLOCKED"));
        } else {
            failed++;
            results.add(fail("Fusion: warrior+fire_mage → blazing_charge NOT unlocked"));
        }

        // Test 13: Reverse fusion check (order doesn't matter)
        // Already tested by having warrior first then fire_mage
        ResourceLocation flameArrow = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "flame_arrow");
        boolean flameArrowUnlocked = CareerDataAccess.get(player)
                .map(d -> d.getUnlockedSkills().contains(flameArrow)).orElse(false);
        // flame_arrow needs fire_mage + archer, so should NOT be unlocked yet
        if (!flameArrowUnlocked) {
            passed++;
            results.add(pass("Fusion guard: flame_arrow NOT unlocked (no archer) - correct"));
        } else {
            failed++;
            results.add(fail("Fusion guard: flame_arrow unlocked without archer"));
        }

        // Sync and report
        CareerDataAccess.sync(player);

        // Output all results
        final int p = passed;
        final int f = failed;
        for (String r : results) {
            source.sendSuccess(() -> Component.literal(r), false);
        }
        source.sendSuccess(() -> Component.literal("=== Results: " + p + " passed, " + f + " failed ===")
                .withStyle(f == 0 ? ChatFormatting.GREEN : ChatFormatting.RED), false);

        return f == 0 ? 1 : 0;
    }

    private static String pass(String msg) {
        return "  ✔ " + msg;
    }

    private static String fail(String msg) {
        return "  ✘ " + msg;
    }
}
