package com.hongyuwu.careerchronicle.client;

import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 阶段3-任务3-单元测试用例文档-与原版行走动画共存.md. */
class CustomAnimationPlayersTest {

    private static CustomLegPlayerModel realModel() {
        ModelPart root = CustomLegPlayerModel.createBodyLayer(false).bakeRoot();
        return new CustomLegPlayerModel(root, false);
    }

    // A组: applyOrReset

    @Test
    void a1_nullPlayer_shinZeroed_othersUntouched() {
        CustomLegPlayerModel model = realModel();
        model.rightArm.xRot = 42F; // sentinel

        CustomAnimationPlayers.applyOrReset(null, model, 0F);

        assertEquals(0F, model.rightShin.xRot);
        assertEquals(0F, model.leftShin.xRot);
        assertEquals(42F, model.rightArm.xRot);
    }

    @Test
    void a2_notPlayingPlayer_shinZeroed() {
        CustomAnimationPlayer player = new CustomAnimationPlayer(); // never play()'d -> not playing
        CustomLegPlayerModel model = realModel();
        model.rightShin.xRot = 1.5F; // stale from a previous frame

        CustomAnimationPlayers.applyOrReset(player, model, 0F);

        assertEquals(0F, model.rightShin.xRot);
        assertEquals(0F, model.leftShin.xRot);
    }

    @Test
    void a3_playingPlayer_delegatesToApplyTo() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        AnimationClip clip = new AnimationClip("test:a3", 10, false, Map.of(Bone.RIGHT_ARM, List.of(
                new Keyframe(0, 30F, 0F, 0F, Easing.LINEAR))));
        player.play(clip);

        CustomLegPlayerModel model = realModel();
        CustomAnimationPlayers.applyOrReset(player, model, 0F);

