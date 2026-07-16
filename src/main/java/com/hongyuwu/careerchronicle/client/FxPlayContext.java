package com.hongyuwu.careerchronicle.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record FxPlayContext(
        ClientLevel level,
        ResourceLocation skillId,
        String fxType,
        int casterId,
        long seed,
        Vec3 origin,
        Vec3 target,
        float particleMultiplier
) {
}
