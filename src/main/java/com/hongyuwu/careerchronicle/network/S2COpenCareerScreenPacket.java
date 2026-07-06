package com.hongyuwu.careerchronicle.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class S2COpenCareerScreenPacket {
    public static void encode(S2COpenCareerScreenPacket packet, FriendlyByteBuf buffer) {
    }

    public static S2COpenCareerScreenPacket decode(FriendlyByteBuf buffer) {
        return new S2COpenCareerScreenPacket();
    }

    public static void handle(S2COpenCareerScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(ClientPacketDispatch::openCareerScreen);
        context.setPacketHandled(true);
    }
}
