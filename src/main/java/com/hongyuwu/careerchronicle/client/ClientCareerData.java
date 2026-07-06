package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.player.CareerDataSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class ClientCareerData {
    private static CareerDataSnapshot snapshot = CareerDataSnapshot.empty();
    private static final Map<ResourceLocation, Integer> cooldownTicks = new LinkedHashMap<>();

    private ClientCareerData() {
    }

    public static CareerDataSnapshot snapshot() {
        return snapshot;
    }

    public static Map<ResourceLocation, Integer> cooldownTicks() {
        return Map.copyOf(cooldownTicks);
    }

    public static int cooldown(ResourceLocation skillId) {
        return cooldownTicks.getOrDefault(skillId, 0);
    }

    public static void tick() {
        cooldownTicks.entrySet().removeIf(entry -> {
            int nextTicks = entry.getValue() - 1;
            if (nextTicks <= 0) {
                return true;
            }
            entry.setValue(nextTicks);
            return false;
        });
    }

    public static void update(CareerDataSnapshot nextSnapshot) {
        snapshot = nextSnapshot;
        cooldownTicks.clear();
        cooldownTicks.putAll(nextSnapshot.cooldownTicks());
        CareerChronicleMod.LOGGER.info(
                "Client career snapshot updated: race={}, level={}, xp={}, classes={}, skills={}",
                nextSnapshot.race(),
                nextSnapshot.careerLevel(),
                nextSnapshot.careerXp(),
                nextSnapshot.classHistory(),
                nextSnapshot.unlockedSkills()
        );
    }
}
