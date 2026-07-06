package com.hongyuwu.careerchronicle.registry;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.item.CareerManualItem;
import com.hongyuwu.careerchronicle.item.LegendaryWeaponItem;
import com.hongyuwu.careerchronicle.item.SkillWeaponItem;
import com.hongyuwu.careerchronicle.item.WeaponPassives;
import com.hongyuwu.careerchronicle.item.WeaponTier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CareerItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CareerChronicleMod.MOD_ID);

    public static final RegistryObject<Item> CAREER_MANUAL = ITEMS.register(
            "career_manual",
            () -> new CareerManualItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> EMBER_STAFF = ITEMS.register(
            "ember_staff",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(192),
                    skill("fireball"))
    );
    public static final RegistryObject<Item> CHRONICLE_RECURVE = ITEMS.register(
            "chronicle_recurve",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(256),
                    skill("charged_shot"))
    );
    public static final RegistryObject<Item> SNARE_LAUNCHER = ITEMS.register(
            "snare_launcher",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(224),
                    skill("snare_shot"))
    );
    public static final RegistryObject<Item> RUNIC_BLADE = ITEMS.register(
            "runic_blade",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(260),
                    skill("lunge_strike"))
    );
    public static final RegistryObject<Item> SUNLIT_SIGIL = ITEMS.register(
            "sunlit_sigil",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(180),
                    skill("mend"))
    );
    // Base class weapons (Common tier)
    public static final RegistryObject<Item> FROST_STAFF = ITEMS.register(
            "frost_staff",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(192), skill("frost_bolt")));
    public static final RegistryObject<Item> SHADOW_DAGGER = ITEMS.register(
            "shadow_dagger",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(200), skill("shadow_strike")));
    public static final RegistryObject<Item> GUARDIAN_SHIELD = ITEMS.register(
            "guardian_shield",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(240), skill("shield_wall")));
    public static final RegistryObject<Item> DARK_SCEPTER = ITEMS.register(
            "dark_scepter",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(192), skill("soul_drain")));

    public static final RegistryObject<Item> EMBER_CORE = ITEMS.register(
            "ember_core",
            () -> new Item(new Item.Properties())
    );

    // --- Tier upgrade materials ---
    public static final RegistryObject<Item> ARCANE_SHARD = ITEMS.register(
            "arcane_shard", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FROST_CRYSTAL = ITEMS.register(
            "frost_crystal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DARK_ESSENCE = ITEMS.register(
            "dark_essence", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HOLY_RELIC = ITEMS.register(
            "holy_relic", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAREER_INGOT = ITEMS.register(
            "career_ingot", () -> new Item(new Item.Properties()));

    // --- Uncommon tier weapons (iron-based upgrades) ---
    public static final RegistryObject<Item> IRON_EMBER_STAFF = ITEMS.register(
            "iron_ember_staff",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(320), skill("fireball")));
    public static final RegistryObject<Item> IRON_CHRONICLE_BOW = ITEMS.register(
            "iron_chronicle_bow",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(384), skill("charged_shot")));
    public static final RegistryObject<Item> IRON_RUNIC_BLADE = ITEMS.register(
            "iron_runic_blade",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(400), skill("lunge_strike")));
    public static final RegistryObject<Item> IRON_HOLY_SIGIL = ITEMS.register(
            "iron_holy_sigil",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(280), skill("mend")));
    public static final RegistryObject<Item> IRON_FROST_STAFF = ITEMS.register(
            "iron_frost_staff",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(320), skill("frost_bolt")));
    public static final RegistryObject<Item> IRON_SHADOW_DAGGER = ITEMS.register(
            "iron_shadow_dagger",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(350), skill("shadow_strike")));
    public static final RegistryObject<Item> IRON_SNARE_LAUNCHER = ITEMS.register(
            "iron_snare_launcher",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(340), skill("snare_shot")));
    public static final RegistryObject<Item> IRON_GUARDIAN_SHIELD = ITEMS.register(
            "iron_guardian_shield",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(380), skill("shield_wall")));
    public static final RegistryObject<Item> IRON_DARK_SCEPTER = ITEMS.register(
            "iron_dark_scepter",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(320), skill("soul_drain")));

    // --- Rare tier weapons (diamond-based) ---
    public static final RegistryObject<Item> DIAMOND_EMBER_STAFF = ITEMS.register(
            "diamond_ember_staff",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(560), skill("fireball")));
    public static final RegistryObject<Item> DIAMOND_CHRONICLE_BOW = ITEMS.register(
            "diamond_chronicle_bow",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(640), skill("charged_shot")));
    public static final RegistryObject<Item> DIAMOND_RUNIC_BLADE = ITEMS.register(
            "diamond_runic_blade",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(680), skill("lunge_strike")));
    public static final RegistryObject<Item> DIAMOND_HOLY_SIGIL = ITEMS.register(
            "diamond_holy_sigil",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(480), skill("mend")));
    public static final RegistryObject<Item> DIAMOND_FROST_STAFF = ITEMS.register(
            "diamond_frost_staff",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(560), skill("frost_bolt")));
    public static final RegistryObject<Item> DIAMOND_SHADOW_DAGGER = ITEMS.register(
            "diamond_shadow_dagger",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(600), skill("shadow_strike")));
    public static final RegistryObject<Item> DIAMOND_DARK_SCEPTER = ITEMS.register(
            "diamond_dark_scepter",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(560), skill("soul_drain")));
    public static final RegistryObject<Item> DIAMOND_GUARDIAN_MACE = ITEMS.register(
            "diamond_guardian_mace",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(700), skill("aegis_smash")));
    public static final RegistryObject<Item> DIAMOND_SNARE_LAUNCHER = ITEMS.register(
            "diamond_snare_launcher",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(580), skill("snare_shot")));
    public static final RegistryObject<Item> DIAMOND_GUARDIAN_SHIELD = ITEMS.register(
            "diamond_guardian_shield",
            () -> new SkillWeaponItem(new Item.Properties().stacksTo(1).durability(660), skill("shield_wall")));

    // ====== Epic tier special weapons ======

    // Staves
    public static final RegistryObject<Item> INFERNAL_SCEPTER = ITEMS.register("infernal_scepter",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(800).fireResistant(),
                    skill("meteor_rite"), WeaponTier.EPIC, WeaponPassives.INFERNAL_DOMINION,
                    "careerchronicle.passive.infernal_dominion"));
    public static final RegistryObject<Item> GLACIAL_HEART = ITEMS.register("glacial_heart",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(800),
                    skill("absolute_zero"), WeaponTier.EPIC, WeaponPassives.ABSOLUTE_FROST,
                    "careerchronicle.passive.absolute_frost"));

    // Bows
    public static final RegistryObject<Item> WINDRUNNER = ITEMS.register("windrunner",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(720),
                    skill("storm_marksman"), WeaponTier.EPIC, WeaponPassives.WIND_WALKER,
                    "careerchronicle.passive.wind_walker"));
    public static final RegistryObject<Item> PREDATOR_BOW = ITEMS.register("predator_bow",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(700),
                    skill("eagle_eye"), WeaponTier.EPIC, WeaponPassives.PREDATOR_MARK,
                    "careerchronicle.passive.predator_mark"));

    // Swords
    public static final RegistryObject<Item> BERSERKER_CLEAVER = ITEMS.register("berserker_cleaver",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(900),
                    skill("unyielding_colossus"), WeaponTier.EPIC, WeaponPassives.BERSERKER_RAGE,
                    "careerchronicle.passive.berserker_rage"));

    // Sigils
    public static final RegistryObject<Item> DIVINE_CODEX = ITEMS.register("divine_codex",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(600),
                    skill("sanctuary_descent"), WeaponTier.EPIC, WeaponPassives.DIVINE_GRACE,
                    "careerchronicle.passive.divine_grace"));

    // Daggers
    public static final RegistryObject<Item> VIPER_FANG = ITEMS.register("viper_fang",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(650),
                    skill("death_blossom"), WeaponTier.EPIC, WeaponPassives.SHADOW_STEP,
                    "careerchronicle.passive.shadow_step"));

    // Dark Scepters
    public static final RegistryObject<Item> LICH_PHYLACTERY = ITEMS.register("lich_phylactery",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(750),
                    skill("lich_form"), WeaponTier.EPIC, WeaponPassives.SOUL_HARVEST,
                    "careerchronicle.passive.soul_harvest"));

    // Maces
    public static final RegistryObject<Item> TITANS_MAUL = ITEMS.register("titans_maul",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(950),
                    skill("impregnable_fortress"), WeaponTier.EPIC, WeaponPassives.EARTHQUAKE,
                    "careerchronicle.passive.earthquake"));

    // ====== Legendary tier weapons (one per weapon line) ======

    public static final RegistryObject<Item> STAFF_OF_AINZ = ITEMS.register("staff_of_ainz",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(2000).fireResistant(),
                    skill("hellfire"), WeaponTier.LEGENDARY, WeaponPassives.LIFE_DRAIN,
                    "careerchronicle.passive.life_drain"));
    public static final RegistryObject<Item> EXECUTIONERS_EDGE = ITEMS.register("executioners_edge",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(1800).fireResistant(),
                    skill("death_strike"), WeaponTier.LEGENDARY, WeaponPassives.DEATH_SENTENCE,
                    "careerchronicle.passive.death_sentence"));
    public static final RegistryObject<Item> ARK_OF_SALVATION = ITEMS.register("ark_of_salvation",
            () -> new LegendaryWeaponItem(new Item.Properties().stacksTo(1).durability(1500).fireResistant(),
                    skill("twilight_mend"), WeaponTier.LEGENDARY, WeaponPassives.MARTYR_BLESSING,
                    "careerchronicle.passive.martyr_blessing"));

    private CareerItems() {
    }

    private static ResourceLocation skill(String path) {
        return ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, path);
    }
}
