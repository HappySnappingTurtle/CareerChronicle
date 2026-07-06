package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.career.CareerProgressionService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record C2SSelectRacePacket(ResourceLocation raceId) {
    public static void encode(C2SSelectRacePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.raceId);
    }

    public static C2SSelectRacePacket decode(FriendlyByteBuf buffer) {
        return new C2SSelectRacePacket(buffer.readResourceLocation());
    }

    public static void handle(C2SSelectRacePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CareerProgressionService.selectRace(player, packet.raceId);
            }
        });
        context.setPacketHandled(true);
    }
}
