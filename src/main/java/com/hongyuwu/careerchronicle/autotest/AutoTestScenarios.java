package com.hongyuwu.careerchronicle.autotest;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.client.CareerClientScreens;
import com.hongyuwu.careerchronicle.client.ClientCareerData;
import com.hongyuwu.careerchronicle.client.RaceSelectionScreen;
import com.hongyuwu.careerchronicle.network.C2SAllocateAttributePacket;
import com.hongyuwu.careerchronicle.network.C2SSelectClassPacket;
import com.hongyuwu.careerchronicle.network.C2SSelectRacePacket;
import com.hongyuwu.careerchronicle.network.C2SSetSkillLoadoutPacket;
import com.hongyuwu.careerchronicle.network.C2SUseSkillPacket;
import com.hongyuwu.careerchronicle.network.NetworkHandler;
import com.hongyuwu.careerchronicle.player.CareerData;
import com.hongyuwu.careerchronicle.player.CareerDataNbt;
import com.hongyuwu.careerchronicle.player.CareerDataSnapshot;
import com.hongyuwu.careerchronicle.skill.CareerLoadoutService;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public final class AutoTestScenarios {
    private static final String NS = CareerChronicleMod.MOD_ID;

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NS, path);
    }

    private AutoTestScenarios() {}

    public static void buildFullFlowSteps(List<AutoTestStep> steps) {
        // Phase 0: Initial state & world screenshot
        steps.add(AutoTestStep.wait("world_load", 60));
        steps.add(AutoTestStep.screenshot("00_world_loaded"));

        // Phase 1: Reset player data for clean test
        steps.add(AutoTestStep.command("career reset", 10));
        steps.add(AutoTestStep.wait("reset_sync", 30));

        // Phase 2: Open career screen → should redirect to race selection
        steps.add(AutoTestStep.verified("race_screen_opens", 20,
                ctrl -> CareerClientScreens.openEntryScreen(),
                () -> {
                    Screen screen = Minecraft.getInstance().screen;
                    if (screen instanceof RaceSelectionScreen) return null;
                    return "Expected RaceSelectionScreen, got " + (screen == null ? "null" : screen.getClass().getSimpleName());
                }));
        steps.add(AutoTestStep.screenshot("01_race_selection_screen"));

        // Phase 3: Select human race via network packet
        steps.add(AutoTestStep.action("select_race_human", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSelectRacePacket(id("human")))));
        steps.add(AutoTestStep.wait("race_sync", 30));
        steps.add(AutoTestStep.verified("race_selected", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (id("human").equals(snap.race())) return null;
                    return "Race not set. Current: " + snap.race();
                }));
        steps.add(AutoTestStep.screenshot("02_race_selected"));

        // Phase 4: Close race screen, verify career screen opens
        steps.add(AutoTestStep.action("close_race_screen", 5,
                ctrl -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(null))));
        steps.add(AutoTestStep.wait("screen_close", 10));

        // Phase 5: Select first class — warrior (segment 1 at level 1)
        steps.add(AutoTestStep.action("select_class_warrior", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSelectClassPacket(id("warrior")))));
        steps.add(AutoTestStep.wait("class_sync", 30));
        steps.add(AutoTestStep.verified("warrior_selected", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.classHistory().contains(id("warrior"))) return null;
                    return "Warrior not in class history. History: " + snap.classHistory();
                }));

        // Phase 6: Verify skills unlocked after warrior selection
        steps.add(AutoTestStep.verified("warrior_skills_unlocked", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.unlockedSkills().isEmpty()) return "No skills unlocked";
                    return null;
                }));

        // Phase 7: Open career screen and screenshot
        steps.add(AutoTestStep.action("open_career_screen", 20,
                ctrl -> Minecraft.getInstance().execute(CareerClientScreens::openEntryScreen)));
        steps.add(AutoTestStep.screenshot("03_career_screen_after_warrior"));
        steps.add(AutoTestStep.action("close_career_screen", 5,
                ctrl -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(null))));

        // Phase 8: Equip skill to slot Z
        steps.add(AutoTestStep.action("equip_skill_slot_z", 5,
                ctrl -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (!snap.unlockedSkills().isEmpty()) {
                        ResourceLocation firstSkill = snap.unlockedSkills().iterator().next();
                        NetworkHandler.CHANNEL.sendToServer(new C2SSetSkillLoadoutPacket(
                                CareerLoadoutService.SLOT_TYPE_ACTIVE, 0, firstSkill));
                    }
                }));
        steps.add(AutoTestStep.wait("loadout_sync", 20));
        steps.add(AutoTestStep.verified("skill_equipped", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (!snap.skillLoadout().isEmpty() && snap.skillLoadout().get(0) != null) return null;
                    return "Skill not equipped in slot 0";
                }));
        steps.add(AutoTestStep.screenshot("04_hud_skill_equipped"));

        // Phase 9: Give player a sword and try casting the skill
        steps.add(AutoTestStep.command("give @s iron_sword", 10));
        steps.add(AutoTestStep.wait("equip_sword", 10));
        steps.add(AutoTestStep.action("cast_skill_z", 5,
                ctrl -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (!snap.skillLoadout().isEmpty() && snap.skillLoadout().get(0) != null) {
                        NetworkHandler.CHANNEL.sendToServer(new C2SUseSkillPacket(snap.skillLoadout().get(0)));
                    }
                }));
        steps.add(AutoTestStep.wait("cast_sync", 40));
        steps.add(AutoTestStep.screenshot("05_after_skill_cast"));

        // Phase 10: Verify cooldown appeared
        steps.add(AutoTestStep.verified("cooldown_visible", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (!snap.skillLoadout().isEmpty() && snap.skillLoadout().get(0) != null) {
                        if (ClientCareerData.cooldown(snap.skillLoadout().get(0)) > 0) return null;
                    }
                    return "No cooldown detected after cast";
                }));

        // Phase 11: Add XP and verify level up
        steps.add(AutoTestStep.command("career add-xp 5000", 20));
        steps.add(AutoTestStep.wait("xp_sync", 20));
        steps.add(AutoTestStep.verified("xp_bar_updated", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.careerLevel() > 1 || snap.careerXp() > 0) return null;
                    return "XP not applied. Level=" + snap.careerLevel() + " XP=" + snap.careerXp();
                }));
        steps.add(AutoTestStep.screenshot("06_after_xp_gain"));

        // Phase 12: Level to 10 for second segment
        steps.add(AutoTestStep.command("career add-xp 50000", 20));
        steps.add(AutoTestStep.wait("level_sync", 30));
        steps.add(AutoTestStep.verified("reached_level_10", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.careerLevel() >= 10) return null;
                    return "Level not reached 10. Current: " + snap.careerLevel();
                }));
        steps.add(AutoTestStep.screenshot("07_level_10"));

        // Phase 13: Select second class — fire_mage for fusion test
        steps.add(AutoTestStep.action("select_class_fire_mage", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSelectClassPacket(id("fire_mage")))));
        steps.add(AutoTestStep.wait("class2_sync", 30));
        steps.add(AutoTestStep.verified("fire_mage_selected", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.classHistory().contains(id("fire_mage"))) return null;
                    return "fire_mage not in history: " + snap.classHistory();
                }));

        // Phase 14: Verify fusion unlocked (warrior + fire_mage = blazing_charge)
        steps.add(AutoTestStep.verified("fusion_blazing_charge", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.unlockedSkills().contains(id("blazing_charge"))) return null;
                    return "blazing_charge not unlocked. Skills: " + snap.unlockedSkills();
                }));
        steps.add(AutoTestStep.screenshot("08_fusion_unlocked"));

        // Phase 15: Open career screen to show expanded skill list
        steps.add(AutoTestStep.action("open_career_after_fusion", 20,
                ctrl -> Minecraft.getInstance().execute(CareerClientScreens::openEntryScreen)));
        steps.add(AutoTestStep.screenshot("09_career_screen_fusion"));
        steps.add(AutoTestStep.action("close_career", 5,
                ctrl -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(null))));

        // Phase 16: Level to 20 for third segment
        steps.add(AutoTestStep.command("career add-xp 200000", 20));
        steps.add(AutoTestStep.wait("level20_sync", 30));
        steps.add(AutoTestStep.verified("reached_level_20", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.careerLevel() >= 20) return null;
                    return "Level not reached 20. Current: " + snap.careerLevel();
                }));
        steps.add(AutoTestStep.screenshot("10_level_20"));

        // Phase 17: Select third class — archer for more fusions
        steps.add(AutoTestStep.action("select_class_archer", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSelectClassPacket(id("archer")))));
        steps.add(AutoTestStep.wait("class3_sync", 30));

        // Phase 18: Verify flame_arrow fusion (fire_mage + archer)
        steps.add(AutoTestStep.verified("fusion_flame_arrow", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.unlockedSkills().contains(id("flame_arrow"))) return null;
                    return "flame_arrow not unlocked. Skills: " + snap.unlockedSkills();
                }));
        steps.add(AutoTestStep.screenshot("11_flame_arrow_fusion"));

        // Phase 19: Level to 30 for fourth segment
        steps.add(AutoTestStep.command("career add-xp 500000", 20));
        steps.add(AutoTestStep.wait("level30_sync", 30));
        steps.add(AutoTestStep.verified("reached_level_30", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.careerLevel() >= 30) return null;
                    return "Level not reached 30. Current: " + snap.careerLevel();
                }));
        steps.add(AutoTestStep.screenshot("12_level_30"));

        // Phase 20: Equip multiple skills and screenshot HUD
        steps.add(AutoTestStep.action("equip_4_skills", 5,
                ctrl -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    int slot = 0;
                    for (ResourceLocation skill : snap.unlockedSkills()) {
                        if (slot >= 4) break;
                        NetworkHandler.CHANNEL.sendToServer(new C2SSetSkillLoadoutPacket(
                                CareerLoadoutService.SLOT_TYPE_ACTIVE, slot, skill));
                        slot++;
                    }
                }));
        steps.add(AutoTestStep.wait("loadout4_sync", 30));
        steps.add(AutoTestStep.screenshot("13_hud_4_skills"));

        // Phase 21: Cast all 4 skills in sequence for cooldown display test
        for (int i = 0; i < 4; i++) {
            final int slotIdx = i;
            steps.add(AutoTestStep.action("cast_slot_" + i, 10,
                    ctrl -> {
                        CareerDataSnapshot snap = ClientCareerData.snapshot();
                        if (snap.skillLoadout().size() > slotIdx && snap.skillLoadout().get(slotIdx) != null) {
                            NetworkHandler.CHANNEL.sendToServer(new C2SUseSkillPacket(snap.skillLoadout().get(slotIdx)));
                        }
                    }));
        }
        steps.add(AutoTestStep.wait("cast_all_sync", 10));
        steps.add(AutoTestStep.screenshot("14_all_cooldowns"));

        // Phase 22: GUI Scale tests
        for (int scale = 2; scale <= 4; scale++) {
            final int s = scale;
            steps.add(AutoTestStep.action("gui_scale_" + s, 10,
                    ctrl -> Minecraft.getInstance().execute(() ->
                            Minecraft.getInstance().options.guiScale().set(s))));
            steps.add(AutoTestStep.wait("gui_scale_apply_" + s, 10));
            steps.add(AutoTestStep.screenshot("15_gui_scale_" + s));
        }
        steps.add(AutoTestStep.action("gui_scale_reset", 5,
                ctrl -> Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().options.guiScale().set(2))));

        // Phase 23: Resource bar test (cast skills to drain mana/stamina)
        steps.add(AutoTestStep.verified("resource_bars_visible", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.maxMana() > 0 || snap.maxStamina() > 0) return null;
                    return "Resources not initialized. maxMana=" + snap.maxMana() + " maxStamina=" + snap.maxStamina();
                }));
        steps.add(AutoTestStep.screenshot("16_resource_bars"));

        // Phase 24: Level to 50 for full 5 segments
        steps.add(AutoTestStep.command("career add-xp 2000000", 20));
        steps.add(AutoTestStep.wait("level50_sync", 40));

        // Select class for segment 4 (priest)
        steps.add(AutoTestStep.action("select_class_priest", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSelectClassPacket(id("priest")))));
        steps.add(AutoTestStep.wait("class4_sync", 40));
        steps.add(AutoTestStep.verified("priest_selected", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.classHistory().contains(id("priest"))) return null;
                    return "priest not in history: " + snap.classHistory();
                }));
        // After 4th class selected, level cap moves to 50 — add more XP
        steps.add(AutoTestStep.command("career add-xp 5000000", 20));
        steps.add(AutoTestStep.wait("level50_sync2", 40));
        steps.add(AutoTestStep.verified("reached_level_50", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.careerLevel() >= 50) return null;
                    return "Level not 50. Current: " + snap.careerLevel();
                }));
        // Allocate attributes for ice_mage (requires int >= 12, wis >= 8; base is 5)
        steps.add(AutoTestStep.action("allocate_int_7", 5,
                ctrl -> {
                    for (int i = 0; i < 7; i++) {
                        NetworkHandler.CHANNEL.sendToServer(new C2SAllocateAttributePacket(CareerData.ATTR_INT));
                    }
                }));
        steps.add(AutoTestStep.wait("int_sync", 10));
        steps.add(AutoTestStep.action("allocate_wis_3", 5,
                ctrl -> {
                    for (int i = 0; i < 3; i++) {
                        NetworkHandler.CHANNEL.sendToServer(new C2SAllocateAttributePacket(CareerData.ATTR_WIS));
                    }
                }));
        steps.add(AutoTestStep.wait("attr_sync", 20));
        // Select class for segment 5 (ice_mage)
        steps.add(AutoTestStep.action("select_class_ice_mage", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSelectClassPacket(id("ice_mage")))));
        steps.add(AutoTestStep.wait("class5_sync", 40));

        steps.add(AutoTestStep.verified("reached_5_segments", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.classHistory().size() >= 5) return null;
                    return "Not 5 segments. History: " + snap.classHistory();
                }));
        steps.add(AutoTestStep.screenshot("17_level_50_full_build"));

        // Phase 25: Open career screen for full build view
        steps.add(AutoTestStep.action("open_final_career", 20,
                ctrl -> Minecraft.getInstance().execute(CareerClientScreens::openEntryScreen)));
        steps.add(AutoTestStep.screenshot("18_final_career_screen"));
        steps.add(AutoTestStep.action("close_final", 5,
                ctrl -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(null))));

        // Phase 26: Give player ember staff and test fire skill
        steps.add(AutoTestStep.command("give @s careerchronicle:ember_staff", 10));
        steps.add(AutoTestStep.wait("staff_sync", 10));
        steps.add(AutoTestStep.action("equip_fireball", 5,
                ctrl -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    ResourceLocation fireball = id("fireball");
                    if (snap.unlockedSkills().contains(fireball)) {
                        NetworkHandler.CHANNEL.sendToServer(new C2SSetSkillLoadoutPacket(
                                CareerLoadoutService.SLOT_TYPE_ACTIVE, 0, fireball));
                    }
                }));
        steps.add(AutoTestStep.wait("equip_fireball_sync", 20));
        steps.add(AutoTestStep.screenshot("19_with_ember_staff"));

        // Phase 27: Give player bow and test archer skills
        steps.add(AutoTestStep.command("give @s careerchronicle:chronicle_recurve", 10));
        steps.add(AutoTestStep.wait("bow_sync", 10));
        steps.add(AutoTestStep.screenshot("20_with_bow"));

        // Phase 28: Death and respawn persistence test
        steps.add(AutoTestStep.action("record_pre_death_state", 5,
                ctrl -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    ctrl.recordResult("pre_death_state",true,
                            "Level=" + snap.careerLevel() +
                            " Classes=" + snap.classHistory().size() +
                            " Skills=" + snap.unlockedSkills().size());
                }));
        steps.add(AutoTestStep.command("kill @s", 20));
        steps.add(AutoTestStep.wait("death_screen_appear", 40));
        steps.add(AutoTestStep.screenshot("21_death_screen"));
        // Click the Respawn button on the death screen
        steps.add(AutoTestStep.action("click_respawn", 10,
                ctrl -> Minecraft.getInstance().execute(() -> {
                    Screen screen = Minecraft.getInstance().screen;
                    if (screen instanceof DeathScreen) {
                        for (var child : screen.children()) {
                            if (child instanceof Button button) {
                                button.onPress();
                                break;
                            }
                        }
                    }
                })));
        steps.add(AutoTestStep.wait("respawn_sync", 60));
        steps.add(AutoTestStep.screenshot("22_after_respawn"));
        steps.add(AutoTestStep.verified("data_persisted_after_death", 10,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.classHistory().size() >= 5 && !snap.unlockedSkills().isEmpty()) return null;
                    return "Data lost after death! Classes=" + snap.classHistory().size() +
                           " Skills=" + snap.unlockedSkills().size();
                }));

        // Phase 29: Final HUD screenshot
        steps.add(AutoTestStep.wait("final_settle", 20));
        steps.add(AutoTestStep.screenshot("23_final_hud"));

        // Phase 30: Server-side test via command
        steps.add(AutoTestStep.command("career test", 40));
        steps.add(AutoTestStep.screenshot("24_server_test_results"));
        steps.add(AutoTestStep.command("career debug", 20));
        steps.add(AutoTestStep.screenshot("25_career_debug"));

        // Phase 31: Registry info
        steps.add(AutoTestStep.command("career registry", 20));
        steps.add(AutoTestStep.screenshot("26_registry_info"));

        steps.add(AutoTestStep.wait("final_wait", 20));
    }
}
