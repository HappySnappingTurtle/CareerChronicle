package com.hongyuwu.careerchronicle.registry;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CareerSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CareerChronicleMod.MOD_ID);

    public static final RegistryObject<SoundEvent> CAST_FIRE = sound("skill.cast.fire");
    public static final RegistryObject<SoundEvent> CAST_FROST = sound("skill.cast.frost");
    public static final RegistryObject<SoundEvent> CAST_HOLY = sound("skill.cast.holy");
    public static final RegistryObject<SoundEvent> CAST_DARK = sound("skill.cast.dark");
    public static final RegistryObject<SoundEvent> CAST_BLADE = sound("skill.cast.blade");
    public static final RegistryObject<SoundEvent> CAST_SHIELD = sound("skill.cast.shield");
    public static final RegistryObject<SoundEvent> CAST_ARROW = sound("skill.cast.arrow");
    public static final RegistryObject<SoundEvent> CAST_SHADOW = sound("skill.cast.shadow");
    public static final RegistryObject<SoundEvent> CAST_ARCANE = sound("skill.cast.arcane");
    public static final RegistryObject<SoundEvent> CAST_NATURE = sound("skill.cast.nature");

    public static final RegistryObject<SoundEvent> HIT_FIRE = sound("skill.hit.fire");
    public static final RegistryObject<SoundEvent> HIT_FROST = sound("skill.hit.frost");
    public static final RegistryObject<SoundEvent> HIT_HOLY = sound("skill.hit.holy");
    public static final RegistryObject<SoundEvent> HIT_DARK = sound("skill.hit.dark");
    public static final RegistryObject<SoundEvent> HIT_PHYSICAL = sound("skill.hit.physical");

    public static final RegistryObject<SoundEvent> UI_CHRONICLE_OPEN = sound("ui.chronicle_open");
    public static final RegistryObject<SoundEvent> UI_SKILL_EQUIP = sound("ui.skill_equip");
    public static final RegistryObject<SoundEvent> UI_TAB_FLIP = sound("ui.tab_flip");
    public static final RegistryObject<SoundEvent> UI_DENY = sound("ui.deny");

    public static final RegistryObject<SoundEvent> EVENT_LEVEL_UP = sound("event.level_up");
    public static final RegistryObject<SoundEvent> EVENT_SEGMENT_CHOICE = sound("event.segment_choice");
    public static final RegistryObject<SoundEvent> EVENT_FUSION_UNLOCK = sound("event.fusion_unlock");
    public static final RegistryObject<SoundEvent> EVENT_HIDDEN_UNLOCK = sound("event.hidden_unlock");
    public static final RegistryObject<SoundEvent> EVENT_SKILL_UPGRADE = sound("event.skill_upgrade");

    private CareerSounds() {
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, name);
        return SOUNDS.register(name.replace('.', '_'), () -> SoundEvent.createVariableRangeEvent(id));
    }
}
