package com.hongyuwu.careerchronicle.autotest;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

public final class TestScreenshotCapture {
    private TestScreenshotCapture() {}

    public static void capture(File outputDir, String filename) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            try {
                NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
                File outFile = new File(outputDir, filename);
                image.writeToFile(outFile);
                image.close();
                CareerChronicleMod.LOGGER.info("[AutoTest] Screenshot saved: {}", outFile.getAbsolutePath());
            } catch (Exception e) {
                CareerChronicleMod.LOGGER.error("[AutoTest] Screenshot failed: {}", e.getMessage(), e);
            }
        });
    }
}
