package com.hongyuwu.careerchronicle.player;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class CareerDataCapability {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "career_data");

    public static final Capability<ICareerData> CAREER_DATA =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private CareerDataCapability() {
    }
}
