package com.hongyuwu.careerchronicle.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

public final class HitFlashOverlay {
    private static int flashTimer;
    private static int flashDuration;
    private static int flashColor = 0xFFFFFFFF;

    private HitFlashOverlay() {}

    public static void trigger(int color, int durationTicks) {
        flashColor = color;
        flashDuration = durationTicks;
        flashTimer = 0;
    }

    public static void triggerWhite() {
        trigger(0xFFFFFFFF, 3);
    }

    public static void triggerFire() {
        trigger(0xFFFF6600, 4);
    }

    public static void triggerIce() {
        trigger(0xFF66CCFF, 4);
    }

    public static void triggerDark() {
        trigger(0xFF6633AA, 4);
    }

    public static void triggerHoly() {
        trigger(0xFFFFEE88, 3);
    }

    public static void tick() {
        if (flashDuration > 0) {
            flashTimer++;
            if (flashTimer >= flashDuration) {
                flashDuration = 0;
            }
        }
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (flashDuration <= 0) return;
        float progress = (float) flashTimer / flashDuration;
        int alpha = (int) (120 * (1.0F - progress));
        if (alpha <= 0) return;
        int color = (flashColor & 0x00FFFFFF) | (alpha << 24);
        graphics.fill(0, 0, screenWidth, screenHeight, color);
    }
}
