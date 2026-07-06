package com.hongyuwu.careerchronicle.data;

import net.minecraft.resources.ResourceLocation;

public record XpSourceDef(
        ResourceLocation id,
        String displayKey,
        int baseAmount,
        double healthMultiplier,
        int minAmount,
        int maxAmount
) {
    public XpSourceDef {
        baseAmount = Math.max(0, baseAmount);
        healthMultiplier = Math.max(0.0D, healthMultiplier);
        minAmount = Math.max(0, minAmount);
        maxAmount = Math.max(minAmount, maxAmount);
    }

    public int amountForHealth(float maxHealth) {
        int scaled = baseAmount + Math.round(Math.max(0.0F, maxHealth) * (float) healthMultiplier);
        if (maxAmount > 0) {
            scaled = Math.min(maxAmount, scaled);
        }
        return Math.max(minAmount, scaled);
    }
}
