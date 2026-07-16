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
import net.minecraft.nbt.CompoundTag;
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

        // Bug9 fix: runTests() below is a destructive self-test suite -- it
        // repeatedly wipes and rebuilds the invoking player's REAL, live
        // Career Chronicle progression (data.deserializePersistentData(new
        // CompoundTag()) used as a "reset" between sub-tests, several times)
        // with no isolation from the player's actual character. Real dynamic
        // testing traced the reported "unlockedSkills lost after death and
        // respawn" symptom to THIS command, not to PlayerEvent.Clone/
        // PlayerRespawnEvent timing (which was verified correct: a diagnostic
        // build showed classHistory/unlockedSkills fully intact immediately
        // after respawn). AutoTestScenarios runs `/career test` on the same
        // live player shortly after its death/respawn check; Test 9's "check
        // every skill has an executor" loop used to call
        // SkillExecutorRegistry.exists(null) for every non-executor skill
        // (post-0.4-05a most skills legitimately have no executor) which threw
        // an NPE, aborting runTests() mid-way and leaving whatever partial
        // wiped/rebuilt state the test was in (just the first test class
        // selected) as the player's new *permanent* data -- with no crash at
        // all, it still would have ended the test on Test 20's leftover state,
        // which is just as wrong for a "test" command to leave behind.
        // Snapshotting before and restoring after -- unconditionally, whether
        // the test passes, fails, or throws -- makes this command safe to run
        // against a live character.
        CompoundTag preTestBackup = CareerDataAccess.get(player).map(ICareerData::serializePersistentData).orElse(null);
        try {
            return runTests(source, player);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Test crashed: " + e.getClass().getSimpleName() + ": " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            CareerChronicleMod.LOGGER.error("Career test failed with exception", e);
            return 0;
        } finally {
            restorePlayerData(player, preTestBackup);
        }
    }

    private static void restorePlayerData(ServerPlayer player, CompoundTag preTestBackup) {
        if (preTestBackup == null) {
            return;
        }
        CareerDataAccess.get(player).ifPresent(data -> {
            data.deserializePersistentData(preTestBackup);
            int maxMana = 80 + Math.max(0, data.getCareerLevel() - 1) * 2;
            int maxStamina = 80 + Math.max(0, data.getCareerLevel() - 1) * 2;
            data.getRuntimeState().clear();
            data.getRuntimeState().setResourceCaps(maxMana, maxStamina);
            data.getRuntimeState().refillResources();
        });
        CareerDataAccess.sync(player);
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

            // Test each *executor-based* skill is registered. Post-0.4-05a most
            // skills run through the effect-component interpreter and have a
            // null executor() by design -- only skills that declare a
            // non-null executor() are expected to resolve via
            // SkillExecutorRegistry, so only those are counted here (the old
            // "must equal registry.skills().size()" check assumed every skill
            // needed a legacy executor, which stopped being true after the FX
            // pipeline refactor and made this loop call exists(null) for most
            // skills -- SkillExecutorRegistry.exists() is now null-safe too,
            // see its own fix note, but the check itself is corrected here so
            // it reflects the current architecture instead of always failing).
            int expectedExecutorSkills = 0;
            int executorCount = 0;
            for (SkillDef skill : registry.skills().values()) {
                if (skill.executor() == null) {
                    continue;
                }
                expectedExecutorSkills++;
                if (SkillExecutorRegistry.exists(skill.executor())) {
                    executorCount++;
                }
            }
            if (executorCount == expectedExecutorSkills) {
                passed++;
                results.add(pass("All " + executorCount + " executor-based skills registered"));
            } else {
                failed++;
                results.add(fail("Missing executors: " + executorCount + "/" + expectedExecutorSkills));
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

        // Test 14: Equipment requirement — cast without required weapon
        CareerDataAccess.get(player).ifPresent(data -> {
            data.deserializePersistentData(new net.minecraft.nbt.CompoundTag());
            data.getRuntimeState().clear();
        });
        CareerDataAccess.get(player).ifPresent(data -> data.setRace(HUMAN));
        CareerProgressionService.selectClass(player, warriorId);
        CareerDataAccess.get(player).ifPresent(data -> {
            int maxM = 80 + Math.max(0, data.getCareerLevel() - 1) * 2;
            int maxS = 80 + Math.max(0, data.getCareerLevel() - 1) * 2;
            data.getRuntimeState().setResourceCaps(maxM, maxS);
            data.getRuntimeState().refillResources();
        });
        player.getInventory().clearContent();
        ResourceLocation lungeStrikeT14 = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "lunge_strike");
        boolean castNoWeapon = CareerSkillService.useSkill(player, lungeStrikeT14);
        player.getInventory().setItem(player.getInventory().selected,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        boolean castWithWeapon = CareerSkillService.useSkill(player, lungeStrikeT14);
        if (!castNoWeapon && castWithWeapon) {
            passed++;
            results.add(pass("Equipment gate: lunge_strike blocked without weapon, succeeded with iron_sword"));
        } else {
            failed++;
            results.add(fail("Equipment gate: without=" + castNoWeapon + " with=" + castWithWeapon));
        }

        // Test 15: Resource cost enforcement — cast without sufficient mana
        CareerDataAccess.get(player).ifPresent(data -> {
            data.deserializePersistentData(new net.minecraft.nbt.CompoundTag());
            data.getRuntimeState().clear();
        });
        CareerDataAccess.get(player).ifPresent(data -> data.setRace(HUMAN));
        ResourceLocation fireMageT15 = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fire_mage");
        CareerProgressionService.selectClass(player, fireMageT15);
        CareerDataAccess.get(player).ifPresent(data -> {
            int maxM = 80 + Math.max(0, data.getCareerLevel() - 1) * 2;
            int maxS = 80 + Math.max(0, data.getCareerLevel() - 1) * 2;
            data.getRuntimeState().setResourceCaps(maxM, maxS);
            data.getRuntimeState().refillResources();
        });
        player.getInventory().setItem(player.getInventory().selected,
                new net.minecraft.world.item.ItemStack(com.hongyuwu.careerchronicle.registry.CareerItems.EMBER_STAFF.get()));
        ResourceLocation fireballT15 = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fireball");
        CareerDataAccess.get(player).ifPresent(data -> {
            data.getRuntimeState().consumeResource("mana", data.getRuntimeState().getMaxMana());
        });
        boolean castNoMana = CareerSkillService.useSkill(player, fireballT15);
        CareerDataAccess.get(player).ifPresent(data -> data.getRuntimeState().refillResources());
        boolean castWithMana = CareerSkillService.useSkill(player, fireballT15);
        if (!castNoMana && castWithMana) {
            passed++;
            results.add(pass("Resource gate: fireball blocked without mana, succeeded with mana"));
        } else {
            failed++;
            results.add(fail("Resource gate: withoutMana=" + castNoMana + " withMana=" + castWithMana));
        }

        // Test 16: Cooldown enforcement
        boolean castOnCooldown = CareerSkillService.useSkill(player, fireballT15);
        CareerDataAccess.get(player).ifPresent(data -> {
            data.getRuntimeState().setCooldown(fireballT15, 0);
            data.getRuntimeState().refillResources();
        });
        boolean castCooldownCleared = CareerSkillService.useSkill(player, fireballT15);
        if (!castOnCooldown && castCooldownCleared) {
            passed++;
            results.add(pass("Cooldown gate: fireball blocked on cooldown, succeeded after clearing"));
        } else {
            failed++;
            results.add(fail("Cooldown gate: onCooldown=" + castOnCooldown + " afterClear=" + castCooldownCleared));
        }

        // Test 17: All 8 base classes repeat rewards at count=2
        String[][] repeatRewardTests = {
            {"warrior", "iron_vanguard"},
            {"fire_mage", "inferno_focus"},
            {"archer", "eagle_eye"},
            {"priest", "seraphic_grace"},
            {"ice_mage", "glacial_prison", "int", "12", "wis", "8"},
            {"necromancer", "death_coil", "int", "12", "con", "8"},
            {"rogue", "assassin_mark", "dex", "12", "str", "8"},
            {"guardian", "fortified_bulwark", "con", "12", "str", "8"},
        };
        boolean allRepeatPassed = true;
        StringBuilder repeatFailures = new StringBuilder();
        for (String[] entry : repeatRewardTests) {
            String classPath = entry[0];
            String rewardPath = entry[1];
            ResourceLocation classIdR = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, classPath);
            ResourceLocation rewardSkillR = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, rewardPath);
            CareerDataAccess.get(player).ifPresent(data -> {
                data.deserializePersistentData(new net.minecraft.nbt.CompoundTag());
                data.getRuntimeState().clear();
            });
            CareerDataAccess.get(player).ifPresent(data -> {
                data.setRace(HUMAN);
                data.setCareerLevel(20);
                if (entry.length > 2) {
                    for (int i = 2; i < entry.length; i += 2) {
                        data.setAttribute(entry[i], Integer.parseInt(entry[i + 1]));
                    }
                }
            });
            CareerProgressionService.selectClass(player, classIdR);
            CareerProgressionService.selectClass(player, classIdR);
            boolean hasReward = CareerDataAccess.get(player)
                    .map(d -> d.getUnlockedSkills().contains(rewardSkillR)).orElse(false);
            if (!hasReward) {
                allRepeatPassed = false;
                repeatFailures.append(classPath).append(" ");
            }
        }
        if (allRepeatPassed) {
            passed++;
            results.add(pass("Repeat rewards: all 8 base classes x2 unlocked correctly"));
        } else {
            failed++;
            results.add(fail("Repeat rewards failed for: " + repeatFailures.toString().trim()));
        }

        // Test 18: Hidden unlock — lich_clue
        CareerDataAccess.get(player).ifPresent(data -> {
            data.deserializePersistentData(new net.minecraft.nbt.CompoundTag());
            data.getRuntimeState().clear();
        });
        CareerDataAccess.get(player).ifPresent(data -> {
            data.setRace(HUMAN);
            data.setCareerLevel(50);
            data.setAttribute(CareerData.ATTR_INT, 12);
            data.setAttribute(CareerData.ATTR_CON, 8);
            data.setAttribute(CareerData.ATTR_WIS, 8);
        });
        ResourceLocation necromancerT18 = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "necromancer");
        ResourceLocation iceMageT18 = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ice_mage");
        CareerProgressionService.selectClass(player, necromancerT18);
        CareerProgressionService.selectClass(player, necromancerT18);
        CareerProgressionService.selectClass(player, iceMageT18);
        ResourceLocation lichClueFlag = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "lich_clue");
        boolean lichClueRevealed = CareerDataAccess.get(player)
                .map(d -> d.getHiddenFlags().contains(lichClueFlag)).orElse(false);
        if (lichClueRevealed) {
            passed++;
            results.add(pass("Hidden unlock: lich_clue revealed (necromancer x2 + ice_mage x1)"));
        } else {
            failed++;
            results.add(fail("Hidden unlock: lich_clue NOT revealed"));
        }

        // Test 19: Class attribute requirement gate
        CareerDataAccess.get(player).ifPresent(data -> {
            data.deserializePersistentData(new net.minecraft.nbt.CompoundTag());
            data.getRuntimeState().clear();
        });
        CareerDataAccess.get(player).ifPresent(data -> data.setRace(HUMAN));
        ResourceLocation iceMageT19 = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ice_mage");
        CareerProgressionService.selectClass(player, iceMageT19);
        boolean iceMageBlocked = CareerDataAccess.get(player)
                .map(d -> !d.getClassHistory().contains(iceMageT19)).orElse(false);
        CareerDataAccess.get(player).ifPresent(data -> {
            data.setAttribute(CareerData.ATTR_INT, 12);
            data.setAttribute(CareerData.ATTR_WIS, 8);
        });
        CareerProgressionService.selectClass(player, iceMageT19);
        boolean iceMageAllowed = CareerDataAccess.get(player)
                .map(d -> d.getClassHistory().contains(iceMageT19)).orElse(false);
        if (iceMageBlocked && iceMageAllowed) {
            passed++;
            results.add(pass("Attribute gate: ice_mage blocked with default attrs, allowed with int=12/wis=8"));
        } else {
            failed++;
            results.add(fail("Attribute gate: blocked=" + iceMageBlocked + " allowed=" + iceMageAllowed));
        }

        // Test 20: Race class restriction
        CareerDataAccess.get(player).ifPresent(data -> {
            data.deserializePersistentData(new net.minecraft.nbt.CompoundTag());
            data.getRuntimeState().clear();
        });
        ResourceLocation elfRace = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "elf");
        ResourceLocation necromancerT20 = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "necromancer");
        CareerDataAccess.get(player).ifPresent(data -> {
            data.setRace(elfRace);
            data.setAttribute(CareerData.ATTR_INT, 12);
            data.setAttribute(CareerData.ATTR_CON, 8);
        });
        CareerProgressionService.selectClass(player, necromancerT20);
        boolean necroBlockedByElf = CareerDataAccess.get(player)
                .map(d -> !d.getClassHistory().contains(necromancerT20)).orElse(false);
        CareerDataAccess.get(player).ifPresent(data -> {
            data.deserializePersistentData(new net.minecraft.nbt.CompoundTag());
            data.getRuntimeState().clear();
        });
        CareerDataAccess.get(player).ifPresent(data -> {
            data.setRace(HUMAN);
            data.setAttribute(CareerData.ATTR_INT, 12);
            data.setAttribute(CareerData.ATTR_CON, 8);
        });
        CareerProgressionService.selectClass(player, necromancerT20);
        boolean necroAllowedByHuman = CareerDataAccess.get(player)
                .map(d -> d.getClassHistory().contains(necromancerT20)).orElse(false);
        if (necroBlockedByElf && necroAllowedByHuman) {
            passed++;
            results.add(pass("Race restriction: elf blocked necromancer, human allowed necromancer"));
        } else {
            failed++;
            results.add(fail("Race restriction: elfBlocked=" + necroBlockedByElf + " humanAllowed=" + necroAllowedByHuman));
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
