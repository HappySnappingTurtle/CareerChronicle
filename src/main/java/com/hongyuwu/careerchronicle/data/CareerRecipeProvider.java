package com.hongyuwu.careerchronicle.data;

import com.hongyuwu.careerchronicle.registry.CareerItems;
import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

public class CareerRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public CareerRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        // === Materials ===
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CareerItems.EMBER_CORE.get())
                .requires(Items.BLAZE_POWDER).requires(Items.MAGMA_CREAM)
                .unlockedBy("has_blaze_powder", has(Items.BLAZE_POWDER)).save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CareerItems.ARCANE_SHARD.get())
                .requires(Items.AMETHYST_SHARD).requires(Items.BLAZE_POWDER)
                .unlockedBy("has_amethyst", has(Items.AMETHYST_SHARD)).save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CareerItems.FROST_CRYSTAL.get())
                .requires(Items.AMETHYST_SHARD).requires(Items.SNOWBALL).requires(Items.PRISMARINE_SHARD)
                .unlockedBy("has_amethyst", has(Items.AMETHYST_SHARD)).save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CareerItems.DARK_ESSENCE.get())
                .requires(Items.BLAZE_POWDER).requires(Items.COAL).requires(Items.BONE)
                .unlockedBy("has_blaze_powder", has(Items.BLAZE_POWDER)).save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CareerItems.HOLY_RELIC.get())
                .requires(Items.GOLD_INGOT).requires(Items.GLOWSTONE_DUST).requires(Items.GHAST_TEAR)
                .unlockedBy("has_ghast_tear", has(Items.GHAST_TEAR)).save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CareerItems.CAREER_INGOT.get())
                .requires(Items.IRON_INGOT).requires(Items.GOLD_INGOT).requires(Items.LAPIS_LAZULI)
                .unlockedBy("has_gold", has(Items.GOLD_INGOT)).save(writer);

        // === Base tier shaped weapons ===
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, CareerItems.EMBER_STAFF.get())
                .pattern(" E ").pattern(" S ").pattern(" S ")
                .define('E', CareerItems.EMBER_CORE.get()).define('S', Items.STICK)
                .unlockedBy("has_ember_core", has(CareerItems.EMBER_CORE.get())).save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, CareerItems.CHRONICLE_RECURVE.get())
                .pattern(" SI").pattern("S I").pattern(" SI")
                .define('S', Items.STICK).define('I', Items.STRING)
                .unlockedBy("has_string", has(Items.STRING)).save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, CareerItems.RUNIC_BLADE.get())
                .pattern(" I ").pattern(" I ").pattern(" S ")
                .define('I', Items.IRON_INGOT).define('S', Items.STICK)
                .unlockedBy("has_iron", has(Items.IRON_INGOT)).save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, CareerItems.SNARE_LAUNCHER.get())
                .pattern("ISI").pattern(" S ").pattern(" S ")
                .define('I', Items.IRON_INGOT).define('S', Items.STICK)
                .unlockedBy("has_iron", has(Items.IRON_INGOT)).save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, CareerItems.SUNLIT_SIGIL.get())
                .pattern(" G ").pattern("GRG").pattern(" G ")
                .define('G', Items.GOLD_INGOT).define('R', CareerItems.HOLY_RELIC.get())
                .unlockedBy("has_holy_relic", has(CareerItems.HOLY_RELIC.get())).save(writer);

        // === Iron tier upgrades (shapeless) ===
        upgrade(writer, CareerItems.EMBER_STAFF.get(), Items.IRON_INGOT, CareerItems.ARCANE_SHARD.get(), CareerItems.IRON_EMBER_STAFF.get());
        upgrade(writer, CareerItems.CHRONICLE_RECURVE.get(), Items.IRON_INGOT, CareerItems.CAREER_INGOT.get(), CareerItems.IRON_CHRONICLE_BOW.get());
        upgrade(writer, CareerItems.RUNIC_BLADE.get(), Items.IRON_INGOT, CareerItems.CAREER_INGOT.get(), CareerItems.IRON_RUNIC_BLADE.get());
        upgrade(writer, CareerItems.SUNLIT_SIGIL.get(), Items.IRON_INGOT, CareerItems.HOLY_RELIC.get(), CareerItems.IRON_HOLY_SIGIL.get());

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, CareerItems.IRON_FROST_STAFF.get())
                .requires(Items.STICK).requires(Items.IRON_INGOT)
                .requires(CareerItems.FROST_CRYSTAL.get()).requires(CareerItems.FROST_CRYSTAL.get())
                .unlockedBy("has_frost_crystal", has(CareerItems.FROST_CRYSTAL.get())).save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, CareerItems.IRON_SHADOW_DAGGER.get())
                .requires(Items.IRON_INGOT).requires(CareerItems.DARK_ESSENCE.get()).requires(CareerItems.CAREER_INGOT.get())
                .unlockedBy("has_dark_essence", has(CareerItems.DARK_ESSENCE.get())).save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, CareerItems.IRON_SNARE_LAUNCHER.get())
                .requires(CareerItems.SNARE_LAUNCHER.get()).requires(Items.IRON_INGOT).requires(CareerItems.CAREER_INGOT.get())
                .unlockedBy("has_snare_launcher", has(CareerItems.SNARE_LAUNCHER.get())).save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, CareerItems.IRON_GUARDIAN_SHIELD.get())
                .requires(CareerItems.GUARDIAN_SHIELD.get()).requires(Items.IRON_INGOT).requires(CareerItems.CAREER_INGOT.get())
                .unlockedBy("has_guardian_shield", has(CareerItems.GUARDIAN_SHIELD.get())).save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, CareerItems.IRON_DARK_SCEPTER.get())
                .requires(CareerItems.DARK_SCEPTER.get()).requires(Items.IRON_INGOT).requires(CareerItems.DARK_ESSENCE.get())
                .unlockedBy("has_dark_scepter", has(CareerItems.DARK_SCEPTER.get())).save(writer);

        // === Diamond tier upgrades (shapeless) ===
        upgrade(writer, CareerItems.IRON_EMBER_STAFF.get(), Items.DIAMOND, CareerItems.FROST_CRYSTAL.get(), CareerItems.DIAMOND_EMBER_STAFF.get());
        upgrade(writer, CareerItems.IRON_CHRONICLE_BOW.get(), Items.DIAMOND, CareerItems.CAREER_INGOT.get(), CareerItems.DIAMOND_CHRONICLE_BOW.get());
        upgrade(writer, CareerItems.IRON_RUNIC_BLADE.get(), Items.DIAMOND, CareerItems.CAREER_INGOT.get(), CareerItems.DIAMOND_RUNIC_BLADE.get());
        upgrade(writer, CareerItems.IRON_HOLY_SIGIL.get(), Items.DIAMOND, CareerItems.HOLY_RELIC.get(), CareerItems.DIAMOND_HOLY_SIGIL.get());
        upgrade(writer, CareerItems.IRON_FROST_STAFF.get(), Items.DIAMOND, CareerItems.FROST_CRYSTAL.get(), CareerItems.DIAMOND_FROST_STAFF.get());
        upgrade(writer, CareerItems.IRON_SHADOW_DAGGER.get(), Items.DIAMOND, CareerItems.DARK_ESSENCE.get(), CareerItems.DIAMOND_SHADOW_DAGGER.get());

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, CareerItems.DIAMOND_DARK_SCEPTER.get())
                .requires(Items.DIAMOND).requires(CareerItems.DARK_ESSENCE.get())
                .requires(CareerItems.DARK_ESSENCE.get()).requires(CareerItems.ARCANE_SHARD.get())
                .unlockedBy("has_diamond", has(Items.DIAMOND)).save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, CareerItems.DIAMOND_GUARDIAN_MACE.get())
                .requires(Items.DIAMOND).requires(CareerItems.CAREER_INGOT.get())
                .requires(CareerItems.CAREER_INGOT.get()).requires(CareerItems.HOLY_RELIC.get())
                .unlockedBy("has_diamond", has(Items.DIAMOND)).save(writer);
    }

    private void upgrade(Consumer<FinishedRecipe> writer,
                         net.minecraft.world.item.Item base,
                         net.minecraft.world.item.Item upgradeItem,
                         net.minecraft.world.item.Item catalyst,
                         net.minecraft.world.item.Item result) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, result)
                .requires(base).requires(upgradeItem).requires(catalyst)
                .unlockedBy("has_base", has(base)).save(writer);
    }
}
