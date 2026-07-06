package com.hongyuwu.careerchronicle.player;

import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public interface ICareerData {
    ResourceLocation getRace();

    void setRace(ResourceLocation race);

    int getCareerLevel();

    void setCareerLevel(int careerLevel);

    int getCareerXp();

    void setCareerXp(int careerXp);

    void addCareerXp(int amount);

    List<ResourceLocation> getClassHistory();

    void addClassHistory(ResourceLocation classId);

    Set<ResourceLocation> getUnlockedSkills();

    boolean unlockSkill(ResourceLocation skillId);

    boolean retainUnlockedSkills(Set<ResourceLocation> skillIds);

    List<ResourceLocation> getSkillLoadout();

    void setSkillLoadout(List<ResourceLocation> skillLoadout);

    ResourceLocation getUltimateSlot();

    void setUltimateSlot(ResourceLocation skillId);

    ResourceLocation getRaceSlot();

    void setRaceSlot(ResourceLocation skillId);

    Set<ResourceLocation> getHiddenFlags();

    void setHiddenFlag(ResourceLocation flagId, boolean enabled);

    int getAttribute(String attr);

    void setAttribute(String attr, int value);

    int getUnspentAttributePoints();

    void setUnspentAttributePoints(int points);

    CareerRuntimeState getRuntimeState();

    CompoundTag serializePersistentData();

    void deserializePersistentData(CompoundTag tag);

    void copyPersistentFrom(ICareerData source);

    CareerDataSnapshot snapshot();
}
