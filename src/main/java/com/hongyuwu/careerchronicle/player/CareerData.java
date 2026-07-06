package com.hongyuwu.careerchronicle.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class CareerData implements ICareerData {
    public static final String ATTR_STR = "str";
    public static final String ATTR_DEX = "dex";
    public static final String ATTR_INT = "int";
    public static final String ATTR_WIS = "wis";
    public static final String ATTR_CON = "con";
    public static final String[] ALL_ATTRIBUTES = {ATTR_STR, ATTR_DEX, ATTR_INT, ATTR_WIS, ATTR_CON};
    public static final int POINTS_PER_LEVEL = 3;
    public static final int BASE_ATTRIBUTE = 5;

    private final List<ResourceLocation> classHistory = new ArrayList<>();
    private final Set<ResourceLocation> unlockedSkills = new LinkedHashSet<>();
    private final List<ResourceLocation> skillLoadout = new ArrayList<>();
    private final Set<ResourceLocation> hiddenFlags = new LinkedHashSet<>();
    private final CareerRuntimeState runtimeState = new CareerRuntimeState();
    private final java.util.Map<String, Integer> attributes = new java.util.HashMap<>();
    private ResourceLocation race = CareerDataNbt.UNSELECTED_RACE;
    private ResourceLocation ultimateSlot;
    private ResourceLocation raceSlot;
    private int careerLevel = 1;
    private int careerXp;
    private int unspentAttributePoints;

    @Override
    public ResourceLocation getRace() {
        return race;
    }

    @Override
    public void setRace(ResourceLocation race) {
        this.race = race == null ? CareerDataNbt.UNSELECTED_RACE : race;
    }

    @Override
    public int getCareerLevel() {
        return careerLevel;
    }

    @Override
    public void setCareerLevel(int careerLevel) {
        this.careerLevel = Math.max(1, careerLevel);
    }

    @Override
    public int getCareerXp() {
        return careerXp;
    }

    @Override
    public void setCareerXp(int careerXp) {
        this.careerXp = Math.max(0, careerXp);
    }

    @Override
    public void addCareerXp(int amount) {
        setCareerXp(careerXp + amount);
    }

    @Override
    public List<ResourceLocation> getClassHistory() {
        return Collections.unmodifiableList(classHistory);
    }

    @Override
    public void addClassHistory(ResourceLocation classId) {
        if (classId != null) {
            classHistory.add(classId);
        }
    }

    @Override
    public Set<ResourceLocation> getUnlockedSkills() {
        return Collections.unmodifiableSet(unlockedSkills);
    }

    @Override
    public boolean unlockSkill(ResourceLocation skillId) {
        return skillId != null && unlockedSkills.add(skillId);
    }

    @Override
    public boolean retainUnlockedSkills(Set<ResourceLocation> skillIds) {
        return unlockedSkills.retainAll(skillIds);
    }

    @Override
    public List<ResourceLocation> getSkillLoadout() {
        return Collections.unmodifiableList(skillLoadout);
    }

    @Override
    public void setSkillLoadout(List<ResourceLocation> skillLoadout) {
        this.skillLoadout.clear();
        int end = skillLoadout.size();
        while (end > 0 && skillLoadout.get(end - 1) == null) {
            end--;
        }
        for (int i = 0; i < end; i++) {
            this.skillLoadout.add(skillLoadout.get(i));
        }
    }

    @Override
    public ResourceLocation getUltimateSlot() {
        return ultimateSlot;
    }

    @Override
    public void setUltimateSlot(ResourceLocation skillId) {
        this.ultimateSlot = skillId;
    }

    @Override
    public ResourceLocation getRaceSlot() {
        return raceSlot;
    }

    @Override
    public void setRaceSlot(ResourceLocation skillId) {
        this.raceSlot = skillId;
    }

    @Override
    public Set<ResourceLocation> getHiddenFlags() {
        return Collections.unmodifiableSet(hiddenFlags);
    }

    @Override
    public void setHiddenFlag(ResourceLocation flagId, boolean enabled) {
        if (flagId == null) {
            return;
        }
        if (enabled) {
            hiddenFlags.add(flagId);
        } else {
            hiddenFlags.remove(flagId);
        }
    }

    @Override
    public int getAttribute(String attr) {
        return attributes.getOrDefault(attr, BASE_ATTRIBUTE);
    }

    @Override
    public void setAttribute(String attr, int value) {
        attributes.put(attr, Math.max(0, value));
    }

    @Override
    public int getUnspentAttributePoints() {
        return unspentAttributePoints;
    }

    @Override
    public void setUnspentAttributePoints(int points) {
        this.unspentAttributePoints = Math.max(0, points);
    }

    public CareerRuntimeState getRuntimeState() {
        return runtimeState;
    }

    @Override
    public CompoundTag serializePersistentData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(CareerDataNbt.KEY_DATA_VERSION, CareerDataNbt.DATA_VERSION);
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
        CompoundTag attrsTag = new CompoundTag();
        for (String attr : ALL_ATTRIBUTES) {
            attrsTag.putInt(attr, getAttribute(attr));
        }
        tag.put("attributes", attrsTag);
        tag.putInt("unspentAttrPoints", unspentAttributePoints);
        return tag;
    }

    @Override
    public void deserializePersistentData(CompoundTag tag) {
        race = CareerDataNbt.readId(tag, CareerDataNbt.KEY_RACE, CareerDataNbt.UNSELECTED_RACE);
        careerLevel = Math.max(1, tag.getInt(CareerDataNbt.KEY_CAREER_LEVEL));
        careerXp = Math.max(0, tag.getInt(CareerDataNbt.KEY_CAREER_XP));

        classHistory.clear();
        unlockedSkills.clear();
        skillLoadout.clear();
        hiddenFlags.clear();

        CareerDataNbt.readIdList(tag, CareerDataNbt.KEY_CLASS_HISTORY, classHistory::add);
        CareerDataNbt.readIdList(tag, CareerDataNbt.KEY_UNLOCKED_SKILLS, unlockedSkills::add);
        CareerDataNbt.readNullableIdList(tag, CareerDataNbt.KEY_SKILL_LOADOUT, skillLoadout::add);
        ultimateSlot = CareerDataNbt.readId(tag, CareerDataNbt.KEY_ULTIMATE_SLOT, null);
        raceSlot = CareerDataNbt.readId(tag, CareerDataNbt.KEY_RACE_SLOT, null);
        CareerDataNbt.readIdList(tag, CareerDataNbt.KEY_HIDDEN_FLAGS, hiddenFlags::add);
        attributes.clear();
        if (tag.contains("attributes")) {
            CompoundTag attrsTag = tag.getCompound("attributes");
            for (String attr : ALL_ATTRIBUTES) {
                if (attrsTag.contains(attr)) {
                    attributes.put(attr, attrsTag.getInt(attr));
                }
            }
        }
        unspentAttributePoints = Math.max(0, tag.getInt("unspentAttrPoints"));
    }

    @Override
    public void copyPersistentFrom(ICareerData source) {
        deserializePersistentData(source.serializePersistentData());
    }

    @Override
    public CareerDataSnapshot snapshot() {
        return new CareerDataSnapshot(
                race,
                careerLevel,
                careerXp,
                classHistory,
                unlockedSkills,
                skillLoadout,
                ultimateSlot,
                raceSlot,
                hiddenFlags,
                runtimeState.getMana(),
                runtimeState.getMaxMana(),
                runtimeState.getStamina(),
                runtimeState.getMaxStamina(),
                runtimeState.getCooldownTicks(),
                java.util.Map.copyOf(attributes),
                unspentAttributePoints
        );
    }
}
