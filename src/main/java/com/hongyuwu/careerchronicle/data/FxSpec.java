package com.hongyuwu.careerchronicle.data;

public record FxSpec(
        String castSound,
        String castParticle,
        String hitSound,
        String hitParticle,
        float cameraShakeStrength,
        int cameraShakeTicks,
        boolean castCircle
) {
    public static final FxSpec EMPTY = new FxSpec(null, null, null, null, 0, 0, false);
}
