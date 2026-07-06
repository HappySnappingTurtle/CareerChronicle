package com.hongyuwu.careerchronicle.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record ManualEntry(
        ManualIcon iconType,
        ResourceLocation iconId,
        Component title,
        Component lineOne,
        Component lineTwo,
        int accentColor,
        Component fallbackIcon
) {
}
