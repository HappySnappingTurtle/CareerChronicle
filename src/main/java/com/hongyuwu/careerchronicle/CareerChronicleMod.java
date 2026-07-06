package com.hongyuwu.careerchronicle;

import com.hongyuwu.careerchronicle.config.ModConfig;
import com.hongyuwu.careerchronicle.network.NetworkHandler;
import com.hongyuwu.careerchronicle.skill.effect.EffectOpRegistry;
import com.hongyuwu.careerchronicle.registry.CareerCreativeModeEvents;
import com.hongyuwu.careerchronicle.registry.CareerEntities;
import com.hongyuwu.careerchronicle.registry.CareerItems;
import com.hongyuwu.careerchronicle.registry.CareerSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CareerChronicleMod.MOD_ID)
public class CareerChronicleMod {
    public static final String MOD_ID = "careerchronicle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CareerChronicleMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        CareerItems.ITEMS.register(modEventBus);
        CareerEntities.ENTITY_TYPES.register(modEventBus);
        CareerCreativeModeEvents.TABS.register(modEventBus);
        CareerSounds.SOUNDS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        context.registerConfig(Type.COMMON, ModConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        EffectOpRegistry.registerBuiltins();
        event.enqueueWork(NetworkHandler::register);
        LOGGER.info("Career Chronicle common setup complete.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Career Chronicle server starting.");
    }
}
