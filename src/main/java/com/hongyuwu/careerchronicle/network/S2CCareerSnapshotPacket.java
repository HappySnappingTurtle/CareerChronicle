package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.player.CareerDataSnapshot;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record S2CCareerSnapshotPacket(CareerDataSnapshot snapshot) {
    public static void encode(S2CCareerSnapshotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.snapshot.toNbt());
    }

    public static S2CCareerSnapshotPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new S2CCareerSnapshotPacket(
                tag == null ? CareerDataSnapshot.empty() : CareerDataSnapshot.fromNbt(tag)
        );
    }

    public static void handle(S2CCareerSnapshotPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientPacketDispatch.updateCareerSnapshot(packet.snapshot));
        context.setPacketHandled(true);
    }
}
