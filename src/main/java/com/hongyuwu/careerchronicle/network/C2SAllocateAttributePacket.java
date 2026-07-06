package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.player.CareerData;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record C2SAllocateAttributePacket(String attribute) {
    private static final Set<String> VALID = Set.of(
            CareerData.ATTR_STR, CareerData.ATTR_DEX,
            CareerData.ATTR_INT, CareerData.ATTR_WIS, CareerData.ATTR_CON
    );

    public static void encode(C2SAllocateAttributePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.attribute, 16);
    }

    public static C2SAllocateAttributePacket decode(FriendlyByteBuf buffer) {
        return new C2SAllocateAttributePacket(buffer.readUtf(16));
    }

    public static void handle(C2SAllocateAttributePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!VALID.contains(packet.attribute)) return;
            CareerDataAccess.get(player).ifPresent(data -> {
                if (data.getUnspentAttributePoints() <= 0) {
                    player.displayClientMessage(Component.translatable("careerchronicle.message.no_attr_points")
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }
                data.setAttribute(packet.attribute, data.getAttribute(packet.attribute) + 1);
                data.setUnspentAttributePoints(data.getUnspentAttributePoints() - 1);
                CareerDataAccess.sync(player);
            });
        });
        context.setPacketHandled(true);
    }
}
