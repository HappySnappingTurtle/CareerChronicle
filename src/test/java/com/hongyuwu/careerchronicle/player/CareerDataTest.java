package com.hongyuwu.careerchronicle.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CareerDataTest {

    private CareerData data;

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("careerchronicle", path);
    }

    @BeforeEach
    void setUp() {
        data = new CareerData();
    }

    // --- Attribute defaults ---

    @Test
    void allAttributesDefaultToBaseAttribute() {
        for (String attr : CareerData.ALL_ATTRIBUTES) {
            assertEquals(CareerData.BASE_ATTRIBUTE, data.getAttribute(attr),
                    "Attribute '" + attr + "' should default to BASE_ATTRIBUTE (" + CareerData.BASE_ATTRIBUTE + ")");
        }
    }

    // --- setAttribute / getAttribute roundtrip ---

    @Test
    void setAttributeAndGetAttribute_roundtrip() {
        data.setAttribute(CareerData.ATTR_STR, 15);
        assertEquals(15, data.getAttribute(CareerData.ATTR_STR));
    }

    @Test
    void setAttribute_negativeClampedToZero() {
        data.setAttribute(CareerData.ATTR_DEX, -5);
        assertEquals(0, data.getAttribute(CareerData.ATTR_DEX));
    }

    // --- Unspent attribute points ---

    @Test
    void unspentAttributePoints_setAndGet() {
        data.setUnspentAttributePoints(10);
        assertEquals(10, data.getUnspentAttributePoints());
    }

    @Test
    void unspentAttributePoints_negativeClampedToZero() {
        data.setUnspentAttributePoints(-3);
        assertEquals(0, data.getUnspentAttributePoints());
    }

    // --- Skill unlock ---

    @Test
    void unlockSkill_firstAddReturnsTrue() {
        assertTrue(data.unlockSkill(rl("fireball")));
    }

    @Test
    void unlockSkill_duplicateReturnsFalse() {
        ResourceLocation skillId = rl("fireball");
        data.unlockSkill(skillId);
        assertFalse(data.unlockSkill(skillId),
                "Duplicate unlock should return false");
    }

    @Test
    void unlockSkill_nullReturnsFalse() {
        assertFalse(data.unlockSkill(null));
    }

    // --- Class history ---

    @Test
    void addClassHistory_appearsInGetClassHistory() {
        ResourceLocation warrior = rl("warrior");
        data.addClassHistory(warrior);
        assertTrue(data.getClassHistory().contains(warrior));
    }

    @Test
    void addClassHistory_preservesOrder() {
        ResourceLocation warrior = rl("warrior");
        ResourceLocation mage = rl("fire_mage");
        data.addClassHistory(warrior);
        data.addClassHistory(mage);
        assertEquals(List.of(warrior, mage), data.getClassHistory());
    }

    @Test
    void addClassHistory_nullIgnored() {
        data.addClassHistory(null);
        assertTrue(data.getClassHistory().isEmpty());
    }

    // --- Race set/get ---

    @Test
    void setRaceAndGetRace_roundtrip() {
        ResourceLocation elf = rl("elf");
        data.setRace(elf);
        assertEquals(elf, data.getRace());
    }

    @Test
    void setRace_nullFallsBackToUnselected() {
        data.setRace(null);
        assertEquals(CareerDataNbt.UNSELECTED_RACE, data.getRace());
    }

    // --- Hidden flags ---

    @Test
    void setHiddenFlag_trueAddsFlag() {
        ResourceLocation flagId = rl("tutorial_done");
        data.setHiddenFlag(flagId, true);
        assertTrue(data.getHiddenFlags().contains(flagId));
    }

    @Test
    void setHiddenFlag_falseRemovesFlag() {
        ResourceLocation flagId = rl("tutorial_done");
        data.setHiddenFlag(flagId, true);
        data.setHiddenFlag(flagId, false);
        assertFalse(data.getHiddenFlags().contains(flagId));
    }

    @Test
    void setHiddenFlag_nullIgnored() {
        data.setHiddenFlag(null, true);
        assertTrue(data.getHiddenFlags().isEmpty());
    }

    // --- NBT round-trip ---

    @Test
    void nbtRoundtrip_allFieldsPreserved() {
        ResourceLocation elf = rl("elf");
        ResourceLocation warrior = rl("warrior");
        ResourceLocation fireMage = rl("fire_mage");
        ResourceLocation fireball = rl("fireball");
        ResourceLocation slash = rl("slash");
        ResourceLocation ultimate = rl("dragon_breath");
        ResourceLocation raceSkill = rl("elven_sight");
        ResourceLocation flag = rl("intro_seen");

        data.setRace(elf);
        data.setCareerLevel(5);
        data.setCareerXp(1200);
        data.addClassHistory(warrior);
        data.addClassHistory(fireMage);
        data.addClassHistory(warrior);
        data.unlockSkill(fireball);
        data.unlockSkill(slash);
        data.setSkillLoadout(List.of(fireball, slash));
        data.setUltimateSlot(ultimate);
        data.setRaceSlot(raceSkill);
        data.setHiddenFlag(flag, true);
        data.setAttribute(CareerData.ATTR_STR, 18);
        data.setAttribute(CareerData.ATTR_DEX, 12);
        data.setAttribute(CareerData.ATTR_INT, 8);
        data.setAttribute(CareerData.ATTR_WIS, 14);
        data.setAttribute(CareerData.ATTR_CON, 10);
        data.setUnspentAttributePoints(7);

        CompoundTag tag = data.serializePersistentData();

        CareerData restored = new CareerData();
        restored.deserializePersistentData(tag);

        assertEquals(elf, restored.getRace());
        assertEquals(5, restored.getCareerLevel());
        assertEquals(1200, restored.getCareerXp());
        assertEquals(List.of(warrior, fireMage, warrior), restored.getClassHistory());
        assertTrue(restored.getUnlockedSkills().contains(fireball));
        assertTrue(restored.getUnlockedSkills().contains(slash));
        assertEquals(2, restored.getUnlockedSkills().size());
        assertEquals(List.of(fireball, slash), restored.getSkillLoadout());
        assertEquals(ultimate, restored.getUltimateSlot());
        assertEquals(raceSkill, restored.getRaceSlot());
        assertTrue(restored.getHiddenFlags().contains(flag));
        assertEquals(1, restored.getHiddenFlags().size());
        assertEquals(18, restored.getAttribute(CareerData.ATTR_STR));
        assertEquals(12, restored.getAttribute(CareerData.ATTR_DEX));
        assertEquals(8, restored.getAttribute(CareerData.ATTR_INT));
        assertEquals(14, restored.getAttribute(CareerData.ATTR_WIS));
        assertEquals(10, restored.getAttribute(CareerData.ATTR_CON));
        assertEquals(7, restored.getUnspentAttributePoints());
    }
}