        float expected = 30F * ((float) Math.PI / 180F);
        assertEquals(expected, model.rightArm.xRot, 1e-4F);
    }

    @Test
    void a4_sentinelProvesOnlyShinIsZeroedWhenIdle() {
        CustomLegPlayerModel model = realModel();
        model.rightArm.xRot = 99F;
        model.body.yRot = 88F;
        model.rightLeg.xRot = 77F;

        CustomAnimationPlayers.applyOrReset(null, model, 0F);

        assertEquals(99F, model.rightArm.xRot);
        assertEquals(88F, model.body.yRot);
        assertEquals(77F, model.rightLeg.xRot);
    }

    @Test
    void a5_playingUpperBodyOnlyAnimation_shinStillForcedToZero_notLeakedStaleValue() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        // upper-body-only: no shin track at all
        AnimationClip clip = new AnimationClip("test:a5", 10, false, Map.of(Bone.RIGHT_ARM, List.of(
                new Keyframe(0, 10F, 0F, 0F, Easing.LINEAR))));
        player.play(clip);

        CustomLegPlayerModel model = realModel();
        model.rightShin.xRot = 99F; // simulates a leftover bent pose from a different entity's
        model.leftShin.xRot = 99F;  // full-body animation on this same shared model instance

        CustomAnimationPlayers.applyOrReset(player, model, 0F);

        assertEquals(0F, model.rightShin.xRot, "shin must be forced to neutral, not inherit a stale value");
        assertEquals(0F, model.leftShin.xRot, "shin must be forced to neutral, not inherit a stale value");
    }

    @Test
    void a6_playingFullBodyAnimation_realShinValueSurvivesTheZeroing() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        AnimationClip clip = new AnimationClip("test:a6", 10, false, Map.of(Bone.RIGHT_SHIN, List.of(
                new Keyframe(0, 55F, 0F, 0F, Easing.LINEAR))));
        player.play(clip);

        CustomLegPlayerModel model = realModel();
        CustomAnimationPlayers.applyOrReset(player, model, 0F);

        float expected = 55F * ((float) Math.PI / 180F);
        assertEquals(expected, model.rightShin.xRot, 1e-4F);
    }

    // 引擎审计修复 任务A / A1 (表现引擎全面审计报告_2026-07-15.md A1): applyOrReset's
    // defense-in-depth position restoration -- these plant corruption that no longer happens in
    // practice (writeToPart no longer writes position at all, see CustomAnimationPlayerTest.b6),
    // but the restore must still work if something *else* ever corrupts these fields (a future
    // bone, a conflicting mod, etc.), since vanilla's own setupAnim() never touches rightLeg.x/
    // leftLeg.x nor anything on the shin bones.

    @Test
    void a7_corruptedLegX_restoredToBakedPivot() {
        CustomLegPlayerModel model = realModel();
        float bakedRightX = model.rightLeg.x;
        float bakedLeftX = model.leftLeg.x;
        model.rightLeg.x = 999F; // simulates corruption from some other source
        model.leftLeg.x = -999F;

        CustomAnimationPlayers.applyOrReset(null, model, 0F);

        assertEquals(bakedRightX, model.rightLeg.x, "rightLeg.x must be restored to its baked pivot");
        assertEquals(bakedLeftX, model.leftLeg.x, "leftLeg.x must be restored to its baked pivot");
    }

    @Test
    void a8_corruptedShinPosition_resetToBakedPose() {
        CustomLegPlayerModel model = realModel();
        float bakedShinY = model.rightShin.y;
        model.rightShin.x = 50F;
        model.rightShin.y = 50F;
        model.rightShin.z = 50F;

        CustomAnimationPlayers.applyOrReset(null, model, 0F);

        assertEquals(0F, model.rightShin.x);
        assertEquals(bakedShinY, model.rightShin.y, "shin.y must reset to its baked pivot, not 0");
        assertEquals(0F, model.rightShin.z);
    }

    @Test
    void a9_clear_removesAllRegisteredPlayers() {
        UUID id = UUID.randomUUID();
        CustomAnimationPlayers.getOrCreate(id).play(new AnimationClip("test:a9", 100, false,
                Map.of(Bone.RIGHT_ARM, List.of(new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR)))));
        assertNotNull(CustomAnimationPlayers.getIfPresent(id));

        CustomAnimationPlayers.clear();

        assertNull(CustomAnimationPlayers.getIfPresent(id));
    }

    // B组: registry

    @Test
    void b1_getIfPresent_unknownUuid_returnsNull() {
        assertNull(CustomAnimationPlayers.getIfPresent(UUID.randomUUID()));
    }

    @Test
    void b2_getOrCreate_unknownUuid_returnsNewInstance() {
        assertNotNull(CustomAnimationPlayers.getOrCreate(UUID.randomUUID()));
    }

    @Test
    void b3_getOrCreate_sameUuidTwice_returnsSameInstance() {
        UUID id = UUID.randomUUID();
        CustomAnimationPlayer first = CustomAnimationPlayers.getOrCreate(id);
        CustomAnimationPlayer second = CustomAnimationPlayers.getOrCreate(id);
        assertSame(first, second);
    }

    @Test
    void b4_getOrCreateThenGetIfPresent_sameUuid_returnsSameInstance() {
        UUID id = UUID.randomUUID();
        CustomAnimationPlayer created = CustomAnimationPlayers.getOrCreate(id);
        assertSame(created, CustomAnimationPlayers.getIfPresent(id));
    }

    @Test
    void b5_tickAll_removesFinishedAndNeverStartedEntries() {
        UUID neverPlayed = UUID.randomUUID();
        CustomAnimationPlayers.getOrCreate(neverPlayed); // registered but never play()'d

        UUID finishing = UUID.randomUUID();
        CustomAnimationPlayer finishingPlayer = CustomAnimationPlayers.getOrCreate(finishing);
        finishingPlayer.play(new AnimationClip("test:b5", 2, false, Map.of(Bone.RIGHT_ARM, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR)))));

        CustomAnimationPlayers.tickAll(); // elapsed=1, still playing (1 < 2)
        assertNotNull(CustomAnimationPlayers.getIfPresent(finishing));
        assertNull(CustomAnimationPlayers.getIfPresent(neverPlayed));

        CustomAnimationPlayers.tickAll(); // elapsed=2, finished (2 >= 2)
        assertNull(CustomAnimationPlayers.getIfPresent(finishing));
    }

    @Test
    void b6_tickAll_keepsStillPlayingEntries() {
        UUID id = UUID.randomUUID();
        CustomAnimationPlayer player = CustomAnimationPlayers.getOrCreate(id);
        player.play(new AnimationClip("test:b6", 100, false, Map.of(Bone.RIGHT_ARM, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR)))));

        for (int i = 0; i < 5; i++) {
            CustomAnimationPlayers.tickAll();
        }
        assertNotNull(CustomAnimationPlayers.getIfPresent(id));
        assertTrue(CustomAnimationPlayers.getIfPresent(id).isPlaying());
    }
}
