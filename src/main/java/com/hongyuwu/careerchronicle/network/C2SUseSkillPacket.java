package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.skill.CareerSkillService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record C2SUseSkillPacket(ResourceLocation skillId) {
    public static void encode(C2SUseSkillPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.skillId);
    }

    public static C2SUseSkillPacket decode(FriendlyByteBuf buffer) {
        return new C2SUseSkillPacket(buffer.readResourceLocation());
    }

    public static void handle(C2SUseSkillPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CareerSkillService.useSkill(player, packet.skillId);
            }
        });
        context.setPacketHandled(true);
    }
}
