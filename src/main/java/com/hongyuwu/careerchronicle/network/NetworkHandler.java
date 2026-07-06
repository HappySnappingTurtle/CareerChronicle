package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.config.ModConfig;
import com.hongyuwu.careerchronicle.player.CareerDataSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;

public final class NetworkHandler {
    private static final String PROTOCOL_VERSION = "0.1.0";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean registered;
    private static int packetId;

    private NetworkHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        CHANNEL.registerMessage(
                nextPacketId(),
                S2CCareerSnapshotPacket.class,
                S2CCareerSnapshotPacket::encode,
                S2CCareerSnapshotPacket::decode,
                S2CCareerSnapshotPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId(),
                S2COpenCareerScreenPacket.class,
                S2COpenCareerScreenPacket::encode,
                S2COpenCareerScreenPacket::decode,
                S2COpenCareerScreenPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId(),
                S2CPlaySkillFxPacket.class,
                S2CPlaySkillFxPacket::encode,
                S2CPlaySkillFxPacket::decode,
                S2CPlaySkillFxPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId(),
                C2SSelectRacePacket.class,
                C2SSelectRacePacket::encode,
                C2SSelectRacePacket::decode,
                C2SSelectRacePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId(),
                C2SSelectClassPacket.class,
                C2SSelectClassPacket::encode,
                C2SSelectClassPacket::decode,
                C2SSelectClassPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId(),
                C2SUseSkillPacket.class,
                C2SUseSkillPacket::encode,
                C2SUseSkillPacket::decode,
                C2SUseSkillPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId(),
                C2SSetSkillLoadoutPacket.class,
                C2SSetSkillLoadoutPacket::encode,
                C2SSetSkillLoadoutPacket::decode,
                C2SSetSkillLoadoutPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId(),
                C2SAllocateAttributePacket.class,
                C2SAllocateAttributePacket::encode,
                C2SAllocateAttributePacket::decode,
                C2SAllocateAttributePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        registered = true;
        CareerChronicleMod.LOGGER.info("Career Chronicle network channel registered.");
    }

    public static void sendCareerSnapshot(ServerPlayer player, CareerDataSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CCareerSnapshotPacket(snapshot));
    }

    public static void openCareerScreen(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2COpenCareerScreenPacket());
    }

    public static void playSkillFx(ServerPlayer player, ResourceLocation skillId, String fxType, Vec3 origin, Vec3 target) {
        double particleMultiplier = ModConfig.SKILL_PARTICLE_MULTIPLIER.get();
        CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(
                origin.x,
                origin.y,
                origin.z,
                32.0D,
                player.level().dimension()
        )), new S2CPlaySkillFxPacket(skillId, fxType, origin, target, particleMultiplier));
    }

    private static int nextPacketId() {
        return packetId++;
    }
}
