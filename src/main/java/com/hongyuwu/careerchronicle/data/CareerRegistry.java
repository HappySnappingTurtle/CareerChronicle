package com.hongyuwu.careerchronicle.data;

public final class CareerRegistry {
    private static volatile RegistrySnapshot snapshot = RegistrySnapshot.EMPTY;

    private CareerRegistry() {
    }

    public static RegistrySnapshot snapshot() {
        return snapshot;
    }

    static synchronized void replace(RegistrySnapshot nextSnapshot) {
        snapshot = nextSnapshot;
    }
}
