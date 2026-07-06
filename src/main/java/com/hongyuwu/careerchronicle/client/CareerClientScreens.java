package com.hongyuwu.careerchronicle.client;

import net.minecraft.client.Minecraft;

public final class CareerClientScreens {
    private CareerClientScreens() {
    }

    public static void openEntryScreen() {
        Minecraft.getInstance().setScreen(RaceSelectionScreen.shouldChooseRace()
                ? new RaceSelectionScreen()
                : new CareerScreen());
    }

    public static void openCareerScreen() {
        openEntryScreen();
    }
}
