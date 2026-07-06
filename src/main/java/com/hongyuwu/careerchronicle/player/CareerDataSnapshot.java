package com.hongyuwu.careerchronicle.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record CareerDataSnapshot(
        ResourceLocation race,
        int careerLevel,
        int careerXp,
        List<ResourceLocation> classHistory,
        Set<ResourceLocation> unlockedSkills,
        List<ResourceLocation> skillLoadout,
        ResourceLocation ultimateSlot,
        ResourceLocation raceSlot,
        Set<ResourceLocation> hiddenFlags,
        int mana,
        int maxMana,
        int stamina,
        int maxStamina,
        Map<ResourceLocation, Integer> cooldownTicks,
        Map<String, Integer> attributes,
        int unspentAttributePoints
) {
    public CareerDataSnapshot {
        race = race == null ? CareerDataNbt.UNSELECTED_RACE : race;
        careerLevel = Math.max(1, careerLevel);
        careerXp = Math.max(0, careerXp);
        classHistory = List.copyOf(classHistory);
        unlockedSkills = Collections.unmodifiableSet(new LinkedHashSet<>(unlockedSkills));
        skillLoadout = Collections.unmodifiableList(new ArrayList<>(skillLoadout));
        hiddenFlags = Collections.unmodifiableSet(new LinkedHashSet<>(hiddenFlags));
        mana = Math.max(0, mana);
        maxMana = Math.max(0, maxMana);
        stamina = Math.max(0, stamina);
        maxStamina = Math.max(0, maxStamina);
        cooldownTicks = Collections.unmodifiableMap(new LinkedHashMap<>(cooldownTicks));
        attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        unspentAttributePoints = Math.max(0, unspentAttributePoints);
    }

    public static CareerDataSnapshot empty() {
        return new CareerDataSnapshot(
                CareerDataNbt.UNSELECTED_RACE,
                1,
                0,
                List.of(),
                Set.of(),
                List.of(),
                null,
                null,
                Set.of(),
                0,
                0,
                0,
                0,
                Map.of(),
                Map.of(),
                0
        );
    }

    public static CareerDataSnapshot fromNbt(CompoundTag tag) {
        CareerData data = new CareerData();
        data.deserializePersistentData(tag);
        return new CareerDataSnapshot(
                data.getRace(),
                data.getCareerLevel(),
                data.getCareerXp(),
                data.getClassHistory(),
                data.getUnlockedSkills(),
                data.getSkillLoadout(),
                data.getUltimateSlot(),
                data.getRaceSlot(),
                data.getHiddenFlags(),
                tag.getInt("mana"),
                tag.getInt("maxMana"),
                tag.getInt("stamina"),
                tag.getInt("maxStamina"),
                readCooldowns(tag),
                readAttributes(tag),
                tag.getInt("unspentAttrPoints")
        );
    }

    private static Map<String, Integer> readAttributes(CompoundTag tag) {
        Map<String, Integer> attrs = new LinkedHashMap<>();
        if (tag.contains("attributes")) {
            CompoundTag attrsTag = tag.getCompound("attributes");
            for (String attr : CareerData.ALL_ATTRIBUTES) {
                attrs.put(attr, attrsTag.contains(attr) ? attrsTag.getInt(attr) : CareerData.BASE_ATTRIBUTE);
            }
        }
        return attrs;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        CareerDataNbt.putId(tag, CareerDataNbt.KEY_RACE, race);
        tag.putInt(CareerDataNbt.KEY_CAREER_LEVEL, careerLevel);
        tag.putInt(CareerDataNbt.KEY_CAREER_XP, careerXp);
        tag.put(CareerDataNbt.KEY_CLASS_HISTORY, CareerDataNbt.writeIdList(classHistory));
        tag.put(CareerDataNbt.KEY_UNLOCKED_SKILLS, CareerDataNbt.writeIdList(unlockedSkills));
        tag.put(CareerDataNbt.KEY_SKILL_LOADOUT, CareerDataNbt.writeNullableIdList(skillLoadout));
        if (ultimateSlot != null) {
            CareerDataNbt.putId(tag, CareerDataNbt.KEY_ULTIMATE_SLOT, ultimateSlot);
        }
        if (raceSlot != null) {
            CareerDataNbt.putId(tag, CareerDataNbt.KEY_RACE_SLOT, raceSlot);
        }
        tag.put(CareerDataNbt.KEY_HIDDEN_FLAGS, CareerDataNbt.writeIdList(hiddenFlags));
        tag.putInt("mana", mana);
        tag.putInt("maxMana", maxMana);
        tag.putInt("stamina", stamina);
        tag.putInt("maxStamina", maxStamina);
        CompoundTag cooldownsTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : cooldownTicks.entrySet()) {
            cooldownsTag.putInt(entry.getKey().toString(), Math.max(0, entry.getValue()));
        }
        tag.put("cooldownTicks", cooldownsTag);
        CompoundTag attrsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : attributes.entrySet()) {
            attrsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("attributes", attrsTag);
        tag.putInt("unspentAttrPoints", unspentAttributePoints);
        return tag;
    }

    private static Map<ResourceLocation, Integer> readCooldowns(CompoundTag tag) {
        Map<ResourceLocation, Integer> cooldowns = new LinkedHashMap<>();
        CompoundTag cooldownsTag = tag.getCompound("cooldownTicks");
        for (String key : cooldownsTag.getAllKeys()) {
            ResourceLocation skillId = ResourceLocation.tryParse(key);
            int ticks = cooldownsTag.getInt(key);
            if (skillId != null && ticks > 0) {
                cooldowns.put(skillId, ticks);
            }
        }
        return cooldowns;
    }
}
