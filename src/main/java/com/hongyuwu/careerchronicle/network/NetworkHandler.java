package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
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
    // 0.2.0: S2CPlaySkillFxPacket moved to the v2 declarative-ops wire format
    // (0.4-05a). Strict-equals handshake means old/new jars simply refuse to
    // connect rather than silently misdecoding each other's fx packets.
    private static final String PROTOCOL_VERSION = "0.2.0";

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
        sendToPlayer(player, () -> new S2CCareerSnapshotPacket(snapshot));
    }

    public static void openCareerScreen(ServerPlayer player) {
        sendToPlayer(player, S2COpenCareerScreenPacket::new);
    }

    /**
     * 0.4-07: discovered via GameTest — {@code helper.makeMockServerPlayerInLevel()}
     * goes through the real {@code PlayerList.placeNewPlayer(...)}, which fires the
     * full player-login event chain (including our own {@code onPlayerLoggedIn} ->
     * {@code CareerDataAccess.sync}) against a player whose {@code Connection} object
     * is real but was never attached to an actual Netty channel. Sending to such a
     * player throws synchronously from deep inside Forge's packet-direction
     * resolution; this guard (mirroring {@code FxDispatcher.hasLiveConnection})
     * makes every outbound send in this class resilient to that, in production as
     * much as in tests -- a player with no live connection has nothing to receive
     * these packets anyway.
     */
    private static void sendToPlayer(ServerPlayer player, java.util.function.Supplier<Object> packet) {
        if (!hasLiveConnection(player)) {
            return;
        }
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet.get());
        } catch (RuntimeException exception) {
            CareerChronicleMod.LOGGER.debug("Skipped packet send to {}: {}", player.getGameProfile().getName(), exception.toString());
        }
    }

    static boolean hasLiveConnection(ServerPlayer player) {
        return player.connection != null
                && player.connection.connection != null
                && player.connection.connection.channel() != null;
    }

    /**
     * Unified fx send entry point. All existing call sites (legacy skill
     * executors, projectile/arrow hit handlers) keep this exact signature and
     * transparently gain casterId + declarative fxOps via FxDispatcher — see
     * 0.4-05a design doc §3.1.
     */
    public static void playSkillFx(ServerPlayer player, ResourceLocation skillId, String fxType, Vec3 origin, Vec3 target) {
        FxDispatcher.send(player, skillId, fxType, origin, target);
    }

    private static int nextPacketId() {
        return packetId++;
    }
}
