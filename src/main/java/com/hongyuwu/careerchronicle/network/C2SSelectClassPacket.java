package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.career.CareerProgressionService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record C2SSelectClassPacket(ResourceLocation classId) {
    public static void encode(C2SSelectClassPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.classId);
    }

    public static C2SSelectClassPacket decode(FriendlyByteBuf buffer) {
        return new C2SSelectClassPacket(buffer.readResourceLocation());
    }

    public static void handle(C2SSelectClassPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CareerProgressionService.selectClass(player, packet.classId);
            }
        });
        context.setPacketHandled(true);
    }
}
