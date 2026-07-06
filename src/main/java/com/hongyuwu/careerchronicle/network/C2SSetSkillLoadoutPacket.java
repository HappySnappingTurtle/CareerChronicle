package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.skill.CareerLoadoutService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record C2SSetSkillLoadoutPacket(int slotType, int slot, ResourceLocation skillId) {
    public static void encode(C2SSetSkillLoadoutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slotType);
        buffer.writeVarInt(packet.slot);
        buffer.writeBoolean(packet.skillId != null);
        if (packet.skillId != null) {
            buffer.writeResourceLocation(packet.skillId);
        }
    }

    public static C2SSetSkillLoadoutPacket decode(FriendlyByteBuf buffer) {
        int slotType = buffer.readVarInt();
        int slot = buffer.readVarInt();
        ResourceLocation skillId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        return new C2SSetSkillLoadoutPacket(slotType, slot, skillId);
    }

    public static void handle(C2SSetSkillLoadoutPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CareerLoadoutService.setSlot(player, packet.slotType, packet.slot, packet.skillId);
            }
        });
        context.setPacketHandled(true);
    }
}
