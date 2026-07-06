package com.hongyuwu.careerchronicle.data;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.registry.CareerItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;

public class CareerItemModelProvider extends ItemModelProvider {
    public CareerItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CareerChronicleMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Materials
        simpleItem(CareerItems.ARCANE_SHARD);
        simpleItem(CareerItems.FROST_CRYSTAL);
        simpleItem(CareerItems.DARK_ESSENCE);
        simpleItem(CareerItems.HOLY_RELIC);
        simpleItem(CareerItems.CAREER_INGOT);
        simpleItem(CareerItems.EMBER_CORE);

        // Base weapons with custom display are hand-written (ember_staff, chronicle_recurve, runic_blade, snare_launcher, sunlit_sigil)

        // Simple weapon models (no custom display transforms)
        simpleItem(CareerItems.FROST_STAFF);
        simpleItem(CareerItems.SHADOW_DAGGER);
        simpleItem(CareerItems.GUARDIAN_SHIELD);
        simpleItem(CareerItems.DARK_SCEPTER);

        // Iron tier
        simpleItem(CareerItems.IRON_EMBER_STAFF);
        simpleItem(CareerItems.IRON_CHRONICLE_BOW);
        simpleItem(CareerItems.IRON_RUNIC_BLADE);
        simpleItem(CareerItems.IRON_HOLY_SIGIL);
        simpleItem(CareerItems.IRON_FROST_STAFF);
        simpleItem(CareerItems.IRON_SHADOW_DAGGER);
        simpleItem(CareerItems.IRON_SNARE_LAUNCHER);
        simpleItem(CareerItems.IRON_GUARDIAN_SHIELD);
        simpleItem(CareerItems.IRON_DARK_SCEPTER);

        // Diamond tier
        simpleItem(CareerItems.DIAMOND_EMBER_STAFF);
        simpleItem(CareerItems.DIAMOND_CHRONICLE_BOW);
        simpleItem(CareerItems.DIAMOND_RUNIC_BLADE);
        simpleItem(CareerItems.DIAMOND_HOLY_SIGIL);
        simpleItem(CareerItems.DIAMOND_FROST_STAFF);
        simpleItem(CareerItems.DIAMOND_SHADOW_DAGGER);
        simpleItem(CareerItems.DIAMOND_DARK_SCEPTER);
        simpleItem(CareerItems.DIAMOND_GUARDIAN_MACE);
        simpleItem(CareerItems.DIAMOND_SNARE_LAUNCHER);
        simpleItem(CareerItems.DIAMOND_GUARDIAN_SHIELD);

        // Epic weapons
        simpleItem(CareerItems.INFERNAL_SCEPTER);
        simpleItem(CareerItems.GLACIAL_HEART);
        simpleItem(CareerItems.WINDRUNNER);
        simpleItem(CareerItems.PREDATOR_BOW);
        simpleItem(CareerItems.BERSERKER_CLEAVER);
        simpleItem(CareerItems.DIVINE_CODEX);
        simpleItem(CareerItems.VIPER_FANG);
        simpleItem(CareerItems.LICH_PHYLACTERY);
        simpleItem(CareerItems.TITANS_MAUL);

        // Legendary weapons
        simpleItem(CareerItems.STAFF_OF_AINZ);
        simpleItem(CareerItems.EXECUTIONERS_EDGE);
        simpleItem(CareerItems.ARK_OF_SALVATION);

        // Career manual uses minecraft:item/book texture — hand-written model
    }

    private void simpleItem(RegistryObject<Item> item) {
        withExistingParent(item.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + item.getId().getPath()));
    }
}
