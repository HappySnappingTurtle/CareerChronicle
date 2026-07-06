package com.hongyuwu.careerchronicle.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class CareerRuntimeState {
    private final Map<ResourceLocation, Integer> cooldownTicks = new HashMap<>();
    private int mana;
    private int stamina;
    private int maxMana;
    private int maxStamina;
    private ResourceLocation nextProjectileModifier;

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = Math.max(0, Math.min(mana, maxMana));
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = Math.max(0, Math.min(stamina, maxStamina));
    }

    public int getMaxMana() {
        return maxMana;
    }

    public int getMaxStamina() {
        return maxStamina;
    }

    public void setResourceCaps(int maxMana, int maxStamina) {
        this.maxMana = Math.max(0, maxMana);
        this.maxStamina = Math.max(0, maxStamina);
        mana = Math.min(mana, this.maxMana);
        stamina = Math.min(stamina, this.maxStamina);
    }

    public void refillResources() {
        mana = maxMana;
        stamina = maxStamina;
    }

    public boolean hasResource(String resource, int amount) {
        if (amount <= 0 || "none".equals(resource)) {
            return true;
        }
        if ("mana".equals(resource)) {
            return mana >= amount;
        }
        if ("stamina".equals(resource)) {
            return stamina >= amount;
        }
        return false;
    }

    public void consumeResource(String resource, int amount) {
        if (amount <= 0 || "none".equals(resource)) {
            return;
        }
        if ("mana".equals(resource)) {
            setMana(mana - amount);
        } else if ("stamina".equals(resource)) {
            setStamina(stamina - amount);
        }
    }

    public void restoreResource(String resource, int amount) {
        if (amount <= 0 || "none".equals(resource)) {
            return;
        }
        if ("mana".equals(resource)) {
            setMana(mana + amount);
        } else if ("stamina".equals(resource)) {
            setStamina(stamina + amount);
        }
    }

    public boolean regenerateResources(int manaAmount, int staminaAmount) {
        int oldMana = mana;
        int oldStamina = stamina;
        setMana(mana + Math.max(0, manaAmount));
        setStamina(stamina + Math.max(0, staminaAmount));
        return oldMana != mana || oldStamina != stamina;
    }

    public ResourceLocation getNextProjectileModifier() {
        return nextProjectileModifier;
    }

    public void setNextProjectileModifier(ResourceLocation nextProjectileModifier) {
        this.nextProjectileModifier = nextProjectileModifier;
    }

    public ResourceLocation consumeNextProjectileModifier() {
        ResourceLocation modifier = nextProjectileModifier;
        nextProjectileModifier = null;
        return modifier;
    }

    public void setCooldown(ResourceLocation skillId, int ticks) {
        if (ticks <= 0) {
            cooldownTicks.remove(skillId);
            return;
        }
        cooldownTicks.put(skillId, ticks);
    }

    public Map<ResourceLocation, Integer> getCooldownTicks() {
        return Collections.unmodifiableMap(cooldownTicks);
    }

    public void tickCooldowns() {
        Iterator<Map.Entry<ResourceLocation, Integer>> iterator = cooldownTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, Integer> entry = iterator.next();
            int nextTicks = entry.getValue() - 1;
            if (nextTicks <= 0) {
                iterator.remove();
            } else {
                entry.setValue(nextTicks);
            }
        }
    }

    public boolean retainCooldowns(Set<ResourceLocation> skillIds) {
        return cooldownTicks.keySet().retainAll(skillIds);
    }

    public void clear() {
        cooldownTicks.clear();
        mana = 0;
        stamina = 0;
        maxMana = 0;
        maxStamina = 0;
        nextProjectileModifier = null;
    }
}
