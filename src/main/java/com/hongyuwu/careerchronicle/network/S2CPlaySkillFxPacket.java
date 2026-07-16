package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.client.FxClientDispatch;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * v2: declarative fx op list + casterId + seed, replacing the v1 five-field
 * hardcoded-renderer packet (0.4-05a). Handling deliberately bypasses the
 * ClientPacketDispatch reflection bridge (0.4-06 audit finding M8/D5): the
 * reflection bridge silently swallows signature-mismatch failures, which is
 * exactly the "silent regression" failure class this task exists to close.
 */
public record S2CPlaySkillFxPacket(
        ResourceLocation skillId,
        String fxType,
        int casterId,
        long seed,
        Vec3 origin,
        Vec3 target,
        float particleMultiplier,
        List<FxOpSpec> ops
) {
    public static final int NO_CASTER = -1;
    private static final String DEFAULT_FX_TYPE = "cast";
    private static final int MAX_FX_TYPE_LENGTH = 16;
    private static final int MAX_OPS = 8;
    private static final Vec3 ZERO = new Vec3(0.0D, 0.0D, 0.0D);

    public S2CPlaySkillFxPacket {
        fxType = fxType == null || fxType.isBlank() ? DEFAULT_FX_TYPE : fxType;
        if (fxType.length() > MAX_FX_TYPE_LENGTH) {
            throw new IllegalArgumentException("fxType too long (max " + MAX_FX_TYPE_LENGTH + "): " + fxType);
        }
        origin = origin == null ? ZERO : origin;
        target = target == null ? origin : target;
        particleMultiplier = Math.max(0.0F, Math.min(2.0F, particleMultiplier));
        ops = ops == null ? List.of() : List.copyOf(ops);
        if (ops.size() > MAX_OPS) {
            throw new IllegalArgumentException("too many fx ops (max " + MAX_OPS + "): " + ops.size());
        }
    }

    public static void encode(S2CPlaySkillFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.skillId);
        buffer.writeUtf(packet.fxType, MAX_FX_TYPE_LENGTH);
        buffer.writeInt(packet.casterId);
        buffer.writeLong(packet.seed);
        writeVec3(buffer, packet.origin);
        writeVec3(buffer, packet.target);
        buffer.writeFloat(packet.particleMultiplier);
        buffer.writeByte(packet.ops.size());
        for (FxOpSpec op : packet.ops) {
            buffer.writeUtf(op.opId(), MAX_FX_TYPE_LENGTH);
            buffer.writeNbt(op.params());
        }
    }

    public static S2CPlaySkillFxPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation skillId = buffer.readResourceLocation();
        String fxType = buffer.readUtf(MAX_FX_TYPE_LENGTH);
        int casterId = buffer.readInt();
        long seed = buffer.readLong();
        Vec3 origin = readVec3(buffer);
        Vec3 target = readVec3(buffer);
        float particleMultiplier = buffer.readFloat();
        int opCount = buffer.readUnsignedByte();
        List<FxOpSpec> ops = new ArrayList<>(opCount);
        for (int i = 0; i < opCount; i++) {
            String opId = buffer.readUtf(MAX_FX_TYPE_LENGTH);
            CompoundTag params = buffer.readNbt();
            ops.add(new FxOpSpec(opId, params == null ? new CompoundTag() : params));
        }
        return new S2CPlaySkillFxPacket(skillId, fxType, casterId, seed, origin, target, particleMultiplier, ops);
    }

    public static void handle(S2CPlaySkillFxPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                FxClientDispatch.play(packet);
            } catch (RuntimeException exception) {
                CareerChronicleMod.LOGGER.error("Failed to play skill fx for {}.", packet.skillId, exception);
            }
        }));
        context.setPacketHandled(true);
    }

    private static void writeVec3(FriendlyByteBuf buffer, Vec3 vec3) {
        buffer.writeDouble(vec3.x);
        buffer.writeDouble(vec3.y);
        buffer.writeDouble(vec3.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
