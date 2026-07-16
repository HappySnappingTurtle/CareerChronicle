package com.hongyuwu.careerchronicle.autotest;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.client.AnimationDriverRegistry;
import com.hongyuwu.careerchronicle.client.CareerClientScreens;
import com.hongyuwu.careerchronicle.client.ClientCareerData;
import com.hongyuwu.careerchronicle.client.RaceSelectionScreen;
import com.hongyuwu.careerchronicle.network.C2SAllocateAttributePacket;
import com.hongyuwu.careerchronicle.network.C2SSelectClassPacket;
import com.hongyuwu.careerchronicle.network.C2SSelectRacePacket;
import com.hongyuwu.careerchronicle.network.C2SSetSkillLoadoutPacket;
import com.hongyuwu.careerchronicle.network.C2SUseSkillPacket;
import com.hongyuwu.careerchronicle.network.FxDispatcher;
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

        // Phase 32: Cooldown re-cast guard
        // After death the inventory is gone; re-give ember_staff so fireball in slot 0 can fire
        steps.add(AutoTestStep.command("give @s careerchronicle:ember_staff", 10));
        steps.add(AutoTestStep.wait("reequip_for_cd_test", 10));
        steps.add(AutoTestStep.action("cast_for_cooldown_test", 5,
                ctrl -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (!snap.skillLoadout().isEmpty() && snap.skillLoadout().get(0) != null) {
                        NetworkHandler.CHANNEL.sendToServer(new C2SUseSkillPacket(snap.skillLoadout().get(0)));
                    }
                }));
        steps.add(AutoTestStep.wait("cooldown_settle", 10));
        steps.add(AutoTestStep.verified("cooldown_recast_guard", 5,
                ctrl -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (!snap.skillLoadout().isEmpty() && snap.skillLoadout().get(0) != null) {
                        int cd = ClientCareerData.cooldown(snap.skillLoadout().get(0));
                        ctrl.recordResult("cooldown_before_recast", true, "CD=" + cd);
                        NetworkHandler.CHANNEL.sendToServer(new C2SUseSkillPacket(snap.skillLoadout().get(0)));
                    }
                },
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (!snap.skillLoadout().isEmpty() && snap.skillLoadout().get(0) != null) {
                        int cd = ClientCareerData.cooldown(snap.skillLoadout().get(0));
                        if (cd > 0) return null; // Still on cooldown = guard worked
                        return "Cooldown reset to 0 after re-cast attempt";
                    }
                    return "No skill in slot 0";
                }));
        steps.add(AutoTestStep.screenshot("27_cooldown_guard"));

        // Phase 33: Resource bar sanity check after casting
        steps.add(AutoTestStep.verified("resource_sanity_check", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    boolean manaOk = snap.maxMana() > 0;
                    boolean staminaOk = snap.maxStamina() > 0;
                    if (manaOk || staminaOk) return null;
                    return "No resources available. maxMana=" + snap.maxMana()
                            + " maxStamina=" + snap.maxStamina();
                }));
        steps.add(AutoTestStep.screenshot("28_resource_sanity"));


        // Phase 33b (0.4-05a): FX dispatch counter smoke tests -- F1/F2/F3.
        // Placed right after resource_sanity_check (confirmed mana/stamina > 0
        // here) and before Phase 34's unequip/Phase 37's reset, so this reuses
        // fireball (already equipped in slot 0, fire_mage selected around level
        // 20) and scatter_shot/frost_arrow (already unlocked -- archer, fire_mage
        // and ice_mage were all selected earlier in the main flow, so both the
        // fire_mage+archer and ice_mage+archer fusions auto-unlocked). No new
        // class selection, no `career reset`, no XP grinding needed -- avoids the
        // segment/level-cap bookkeeping that made an earlier version of this test
        // fragile. `/item replace` forces the required weapon into the main hand
        // rather than relying on `/give` landing there, since both
        // CareerLoadoutService.setActiveSlot and useKnownSkill check the
        // currently held item.
        //
        // Workaround for a separate, pre-existing bug found while building this
        // test (independent of 0.4-05a, flagged for its own bug report): Phase 28's
        // death/respawn runs CareerProgressionService.refreshGrantedSkills on the
        // freshly-cloned player entity via onPlayerRespawn, and that call's
        // validSkills set ends up covering only the starter class (warrior) --
        // data.retainUnlockedSkills(validSkills) then strips every skill granted by
        // fire_mage/archer/priest/ice_mage down to just the 3 warrior skills. The
        // "data_persisted_after_death" check at Phase 28 doesn't catch this because
        // it only asserts unlockedSkills is non-empty, not that it's complete.
        // `/reload` re-triggers refreshGrantedSkills via OnDatapackSyncEvent against
        // the *same* long-lived capability instance (no clone involved), so
        // classHistory is already complete and unlockedSkills is correctly rebuilt.
        steps.add(AutoTestStep.command("reload", 20));
        steps.add(AutoTestStep.wait("reload_sync", 60));
        steps.add(AutoTestStep.verified("skills_restored_after_reload", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    return snap.unlockedSkills().contains(id("fireball"))
                            && snap.unlockedSkills().contains(id("scatter_shot"))
                            ? null : "unlockedSkills still missing fireball/scatter_shot after /reload: "
                            + snap.unlockedSkills();
                }));
        steps.add(AutoTestStep.wait("fireball_cooldown_clear", 80));
        steps.add(AutoTestStep.command("item replace entity @s weapon.mainhand with careerchronicle:ember_staff", 10));
        steps.add(AutoTestStep.wait("ember_staff_sync", 10));
        steps.add(AutoTestStep.action("fx_counter_smoke_reset", 5,
                ctrl -> {
                    FxDispatcher.TestHooks.clearForTesting();
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    ctrl.recordResult("pre_fireball_cast_state", true,
                            "mana=" + snap.mana() + "/" + snap.maxMana()
                            + " cooldown=" + ClientCareerData.cooldown(id("fireball"))
                            + " loadout=" + snap.skillLoadout());
                }));
        steps.add(AutoTestStep.action("cast_fireball_for_fx_smoke", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SUseSkillPacket(id("fireball")))));
        steps.add(AutoTestStep.wait("fx_counter_smoke_settle", 10));
        steps.add(AutoTestStep.verified("fx_counter_smoke", 5,
                ctrl -> {},
                () -> {
                    int count = FxDispatcher.TestHooks.sentCount("cast");
                    if (count == 1) return null;
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    return "Expected exactly 1 'cast' fx dispatch after casting fireball once, got " + count
                            + " (diag: mana=" + snap.mana() + "/" + snap.maxMana()
                            + " cooldown=" + ClientCareerData.cooldown(id("fireball"))
                            + " loadout=" + snap.skillLoadout() + ")";
                }));

        // --- F2: scatter_shot (archer, already unlocked earlier in the main flow) ---
        steps.add(AutoTestStep.command("item replace entity @s weapon.mainhand with careerchronicle:chronicle_recurve", 10));
        steps.add(AutoTestStep.wait("chronicle_recurve_sync", 10));
        steps.add(AutoTestStep.action("equip_scatter_shot_for_fx_test", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSetSkillLoadoutPacket(
                        CareerLoadoutService.SLOT_TYPE_ACTIVE, 1, id("scatter_shot")))));
        steps.add(AutoTestStep.wait("scatter_shot_equip_sync", 20));
        steps.add(AutoTestStep.verified("scatter_shot_equipped_for_fx_test", 5,
                ctrl -> {},
                () -> {
                    List<ResourceLocation> loadout = ClientCareerData.snapshot().skillLoadout();
                    return loadout.size() > 1 && id("scatter_shot").equals(loadout.get(1))
                            ? null : "scatter_shot not equipped in slot 1 after gearing up with chronicle_recurve; loadout=" + loadout;
                }));
        // scatter_shot fires 3 arrows from one cast; ProjectileOp/ArrowOp's in-op fx
        // call was removed (0.4-05a H1), so this must still be exactly 1, not 3.
        steps.add(AutoTestStep.action("projectile_dedup_reset", 5,
                ctrl -> {
                    FxDispatcher.TestHooks.clearForTesting();
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    ctrl.recordResult("pre_scatter_shot_cast_state", true,
                            "stamina=" + snap.stamina() + "/" + snap.maxStamina()
                            + " cooldown=" + ClientCareerData.cooldown(id("scatter_shot")));
                }));
        steps.add(AutoTestStep.action("cast_scatter_shot_for_dedup", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SUseSkillPacket(id("scatter_shot")))));
        steps.add(AutoTestStep.wait("projectile_dedup_settle", 10));
        steps.add(AutoTestStep.verified("projectile_dedup", 5,
                ctrl -> {},
                () -> {
                    int count = FxDispatcher.TestHooks.sentCount("cast");
                    if (count == 1) return null;
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    return "Expected exactly 1 'cast' fx dispatch after casting 3-arrow scatter_shot once, got "
                            + count + " (regression: in-op fx not deduplicated; diag: stamina="
                            + snap.stamina() + "/" + snap.maxStamina()
                            + " cooldown=" + ClientCareerData.cooldown(id("scatter_shot")) + ")";
                }));

        // --- F3: frost_arrow (fusion of ice_mage+archer, both selected earlier;
        // executor-only skill with no fx block/dispatchCast hook -- exercises the
        // legacy path through NetworkHandler.playSkillFx -> FxDispatcher.send) ---
        steps.add(AutoTestStep.action("equip_frost_arrow_for_fx_test", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSetSkillLoadoutPacket(
                        CareerLoadoutService.SLOT_TYPE_ACTIVE, 2, id("frost_arrow")))));
        steps.add(AutoTestStep.wait("frost_arrow_equip_sync", 20));
        steps.add(AutoTestStep.verified("frost_arrow_equipped_for_fx_test", 5,
                ctrl -> {},
                () -> {
                    List<ResourceLocation> loadout = ClientCareerData.snapshot().skillLoadout();
                    return loadout.size() > 2 && id("frost_arrow").equals(loadout.get(2))
                            ? null : "frost_arrow not equipped in slot 2; loadout=" + loadout
                            + " (is ice_mage+archer fusion unlocked? unlockedSkills="
                            + ClientCareerData.snapshot().unlockedSkills() + ")";
                }));
        steps.add(AutoTestStep.action("legacy_path_intact_reset", 5,
                ctrl -> {
                    FxDispatcher.TestHooks.clearForTesting();
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    ctrl.recordResult("pre_frost_arrow_cast_state", true,
                            "mana=" + snap.mana() + "/" + snap.maxMana()
                            + " cooldown=" + ClientCareerData.cooldown(id("frost_arrow")));
                }));
        steps.add(AutoTestStep.action("cast_frost_arrow_for_legacy_check", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SUseSkillPacket(id("frost_arrow")))));
        steps.add(AutoTestStep.wait("legacy_path_intact_settle", 10));
        steps.add(AutoTestStep.verified("legacy_path_intact", 5,
                ctrl -> {},
                () -> {
                    int count = FxDispatcher.TestHooks.sentCount("cast");
                    if (count == 1) return null;
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    return "Expected exactly 1 'cast' fx dispatch from legacy executor frost_arrow, got " + count
                            + " (diag: mana=" + snap.mana() + "/" + snap.maxMana()
                            + " cooldown=" + ClientCareerData.cooldown(id("frost_arrow")) + ")";
                }));
        steps.add(AutoTestStep.screenshot("28b_fx_counter_smoke_tests"));

        // Restore slot 0 to fireball for the rest of the flow (Phase 34 onward
        // expects slot 0 == fireball, e.g. Phase 37's iron_vanguard check reads
        // unlockedSkills only, but keep the loadout tidy regardless).
        steps.add(AutoTestStep.action("restore_slot0_after_fx_test", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSetSkillLoadoutPacket(
                        CareerLoadoutService.SLOT_TYPE_ACTIVE, 0, id("fireball")))));
        steps.add(AutoTestStep.wait("restore_slot0_sync", 10));

        // Phase 34: Skill unequip — clear loadout slot 0
        steps.add(AutoTestStep.action("unequip_slot_0", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSetSkillLoadoutPacket(
                        CareerLoadoutService.SLOT_TYPE_ACTIVE, 0, null))));
        steps.add(AutoTestStep.wait("unequip_sync", 20));
        steps.add(AutoTestStep.verified("slot_0_cleared", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.skillLoadout().isEmpty() || snap.skillLoadout().get(0) == null) return null;
                    return "Slot 0 not cleared. Still has: " + snap.skillLoadout().get(0);
                }));
        steps.add(AutoTestStep.screenshot("29_skill_unequipped"));

        // Phase 35: Re-equip skill after unequip and verify
        steps.add(AutoTestStep.action("reequip_slot_0", 5,
                ctrl -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (!snap.unlockedSkills().isEmpty()) {
                        ResourceLocation firstSkill = snap.unlockedSkills().iterator().next();
                        NetworkHandler.CHANNEL.sendToServer(new C2SSetSkillLoadoutPacket(
                                CareerLoadoutService.SLOT_TYPE_ACTIVE, 0, firstSkill));
                    }
                }));
        steps.add(AutoTestStep.wait("reequip_sync", 20));
        steps.add(AutoTestStep.verified("slot_0_reequipped", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (!snap.skillLoadout().isEmpty() && snap.skillLoadout().get(0) != null) return null;
                    return "Slot 0 still empty after re-equip";
                }));
        steps.add(AutoTestStep.screenshot("30_skill_reequipped"));

        // Phase 36: Career screen navigation — final build review
        steps.add(AutoTestStep.action("open_career_review", 20,
                ctrl -> Minecraft.getInstance().execute(CareerClientScreens::openEntryScreen)));
        steps.add(AutoTestStep.screenshot("31_career_review"));
        steps.add(AutoTestStep.action("close_career_review", 5,
                ctrl -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(null))));

        // Phase 37: Reset + duplicate class selection — warrior repeat for iron_vanguard
        steps.add(AutoTestStep.action("record_pre_reset_state", 5,
                ctrl -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    ctrl.recordResult("pre_reset_state", true,
                            "Level=" + snap.careerLevel()
                            + " Classes=" + snap.classHistory().size()
                            + " Skills=" + snap.unlockedSkills().size());
                }));
        steps.add(AutoTestStep.command("career reset", 10));
        steps.add(AutoTestStep.wait("reset_for_dup_sync", 30));
        steps.add(AutoTestStep.verified("reset_confirmed", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.classHistory().isEmpty() && snap.careerLevel() == 1) return null;
                    return "Reset incomplete. Level=" + snap.careerLevel()
                            + " Classes=" + snap.classHistory();
                }));
        // Select race again after reset
        steps.add(AutoTestStep.action("reselect_race_human", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSelectRacePacket(id("human")))));
        steps.add(AutoTestStep.wait("race_resync", 30));
        // Select warrior first time (segment 1 at level 1)
        steps.add(AutoTestStep.action("select_warrior_dup_1", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSelectClassPacket(id("warrior")))));
        steps.add(AutoTestStep.wait("warrior_dup1_sync", 30));
        steps.add(AutoTestStep.verified("warrior_first_selected", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.classHistory().contains(id("warrior"))) return null;
                    return "Warrior 1st not in history: " + snap.classHistory();
                }));
        // Add XP to reach level >= 10 for 2nd segment
        steps.add(AutoTestStep.command("career add-xp 100000", 20));
        steps.add(AutoTestStep.wait("xp_for_dup_sync", 30));
        // Select warrior second time (segment 2) — triggers repeat_rewards count=2
        steps.add(AutoTestStep.action("select_warrior_dup_2", 5,
                ctrl -> NetworkHandler.CHANNEL.sendToServer(new C2SSelectClassPacket(id("warrior")))));
        steps.add(AutoTestStep.wait("warrior_dup2_sync", 30));
        steps.add(AutoTestStep.verified("iron_vanguard_unlocked", 5,
                ctrl -> {},
                () -> {
                    CareerDataSnapshot snap = ClientCareerData.snapshot();
                    if (snap.unlockedSkills().contains(id("iron_vanguard"))) return null;
                    return "iron_vanguard not unlocked after 2nd warrior. Skills="
                            + snap.unlockedSkills();
                }));
        steps.add(AutoTestStep.screenshot("32_duplicate_class_iron_vanguard"));

        steps.add(AutoTestStep.wait("final_wait", 20));
    }

    /**
     * Stage-1 leg-joint validation scenario (自定义骨骼引擎-单元测试用例文档 E 组, 阶段1). Separate from
     * {@link #buildFullFlowSteps} (selected via {@code careerchronicle.autotest.scenario=legjoint},
     * see {@link AutoTestController#start()}) so this throwaway stage-1 check never touches the
     * existing regression flow. Switches to third-person so the legs are actually visible, then
     * drives {@link com.hongyuwu.careerchronicle.client.CustomLegModelSwap#setShinPitchDegrees}
     * directly -- the exact same call the temporary {@code /careershin} debug command makes --
     * and screenshots before/after so the knee fold can be eyeballed from the saved PNGs.
     */
    public static void buildLegJointStageOneSteps(List<AutoTestStep> steps) {
        steps.add(AutoTestStep.wait("world_load", 60));
        // A brand-new world auto-opens RaceSelectionScreen after join and blocks all further
        // input/rendering until a race is picked -- without this, every later screenshot in this
        // scenario is just a static copy of that screen (bitten by this once: got 4 byte-identical
        // "knee bend" screenshots because the character never actually spawned into the world).
        // Handle it defensively: only select+close if the screen is actually showing.
        steps.add(AutoTestStep.action("select_race_if_needed", 5,
                ctrl -> {
                    if (Minecraft.getInstance().screen instanceof RaceSelectionScreen) {
                        NetworkHandler.CHANNEL.sendToServer(new C2SSelectRacePacket(id("human")));
                    }
                }));
        steps.add(AutoTestStep.wait("race_sync", 30));
        // RaceSelectionScreen.refreshFromServer() auto-transitions itself to CareerScreen the
        // moment the race sync lands (RaceSelectionScreen.java:70) -- by the time this step runs,
        // mc.screen is already CareerScreen, not RaceSelectionScreen, so checking for the original
        // type here would silently no-op and leave that screen open, blocking the world render.
        // Close whatever screen is actually up, not just the one we expected.
        steps.add(AutoTestStep.action("close_any_screen_after_race_sync", 5,
                ctrl -> {
                    if (Minecraft.getInstance().screen != null) {
                        Minecraft.getInstance().setScreen(null);
                    }
                }));
        steps.add(AutoTestStep.wait("screen_close_settle", 10));
        // This save is shared with the full-flow regression scenario and may already have leg
        // armor equipped from an earlier run -- strip it so the base leg mesh (the thing this
        // stage-1 check actually needs to see) isn't hidden behind a rigid armor shell (armor
        // not following the new shin joint is a known, explicitly out-of-scope limitation for
        // this stage, not something to accidentally test around).
        steps.add(AutoTestStep.command("item replace entity @s armor.legs with minecraft:air", 5));
        steps.add(AutoTestStep.command("item replace entity @s armor.feet with minecraft:air", 5));
        steps.add(AutoTestStep.action("switch_third_person", 5,
                ctrl -> Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK)));
        steps.add(AutoTestStep.wait("camera_switch_settle", 15));
        steps.add(AutoTestStep.verified("model_swap_succeeded", 5,
                ctrl -> {},
                () -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.isSwapSucceeded()
                        ? null : "CustomLegPlayerModel reflective swap did not succeed -- check ERROR logs"));
        // C2 (自定义骨骼引擎-单元测试用例文档-护甲跟随腿部弯曲.md): armor layer swap is independent
        // of the body model swap (armorSwapSucceeded has its own reflection path), so it gets its
        // own assertion rather than being assumed from model_swap_succeeded passing.
        steps.add(AutoTestStep.verified("armor_swap_succeeded", 5,
                ctrl -> {},
                () -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.isArmorSwapSucceeded()
                        ? null : "CustomLegArmorLayer swap did not succeed -- check ERROR logs"));
        steps.add(AutoTestStep.screenshot("legjoint_00_third_person_rest_pose"));

        steps.add(AutoTestStep.action("bend_knees_30deg", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(30F)));
        steps.add(AutoTestStep.wait("render_settle_30", 10));
        steps.add(AutoTestStep.screenshot("legjoint_01_knee_bent_30deg"));

        steps.add(AutoTestStep.action("bend_knees_75deg", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(75F)));
        steps.add(AutoTestStep.wait("render_settle_75", 10));
        steps.add(AutoTestStep.screenshot("legjoint_02_knee_bent_75deg"));

        steps.add(AutoTestStep.action("reset_knees", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(0F)));
        steps.add(AutoTestStep.wait("render_settle_reset", 10));
        steps.add(AutoTestStep.screenshot("legjoint_03_knee_reset"));

        // Stage-2 (设计文档 step 2): now put the leg armor back on and bend the knee again --
        // this is the "eyeball it, armor is known not to follow the joint, confirm it's still
        // acceptable (no clipping/z-fighting/crash)" check, not a pass/fail assertion.
        steps.add(AutoTestStep.command("item replace entity @s armor.legs with minecraft:iron_leggings", 5));
        steps.add(AutoTestStep.command("item replace entity @s armor.feet with minecraft:iron_boots", 5));
        steps.add(AutoTestStep.wait("armor_equip_settle", 10));
        steps.add(AutoTestStep.screenshot("legjoint_04_armor_rest_pose"));

        steps.add(AutoTestStep.action("bend_knees_30deg_armored", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(30F)));
        steps.add(AutoTestStep.wait("render_settle_armor_30", 10));
        steps.add(AutoTestStep.screenshot("legjoint_05_armor_bent_30deg"));

        steps.add(AutoTestStep.action("bend_knees_75deg_armored", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(75F)));
        steps.add(AutoTestStep.wait("render_settle_armor_75", 10));
        steps.add(AutoTestStep.screenshot("legjoint_06_armor_bent_75deg"));

        steps.add(AutoTestStep.action("reset_knees_armored", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(0F)));
        steps.add(AutoTestStep.wait("render_settle_armor_reset", 10));

        // C4/C5/C6 (自定义骨骼引擎-单元测试用例文档-护甲跟随腿部弯曲.md): enchant glint, armor trim,
        // and leather dye are all rendered off the same armor model instance that just got bent
        // (HumanoidArmorLayer.renderArmorPiece -> renderTrim/renderGlint/renderModel all take the
        // same `model` argument), so per the design doc these should follow "for free" -- worth a
        // dedicated screenshot each rather than assuming it from the plain-iron result above.
        steps.add(AutoTestStep.command(
                "item replace entity @s armor.legs with minecraft:iron_leggings{Enchantments:[{id:\"minecraft:protection\",lvl:1}]}", 5));
        steps.add(AutoTestStep.wait("enchant_equip_settle", 10));
        steps.add(AutoTestStep.action("bend_knees_75deg_enchanted", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(75F)));
        steps.add(AutoTestStep.wait("render_settle_enchant_75", 10));
        steps.add(AutoTestStep.screenshot("legjoint_07_enchanted_bent_75deg"));
        steps.add(AutoTestStep.action("reset_knees_enchanted", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(0F)));
        steps.add(AutoTestStep.wait("render_settle_enchant_reset", 10));

        steps.add(AutoTestStep.command(
                "item replace entity @s armor.legs with minecraft:iron_leggings{Trim:{material:\"minecraft:redstone\",pattern:\"minecraft:coast\"}}", 5));
        steps.add(AutoTestStep.wait("trim_equip_settle", 10));
        steps.add(AutoTestStep.action("bend_knees_75deg_trim", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(75F)));
        steps.add(AutoTestStep.wait("render_settle_trim_75", 10));
        steps.add(AutoTestStep.screenshot("legjoint_08_trim_bent_75deg"));
        steps.add(AutoTestStep.action("reset_knees_trim", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(0F)));
        steps.add(AutoTestStep.wait("render_settle_trim_reset", 10));

        steps.add(AutoTestStep.command(
                "item replace entity @s armor.legs with minecraft:leather_leggings{display:{color:16711680}}", 5));
        steps.add(AutoTestStep.wait("leather_equip_settle", 10));
        steps.add(AutoTestStep.action("bend_knees_75deg_leather", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(75F)));
        steps.add(AutoTestStep.wait("render_settle_leather_75", 10));
        steps.add(AutoTestStep.screenshot("legjoint_09_leather_dyed_bent_75deg"));
        steps.add(AutoTestStep.action("reset_knees_leather", 5,
                ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setShinPitchDegrees(0F)));
        steps.add(AutoTestStep.wait("render_settle_leather_reset", 10));

        steps.add(AutoTestStep.wait("final_wait", 20));
    }

    /**
     * 阶段3-任务6 诊断: systematically probes what each (bone, axis) rotation actually looks like
     * from the default third-person camera, instead of assuming pitch/yaw/roll map onto "obvious"
     * real-world directions -- user feedback after the setupAnim() timing bugfix asked for this:
     * "先保证所有的东西跟json之间的对应关系...而不是猜测为主" (verify the JSON-to-visual mapping as a
     * reference before designing more keyframe data on top of guesses). Uses
     * {@link com.hongyuwu.careerchronicle.client.CustomLegModelSwap#setDebugBoneAxis} (generic
     * bone/axis debug override, applied post-setupAnim() -- see that method's doc). Selected via
     * {@code -Dcareerchronicle.autotest.scenario=legpitch}.
     */
    public static void buildLegPitchDiagnosticSteps(List<AutoTestStep> steps) {
        steps.add(AutoTestStep.wait("world_load", 60));
        steps.add(AutoTestStep.action("select_race_if_needed", 5,
                ctrl -> {
                    if (Minecraft.getInstance().screen instanceof RaceSelectionScreen) {
                        NetworkHandler.CHANNEL.sendToServer(new C2SSelectRacePacket(id("human")));
                    }
                }));
        steps.add(AutoTestStep.wait("race_sync", 30));
        steps.add(AutoTestStep.action("close_any_screen_after_race_sync", 5,
                ctrl -> {
                    if (Minecraft.getInstance().screen != null) {
                        Minecraft.getInstance().setScreen(null);
                    }
                }));
        steps.add(AutoTestStep.wait("screen_close_settle", 10));
        steps.add(AutoTestStep.action("switch_third_person", 5,
                ctrl -> Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK)));
        steps.add(AutoTestStep.wait("camera_switch_settle", 15));
        steps.add(AutoTestStep.screenshot("boneaxis_00_rest"));

        // (bone, axis, label) x 60 degrees each -- covers the bones/axes that 阶段3's existing
        // keyframe data actually uses (pitch everywhere, yaw on body/arms).
        Object[][] probes = {
                {com.hongyuwu.careerchronicle.client.Bone.RIGHT_LEG, 'p', "rightleg_pitch", 60F},
                {com.hongyuwu.careerchronicle.client.Bone.RIGHT_LEG, 'p', "rightleg_pitch_neg", -45F},
                {com.hongyuwu.careerchronicle.client.Bone.RIGHT_LEG, 'y', "rightleg_yaw", 60F},
                {com.hongyuwu.careerchronicle.client.Bone.RIGHT_LEG, 'r', "rightleg_roll", 60F},
                {com.hongyuwu.careerchronicle.client.Bone.RIGHT_ARM, 'p', "rightarm_pitch", 60F},
                {com.hongyuwu.careerchronicle.client.Bone.RIGHT_ARM, 'y', "rightarm_yaw", 60F},
                {com.hongyuwu.careerchronicle.client.Bone.RIGHT_ARM, 'r', "rightarm_roll", 60F},
                {com.hongyuwu.careerchronicle.client.Bone.BODY, 'p', "body_pitch", 60F},
                {com.hongyuwu.careerchronicle.client.Bone.BODY, 'y', "body_yaw", 60F},
        };
        for (Object[] probe : probes) {
            com.hongyuwu.careerchronicle.client.Bone bone = (com.hongyuwu.careerchronicle.client.Bone) probe[0];
            char axis = (char) probe[1];
            String label = (String) probe[2];
            float degrees = (float) probe[3];
            // Defensive: a window-focus loss (or any other cause) can pop PauseScreen mid-scenario
            // (observed once in practice -- ScreenEvent.Opening: null -> PauseScreen), which would
            // otherwise silently make every subsequent screenshot just a photo of the pause menu.
            steps.add(AutoTestStep.action("close_any_screen_before_" + label, 5,
                    ctrl -> {
                        if (Minecraft.getInstance().screen != null) {
                            Minecraft.getInstance().setScreen(null);
                        }
                    }));
            steps.add(AutoTestStep.action("set_" + label,
                    5, ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setDebugBoneAxis(bone, axis, degrees)));
            steps.add(AutoTestStep.wait("settle_" + label, 5));
            steps.add(AutoTestStep.screenshot("boneaxis_" + label));
            steps.add(AutoTestStep.action("clear_" + label,
                    5, ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.clearDebugBoneAxes()));
        }

        // Static-pose replay of cast_stomp's exact keyframe values (阶段3-任务5 rev3), bypassing
        // AnimationClip tick-based playback entirely -- the tick-based castanim scenario proved
        // unreliable for capturing a specific tick's pose (diagnostic logging showed the
        // "tick5_legraised" screenshot actually landed around tick7's values, not tick5's, due to
        // the same elapsedTicks-runs-ahead-of-AutoTestStep-wait-count effect seen earlier with
        // fireball). Testing each keyframe's five-bone combination as a static snapshot instead
        // isolates "does this keyframe's pose look right" from "did the screenshot land on the
        // right tick".
        Object[][][] keyframeSnapshots = {
                { // tick5: 抬腿
                        {com.hongyuwu.careerchronicle.client.Bone.RIGHT_LEG, 'p', -45F},
                        {com.hongyuwu.careerchronicle.client.Bone.RIGHT_SHIN, 'p', 8F},
                        {com.hongyuwu.careerchronicle.client.Bone.LEFT_LEG, 'p', 4F},
                        {com.hongyuwu.careerchronicle.client.Bone.LEFT_SHIN, 'p', 16F},
                        {com.hongyuwu.careerchronicle.client.Bone.BODY, 'p', -4F},
                },
                { // tick7: 冲击
                        {com.hongyuwu.careerchronicle.client.Bone.RIGHT_LEG, 'p', 8F},
                        {com.hongyuwu.careerchronicle.client.Bone.RIGHT_SHIN, 'p', 20F},
                        {com.hongyuwu.careerchronicle.client.Bone.LEFT_LEG, 'p', 10F},
                        {com.hongyuwu.careerchronicle.client.Bone.LEFT_SHIN, 'p', 26F},
                        {com.hongyuwu.careerchronicle.client.Bone.BODY, 'p', 8F},
                },
                { // tick9: 跟进
                        {com.hongyuwu.careerchronicle.client.Bone.RIGHT_LEG, 'p', 0F},
                        {com.hongyuwu.careerchronicle.client.Bone.RIGHT_SHIN, 'p', 12F},
                        {com.hongyuwu.careerchronicle.client.Bone.LEFT_LEG, 'p', 6F},
                        {com.hongyuwu.careerchronicle.client.Bone.LEFT_SHIN, 'p', 18F},
                        {com.hongyuwu.careerchronicle.client.Bone.BODY, 'p', 18F},
                },
        };
        String[] keyframeLabels = {"stomp_kf_tick5_legraise", "stomp_kf_tick7_impact", "stomp_kf_tick9_followthrough"};
        for (int i = 0; i < keyframeSnapshots.length; i++) {
            Object[][] bones = keyframeSnapshots[i];
            String label = keyframeLabels[i];
            steps.add(AutoTestStep.action("close_any_screen_before_" + label, 5,
                    ctrl -> {
                        if (Minecraft.getInstance().screen != null) {
                            Minecraft.getInstance().setScreen(null);
                        }
                    }));
            steps.add(AutoTestStep.action("set_" + label, 5,
                    ctrl -> {
                        for (Object[] b : bones) {
                            com.hongyuwu.careerchronicle.client.CustomLegModelSwap.setDebugBoneAxis(
                                    (com.hongyuwu.careerchronicle.client.Bone) b[0], (char) b[1], (float) b[2]);
                        }
                    }));
            steps.add(AutoTestStep.wait("settle_" + label, 5));
            steps.add(AutoTestStep.screenshot("boneaxis_" + label));
            steps.add(AutoTestStep.action("clear_" + label, 5,
                    ctrl -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.clearDebugBoneAxes()));
        }

        steps.add(AutoTestStep.wait("final_wait", 20));
    }

    /**
     * 阶段3-任务5-单元测试用例文档 (视觉验证段): drives the 4 migrated custom_animation clips
     * directly through {@link AnimationDriverRegistry#current()} -- bypassing skill cast
     * preconditions (mana/stamina/cooldown/equipment) entirely, since this scenario's job is
     * verifying the migrated JSON keyframe data renders correctly through
     * CustomSkeletonAnimationDriver/CustomAnimationPlayer/CustomLegPlayerModel, not re-testing the
     * cast pipeline itself (already covered by the full-flow scenario's F1-F3 steps). Selected via
     * {@code -Dcareerchronicle.autotest.scenario=castanim}, same pattern as {@code legjoint}.
     *
     * <p>Screenshot ticks are chosen at each clip's most visually distinctive keyframe(s) -- for
     * {@code cast_stomp} specifically, tick4/6/8 span the wind-up/impact/torso-follow-through
     * sequence so the redesigned asymmetric leg mechanics (阶段3-任务5, replacing the old
     * whole-leg-only stomp) are visible across multiple frames, not just one.
     */
    public static void buildCastAnimStageFiveSteps(List<AutoTestStep> steps) {
        steps.add(AutoTestStep.wait("world_load", 60));
        steps.add(AutoTestStep.action("select_race_if_needed", 5,
                ctrl -> {
                    if (Minecraft.getInstance().screen instanceof RaceSelectionScreen) {
                        NetworkHandler.CHANNEL.sendToServer(new C2SSelectRacePacket(id("human")));
                    }
                }));
        steps.add(AutoTestStep.wait("race_sync", 30));
        steps.add(AutoTestStep.action("close_any_screen_after_race_sync", 5,
                ctrl -> {
                    if (Minecraft.getInstance().screen != null) {
                        Minecraft.getInstance().setScreen(null);
                    }
                }));
        steps.add(AutoTestStep.wait("screen_close_settle", 10));
        steps.add(AutoTestStep.action("switch_third_person", 5,
                ctrl -> Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK)));
        steps.add(AutoTestStep.wait("camera_switch_settle", 15));
        steps.add(AutoTestStep.verified("model_swap_succeeded", 5,
                ctrl -> {},
                () -> com.hongyuwu.careerchronicle.client.CustomLegModelSwap.isSwapSucceeded()
                        ? null : "CustomLegPlayerModel reflective swap did not succeed -- check ERROR logs"));
        steps.add(AutoTestStep.screenshot("castanim_00_rest_pose"));

        playAndCapture(steps, "careerchronicle:cast_onehand_quick", "fireball", true,
                new int[] {3, 6}, new String[] {"01_fireball_tick3_windup", "02_fireball_tick6_peak"}, 10);

        playAndCapture(steps, "careerchronicle:cast_twohand_chant", "blessing", true,
                new int[] {10}, new String[] {"03_blessing_tick10_arms_raised"}, 18);

        playAndCapture(steps, "careerchronicle:cast_onehand_slash", "lunge_strike", true,
                new int[] {6}, new String[] {"04_lunge_strike_tick6_slash"}, 12);

        // cast_stomp: rev2 (用户实机反馈重做后) -- tick5 leg-raised-parallel-to-ground, tick7
        // impact, tick9 torso follow-through peak (阶段3-任务5 rev2 keyframe grid).
        playAndCapture(steps, "careerchronicle:cast_stomp", "ground_slam", false,
                new int[] {5, 7, 9},
                new String[] {"05_ground_slam_tick5_legraised", "06_ground_slam_tick7_impact", "07_ground_slam_tick9_followthrough"},
                17);

        steps.add(AutoTestStep.wait("final_wait", 20));
    }

    /** Triggers one custom_animation clip via the real {@link AnimationDriverRegistry} entry point
     * (same call {@code AnimFxOp} makes in production) and screenshots at each requested tick
     * offset from the moment playback starts, then waits out the remainder of the clip so the next
     * {@code playAndCapture} call starts from a clean (non-playing) state.
     *
     * <p>The trigger step's own waitTicks is 0, not some nonzero "settle" value like every other
     * action() call in this file uses -- AutoTestController.advanceStep's contract is "waitTicks
     * ticks pass *inside this step* before moving on" (confirmed by reading its tick handling, not
     * assumed), so any nonzero value here would silently add that many extra elapsed animation
     * ticks before the first capture-tick wait even starts counting, throwing off every
     * captureTicks offset below it by that same constant amount. First bug found by this
     * scenario's own dry run: with waitTicks=5 here, every capture landed 5 ticks later than its
     * label claimed -- e.g. the "tick6 peak" fireball screenshot was actually taken at
     * elapsedTicks=11, past the clip's 10-tick duration, showing the already-finished rest pose. */
    private static void playAndCapture(List<AutoTestStep> steps, String animId, String label,
            boolean upperBodyOnly, int[] captureTicks, String[] screenshotNames, int durationTicks) {
        // Diagnostic: 001/002 dry runs showed no visible pose change in any screenshot despite the
        // tick-offset bug being fixed -- verified() here proves (or disproves) whether the trigger
        // actually reached CustomAnimationPlayers, instead of guessing from screenshots alone.
        boolean[] playResult = new boolean[1];
        steps.add(AutoTestStep.verified("play_" + label, 0,
                ctrl -> {
                    if (Minecraft.getInstance().player != null) {
                        playResult[0] = AnimationDriverRegistry.current().playAnimation(
                                Minecraft.getInstance().player, animId, upperBodyOnly, 1.0F, false);
                    }
                },
                () -> {
                    if (Minecraft.getInstance().player == null) {
                        return label + ": no local player";
                    }
                    if (!playResult[0]) {
                        return label + ": playAnimation() returned false (driver="
                                + AnimationDriverRegistry.current().getClass().getSimpleName() + ")";
                    }
                    com.hongyuwu.careerchronicle.client.CustomAnimationPlayer p =
                            com.hongyuwu.careerchronicle.client.CustomAnimationPlayers.getIfPresent(
                                    Minecraft.getInstance().player.getUUID());
                    if (p == null || !p.isPlaying()) {
                        return label + ": playAnimation() returned true but no active/playing "
                                + "CustomAnimationPlayer found afterward (p=" + p + ")";
                    }
                    return null;
                }));
        int previousTick = 0;
        for (int i = 0; i < captureTicks.length; i++) {
            int gap = captureTicks[i] - previousTick;
            steps.add(AutoTestStep.wait(label + "_settle_" + captureTicks[i], Math.max(1, gap)));
            steps.add(AutoTestStep.screenshot(screenshotNames[i]));
            previousTick = captureTicks[i];
        }
        int remaining = durationTicks - previousTick + 10; // +10 so the clip fully finishes and
        // CustomAnimationPlayers.tickAll() drops the finished entry before the next clip plays.
        steps.add(AutoTestStep.wait(label + "_finish", Math.max(1, remaining)));
    }
}
