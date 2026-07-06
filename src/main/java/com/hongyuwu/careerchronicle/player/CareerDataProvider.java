package com.hongyuwu.careerchronicle.player;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CareerDataProvider implements ICapabilitySerializable<CompoundTag> {
    private final CareerData data = new CareerData();
    private final LazyOptional<ICareerData> optional = LazyOptional.of(() -> data);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> capability,
            @Nullable Direction side
    ) {
        if (capability == CareerDataCapability.CAREER_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializePersistentData();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.deserializePersistentData(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
