package com.hongyuwu.careerchronicle.data;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CareerDataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new CareerRecipeProvider(output));
        generator.addProvider(event.includeClient(), new CareerItemModelProvider(output, existingFileHelper));
    }
}
