package com.hongyuwu.careerchronicle.data;

public record UpgradeRule(
        String source,
        int maxLevel
) {
    public static final UpgradeRule NONE = new UpgradeRule("none", 1);

    public UpgradeRule {
        if (source == null || source.isBlank()) {
            source = "none";
        }
        maxLevel = Math.max(1, maxLevel);
    }
}
