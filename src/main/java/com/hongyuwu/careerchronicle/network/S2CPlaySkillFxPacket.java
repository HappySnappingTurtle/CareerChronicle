package com.hongyuwu.careerchronicle.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public record S2CPlaySkillFxPacket(
        ResourceLocation skillId,
        String fxType,
        Vec3 origin,
        Vec3 target,
        double particleMultiplier
) {
    private static final String DEFAULT_FX_TYPE = "cast";
    private static final Vec3 ZERO = new Vec3(0.0D, 0.0D, 0.0D);

    public S2CPlaySkillFxPacket {
        fxType = fxType == null || fxType.isBlank() ? DEFAULT_FX_TYPE : fxType;
        origin = origin == null ? ZERO : origin;
        target = target == null ? origin : target;
        particleMultiplier = Math.max(0.0D, Math.min(2.0D, particleMultiplier));
    }

    public static void encode(S2CPlaySkillFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.skillId);
        buffer.writeUtf(packet.fxType, 32);
        writeVec3(buffer, packet.origin);
        writeVec3(buffer, packet.target);
        buffer.writeDouble(packet.particleMultiplier);
    }

    public static S2CPlaySkillFxPacket decode(FriendlyByteBuf buffer) {
        return new S2CPlaySkillFxPacket(
                buffer.readResourceLocation(),
                buffer.readUtf(32),
                readVec3(buffer),
                readVec3(buffer),
                buffer.readDouble()
        );
    }

    public static void handle(S2CPlaySkillFxPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientPacketDispatch.playSkillFx(
                packet.skillId,
                packet.fxType,
                packet.origin,
                packet.target,
                packet.particleMultiplier
        ));
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
