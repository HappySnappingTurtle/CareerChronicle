package com.hongyuwu.careerchronicle.network;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FxPacketV2CodecTest {

    private static ResourceLocation skillId() {
        return ResourceLocation.fromNamespaceAndPath("careerchronicle", "fireball");
    }

    private static FxOpSpec soundOp() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "careerchronicle:skill.cast.fire");
        return new FxOpSpec("sound", tag);
    }

    private static FxOpSpec particlesOp() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:flame");
        tag.putInt("count", 12);
        tag.putFloat("spread", 0.6F);
        return new FxOpSpec("particles", tag);
    }

    private static FxOpSpec shakeOp() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("strength", 0.3F);
        tag.putInt("ticks", 6);
        return new FxOpSpec("shake", tag);
    }

    private static FriendlyByteBuf newBuffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    // B1: 正常-3 op 往返
    @Test
    void encodeDecode_threeOps_roundTripsExactly() {
        S2CPlaySkillFxPacket original = new S2CPlaySkillFxPacket(
                skillId(), "cast", 42, 123456789L,
                new Vec3(1.0, 2.0, 3.0), new Vec3(4.0, 5.0, 6.0), 1.5F,
                List.of(soundOp(), particlesOp(), shakeOp()));

        FriendlyByteBuf buffer = newBuffer();
        S2CPlaySkillFxPacket.encode(original, buffer);
        S2CPlaySkillFxPacket decoded = S2CPlaySkillFxPacket.decode(buffer);

        assertEquals(original.skillId(), decoded.skillId());
        assertEquals(original.fxType(), decoded.fxType());
        assertEquals(original.casterId(), decoded.casterId());
        assertEquals(original.seed(), decoded.seed());
        assertEquals(original.origin(), decoded.origin());
        assertEquals(original.target(), decoded.target());
        assertEquals(original.particleMultiplier(), decoded.particleMultiplier());
        assertEquals(original.ops().size(), decoded.ops().size());
        for (int i = 0; i < original.ops().size(); i++) {
            assertEquals(original.ops().get(i).opId(), decoded.ops().get(i).opId());
            assertEquals(original.ops().get(i).params(), decoded.ops().get(i).params());
        }
    }

    // B2: 边界-空 ops
    @Test
    void encodeDecode_emptyOps_roundTripsAndStaysCompact() {
        S2CPlaySkillFxPacket original = new S2CPlaySkillFxPacket(
                skillId(), "cast", 1, 0L, new Vec3(0, 0, 0), new Vec3(0, 0, 0), 1.0F, List.of());

        FriendlyByteBuf buffer = newBuffer();
        S2CPlaySkillFxPacket.encode(original, buffer);
        int packetLength = buffer.writerIndex();
        S2CPlaySkillFxPacket decoded = S2CPlaySkillFxPacket.decode(buffer);

        assertNotNull(decoded.ops());
        assertTrue(decoded.ops().isEmpty());
        assertTrue(packetLength < 128, "packet should stay compact, was " + packetLength + " bytes");
    }

    // B3: 边界-casterId=-1
    @Test
    void encodeDecode_noCasterSentinel_roundTripsAsMinusOne() {
        S2CPlaySkillFxPacket original = new S2CPlaySkillFxPacket(
                skillId(), "hit", S2CPlaySkillFxPacket.NO_CASTER, 0L,
                new Vec3(0, 0, 0), new Vec3(0, 0, 0), 1.0F, List.of());

        FriendlyByteBuf buffer = newBuffer();
        S2CPlaySkillFxPacket.encode(original, buffer);
        S2CPlaySkillFxPacket decoded = S2CPlaySkillFxPacket.decode(buffer);

        assertEquals(-1, decoded.casterId());
    }

    // B4: 边界-multiplier 钳制
    @Test
    void constructor_clampsMultiplierToV1Range() {
        S2CPlaySkillFxPacket high = new S2CPlaySkillFxPacket(
                skillId(), "cast", 0, 0L, new Vec3(0, 0, 0), new Vec3(0, 0, 0), 5.0F, List.of());
        S2CPlaySkillFxPacket low = new S2CPlaySkillFxPacket(
                skillId(), "cast", 0, 0L, new Vec3(0, 0, 0), new Vec3(0, 0, 0), -1.0F, List.of());

        assertEquals(2.0F, high.particleMultiplier());
        assertEquals(0.0F, low.particleMultiplier());
    }

    // B5: 边界-fxType 超长（构造期校验，推荐方案）
    @Test
    void constructor_fxTypeTooLong_throws() {
        String tooLong = "a".repeat(32);
        assertThrows(IllegalArgumentException.class, () -> new S2CPlaySkillFxPacket(
                skillId(), tooLong, 0, 0L, new Vec3(0, 0, 0), new Vec3(0, 0, 0), 1.0F, List.of()));
    }

    // B6: 边界-op 数上限
    @Test
    void constructor_tooManyOps_throws() {
        List<FxOpSpec> nineOps = java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> new FxOpSpec("op" + i, new CompoundTag()))
                .toList();

        assertThrows(IllegalArgumentException.class, () -> new S2CPlaySkillFxPacket(
                skillId(), "cast", 0, 0L, new Vec3(0, 0, 0), new Vec3(0, 0, 0), 1.0F, nineOps));
    }

    // B7: 异常-未知 opId 容错（解码不丢弃，跳过发生在客户端执行层）
    @Test
    void decode_unknownOpId_preservedNotDropped() {
        CompoundTag tag = new CompoundTag();
        tag.putString("marker", "value");
        S2CPlaySkillFxPacket original = new S2CPlaySkillFxPacket(
                skillId(), "cast", 0, 0L, new Vec3(0, 0, 0), new Vec3(0, 0, 0), 1.0F,
                List.of(new FxOpSpec("future_op", tag)));

        FriendlyByteBuf buffer = newBuffer();
        S2CPlaySkillFxPacket.encode(original, buffer);
        S2CPlaySkillFxPacket decoded = S2CPlaySkillFxPacket.decode(buffer);

        assertEquals(1, decoded.ops().size());
        assertEquals("future_op", decoded.ops().get(0).opId());
        assertEquals("value", decoded.ops().get(0).params().getString("marker"));
    }

    // B8: 正常-seed 保真
    @Test
    void encodeDecode_seedRoundTripsForExtremeValues() {
        for (long seed : new long[]{Long.MIN_VALUE, 0L, Long.MAX_VALUE}) {
            S2CPlaySkillFxPacket original = new S2CPlaySkillFxPacket(
                    skillId(), "cast", 0, seed, new Vec3(0, 0, 0), new Vec3(0, 0, 0), 1.0F, List.of());

            FriendlyByteBuf buffer = newBuffer();
            S2CPlaySkillFxPacket.encode(original, buffer);
            S2CPlaySkillFxPacket decoded = S2CPlaySkillFxPacket.decode(buffer);

            assertEquals(seed, decoded.seed());
        }
    }
}
