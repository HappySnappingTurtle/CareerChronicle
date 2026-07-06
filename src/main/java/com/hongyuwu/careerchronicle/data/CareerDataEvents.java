package com.hongyuwu.careerchronicle.data;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.career.CareerProgressionService;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID)
public final class CareerDataEvents {
    private CareerDataEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new CareerDataReloadListener());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        for (var player : event.getPlayers()) {
            CareerDataAccess.get(player).ifPresent(data -> {
                CareerProgressionService.refreshGrantedSkills(data);
                CareerDataAccess.sync(player);
            });
        }
    }
}
