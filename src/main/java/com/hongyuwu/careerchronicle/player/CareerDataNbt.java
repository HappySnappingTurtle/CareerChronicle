package com.hongyuwu.careerchronicle.player;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import java.util.Collection;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class CareerDataNbt {
    public static final ResourceLocation UNSELECTED_RACE =
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "unselected");

    public static final int DATA_VERSION = 1;
    public static final String KEY_DATA_VERSION = "dataVersion";
    public static final String KEY_RACE = "race";
    public static final String KEY_CAREER_LEVEL = "careerLevel";
    public static final String KEY_CAREER_XP = "careerXp";
    public static final String KEY_CLASS_HISTORY = "classHistory";
    public static final String KEY_UNLOCKED_SKILLS = "unlockedSkills";
    public static final String KEY_SKILL_LOADOUT = "skillLoadout";
    public static final String KEY_ULTIMATE_SLOT = "ultimateSlot";
    public static final String KEY_RACE_SLOT = "raceSlot";
    public static final String KEY_HIDDEN_FLAGS = "hiddenFlags";

    private CareerDataNbt() {
    }

    public static ResourceLocation readId(CompoundTag tag, String key, ResourceLocation fallback) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            return fallback;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString(key));
        return parsed == null ? fallback : parsed;
    }

    public static void putId(CompoundTag tag, String key, ResourceLocation id) {
        tag.putString(key, id.toString());
    }

    public static ListTag writeIdList(Collection<ResourceLocation> ids) {
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    public static ListTag writeNullableIdList(Collection<ResourceLocation> ids) {
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id == null ? "" : id.toString()));
        }
        return list;
    }

    public static void readIdList(CompoundTag tag, String key, Consumer<ResourceLocation> consumer) {
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation parsed = ResourceLocation.tryParse(list.getString(i));
            if (parsed != null) {
                consumer.accept(parsed);
            }
        }
    }

    public static void readNullableIdList(CompoundTag tag, String key, Consumer<ResourceLocation> consumer) {
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String value = list.getString(i);
            if (value.isBlank()) {
                consumer.accept(null);
                continue;
            }
            ResourceLocation parsed = ResourceLocation.tryParse(value);
            if (parsed != null) {
                consumer.accept(parsed);
            }
        }
    }
}
