package com.hongyuwu.careerchronicle.career;

public final class CareerProgressionMath {
    public static final int SEGMENT_LEVELS = 10;
    public static final int MAX_ALPHA_LEVEL = 50;
    public static final int MAX_ALPHA_SEGMENTS = 5;

    private CareerProgressionMath() {
    }

    public static int xpForNextLevel(int level) {
        int safeLevel = Math.max(1, level);
        return Math.max(1, 100 + safeLevel * 35 + (int) Math.floor(Math.pow(safeLevel, 1.35D) * 12.0D));
    }

    public static int nextSegmentIndex(int selectedSegments) {
        return Math.max(1, selectedSegments + 1);
    }

    public static int requiredLevelForNextSegment(int selectedSegments) {
        if (selectedSegments == 0) {
            return 1;
        }
        int nextSegment = nextSegmentIndex(selectedSegments);
        return Math.min(MAX_ALPHA_LEVEL, nextSegment * SEGMENT_LEVELS);
    }

    public static boolean hasAlphaSegmentSlot(int selectedSegments) {
        return selectedSegments < MAX_ALPHA_SEGMENTS;
    }

    public static int levelCapForSelectedSegments(int selectedSegments) {
        if (!hasAlphaSegmentSlot(selectedSegments)) {
            return MAX_ALPHA_LEVEL;
        }
        return requiredLevelForNextSegment(selectedSegments);
    }

    public static boolean hasPendingSegmentChoice(int selectedSegments, int level) {
        return hasAlphaSegmentSlot(selectedSegments) && level >= requiredLevelForNextSegment(selectedSegments);
    }
}
