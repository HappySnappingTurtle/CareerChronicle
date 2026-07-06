package com.hongyuwu.careerchronicle.item;

import net.minecraft.ChatFormatting;

public enum WeaponTier {
    COMMON("common", ChatFormatting.WHITE, 1.0F, 1.0F),
    UNCOMMON("uncommon", ChatFormatting.GREEN, 1.3F, 1.2F),
    RARE("rare", ChatFormatting.BLUE, 1.6F, 1.5F),
    EPIC("epic", ChatFormatting.DARK_PURPLE, 2.0F, 1.8F),
    LEGENDARY("legendary", ChatFormatting.GOLD, 2.5F, 2.2F);

    private final String id;
    private final ChatFormatting color;
    private final float damageMultiplier;
    private final float durabilityMultiplier;

    WeaponTier(String id, ChatFormatting color, float damageMultiplier, float durabilityMultiplier) {
        this.id = id;
        this.color = color;
        this.damageMultiplier = damageMultiplier;
        this.durabilityMultiplier = durabilityMultiplier;
    }

    public String id() { return id; }
    public ChatFormatting color() { return color; }
    public float damageMultiplier() { return damageMultiplier; }
    public float durabilityMultiplier() { return durabilityMultiplier; }

    public static WeaponTier fromId(String id) {
        for (WeaponTier tier : values()) {
            if (tier.id.equals(id)) return tier;
        }
        return COMMON;
    }
}
