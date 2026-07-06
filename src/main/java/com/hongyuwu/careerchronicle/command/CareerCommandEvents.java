package com.hongyuwu.careerchronicle.command;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.autotest.AutoTestCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID)
public final class CareerCommandEvents {
    private CareerCommandEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CareerCommands.register(event.getDispatcher());
        AutoTestCommand.register(event.getDispatcher());
    }
}
