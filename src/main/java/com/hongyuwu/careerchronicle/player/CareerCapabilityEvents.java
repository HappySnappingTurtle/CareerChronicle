package com.hongyuwu.careerchronicle.player;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CareerCapabilityEvents {
    private CareerCapabilityEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ICareerData.class);
    }
}
