package com.hongyuwu.careerchronicle.autotest;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID, value = Dist.CLIENT)
public final class AutoTestClientEvents {
    private AutoTestClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        AutoTestController controller = AutoTestController.getInstance();
        if (controller.isRunning()) {
            controller.tick();
        }
    }
}
