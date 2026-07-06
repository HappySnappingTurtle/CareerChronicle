package com.hongyuwu.careerchronicle.registry;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CareerCreativeModeEvents {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, CareerChronicleMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CAREER_TAB = TABS.register("career_chronicle_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.careerchronicle"))
                    .icon(() -> new ItemStack(CareerItems.CAREER_MANUAL.get()))
                    .displayItems((params, output) -> {
                        // Manual
                        output.accept(CareerItems.CAREER_MANUAL.get());
                        // Materials
                        output.accept(CareerItems.EMBER_CORE.get());
                        output.accept(CareerItems.ARCANE_SHARD.get());
                        output.accept(CareerItems.FROST_CRYSTAL.get());
                        output.accept(CareerItems.DARK_ESSENCE.get());
                        output.accept(CareerItems.HOLY_RELIC.get());
                        output.accept(CareerItems.CAREER_INGOT.get());
                        // Common weapons (all classes)
                        output.accept(CareerItems.EMBER_STAFF.get());
                        output.accept(CareerItems.FROST_STAFF.get());
                        output.accept(CareerItems.DARK_SCEPTER.get());
                        output.accept(CareerItems.CHRONICLE_RECURVE.get());
                        output.accept(CareerItems.SNARE_LAUNCHER.get());
                        output.accept(CareerItems.RUNIC_BLADE.get());
                        output.accept(CareerItems.SHADOW_DAGGER.get());
                        output.accept(CareerItems.GUARDIAN_SHIELD.get());
                        output.accept(CareerItems.SUNLIT_SIGIL.get());
                        // Iron tier
                        output.accept(CareerItems.IRON_EMBER_STAFF.get());
                        output.accept(CareerItems.IRON_CHRONICLE_BOW.get());
                        output.accept(CareerItems.IRON_RUNIC_BLADE.get());
                        output.accept(CareerItems.IRON_HOLY_SIGIL.get());
                        output.accept(CareerItems.IRON_FROST_STAFF.get());
                        output.accept(CareerItems.IRON_SHADOW_DAGGER.get());
                        output.accept(CareerItems.IRON_SNARE_LAUNCHER.get());
                        output.accept(CareerItems.IRON_GUARDIAN_SHIELD.get());
                        output.accept(CareerItems.IRON_DARK_SCEPTER.get());
                        // Diamond tier
                        output.accept(CareerItems.DIAMOND_EMBER_STAFF.get());
                        output.accept(CareerItems.DIAMOND_CHRONICLE_BOW.get());
                        output.accept(CareerItems.DIAMOND_RUNIC_BLADE.get());
                        output.accept(CareerItems.DIAMOND_HOLY_SIGIL.get());
                        output.accept(CareerItems.DIAMOND_FROST_STAFF.get());
                        output.accept(CareerItems.DIAMOND_SHADOW_DAGGER.get());
                        output.accept(CareerItems.DIAMOND_DARK_SCEPTER.get());
                        output.accept(CareerItems.DIAMOND_GUARDIAN_MACE.get());
                        output.accept(CareerItems.DIAMOND_SNARE_LAUNCHER.get());
                        output.accept(CareerItems.DIAMOND_GUARDIAN_SHIELD.get());
                        // Epic weapons
                        output.accept(CareerItems.INFERNAL_SCEPTER.get());
                        output.accept(CareerItems.GLACIAL_HEART.get());
                        output.accept(CareerItems.WINDRUNNER.get());
                        output.accept(CareerItems.PREDATOR_BOW.get());
                        output.accept(CareerItems.BERSERKER_CLEAVER.get());
                        output.accept(CareerItems.DIVINE_CODEX.get());
                        output.accept(CareerItems.VIPER_FANG.get());
                        output.accept(CareerItems.LICH_PHYLACTERY.get());
                        output.accept(CareerItems.TITANS_MAUL.get());
                        // Legendary weapons
                        output.accept(CareerItems.STAFF_OF_AINZ.get());
                        output.accept(CareerItems.EXECUTIONERS_EDGE.get());
                        output.accept(CareerItems.ARK_OF_SALVATION.get());
                    })
                    .build());

    private CareerCreativeModeEvents() {
    }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CareerItems.CAREER_MANUAL);
        }
    }
}
