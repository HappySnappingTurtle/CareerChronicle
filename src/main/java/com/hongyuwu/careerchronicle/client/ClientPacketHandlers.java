package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.player.CareerDataSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void updateCareerSnapshot(CareerDataSnapshot snapshot) {
        ClientCareerData.update(snapshot);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CareerScreen careerScreen) {
            minecraft.execute(careerScreen::refreshFromServer);
        } else if (minecraft.screen instanceof RaceSelectionScreen raceSelectionScreen) {
            minecraft.execute(raceSelectionScreen::refreshFromServer);
        }
    }

    public static void openCareerScreen() {
        CareerClientScreens.openCareerScreen();
    }

    public static void playSkillFx(ResourceLocation skillId, String fxType, Vec3 origin, Vec3 target, double particleMultiplier) {
        Minecraft.getInstance().execute(() -> SkillFxRenderer.play(skillId, fxType, origin, target, particleMultiplier));
    }
}
