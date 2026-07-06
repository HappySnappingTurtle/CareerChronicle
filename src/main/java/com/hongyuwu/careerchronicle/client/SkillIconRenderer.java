package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.data.CareerRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class SkillIconRenderer {
    private SkillIconRenderer() {
    }

    public static void render(GuiGraphics graphics, ResourceLocation skillId, int x, int y, int size) {
        if (skillId == null) {
            graphics.fill(x, y, x + size, y + size, 0xBB1B2028);
            graphics.fill(x + 3, y + 3, x + size - 3, y + size - 3, 0xAA303642);
            return;
        }
        graphics.blit(icon(skillId), x, y, 0, 0, size, size, size, size);
        renderTypeFrame(graphics, x, y, size, skillType(skillId));
    }

    public static void renderClass(GuiGraphics graphics, ResourceLocation classId, int x, int y, int size) {
        if (classId == null) {
            render(graphics, null, x, y, size);
            return;
        }
        graphics.blit(classIcon(classId), x, y, 0, 0, size, size, size, size);
    }

    public static void renderRace(GuiGraphics graphics, ResourceLocation raceId, int x, int y, int size) {
        if (raceId == null) {
            render(graphics, null, x, y, size);
            return;
        }
        graphics.blit(raceIcon(raceId), x, y, 0, 0, size, size, size, size);
    }

    private static ResourceLocation icon(ResourceLocation skillId) {
        return ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID,
                "textures/gui/skill/" + skillId.getPath() + ".png");
    }

    private static ResourceLocation classIcon(ResourceLocation classId) {
        return ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID,
                "textures/gui/class/" + classId.getPath() + ".png");
    }

    private static ResourceLocation raceIcon(ResourceLocation raceId) {
        return ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID,
                "textures/gui/race/" + raceId.getPath() + ".png");
    }

    private static String skillType(ResourceLocation skillId) {
        return CareerRegistry.snapshot().skill(skillId)
                .map(skill -> skill.type())
                .orElse("active");
    }

    private static void renderTypeFrame(GuiGraphics graphics, int x, int y, int size, String type) {
        int color = typeColor(type);
        switch (type) {
            case "fusion" -> renderDiamondFrame(graphics, x, y, size, color);
            case "hidden" -> renderHexFrame(graphics, x, y, size, color);
            case "passive" -> renderSquareFrame(graphics, x, y, size, color);
            case "ultimate" -> renderUltimateFrame(graphics, x, y, size, color);
            case "race" -> renderCornerFrame(graphics, x, y, size, color);
            default -> renderRoundFrame(graphics, x, y, size, color);
        }
    }

    private static int typeColor(String type) {
        return switch (type) {
            case "fusion" -> 0xFFFFCF5A;
            case "hidden" -> 0xFFFF78E8;
            case "passive" -> 0xFF8DEFA2;
            case "ultimate" -> 0xFFFF645A;
            case "race" -> 0xFF9BD2FF;
            default -> 0xFF6BCBFF;
        };
    }

    private static void renderRoundFrame(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 4, y, x + size - 4, y + 1, color);
        graphics.fill(x + 4, y + size - 1, x + size - 4, y + size, color);
        graphics.fill(x, y + 4, x + 1, y + size - 4, color);
        graphics.fill(x + size - 1, y + 4, x + size, y + size - 4, color);
        graphics.fill(x + 2, y + 1, x + 4, y + 2, color);
        graphics.fill(x + size - 4, y + 1, x + size - 2, y + 2, color);
        graphics.fill(x + 2, y + size - 2, x + 4, y + size - 1, color);
        graphics.fill(x + size - 4, y + size - 2, x + size - 2, y + size - 1, color);
    }

    private static void renderDiamondFrame(GuiGraphics graphics, int x, int y, int size, int color) {
        int mid = size / 2;
        for (int i = 0; i < mid; i++) {
            plot(graphics, x + mid - i, y + i, color);
            plot(graphics, x + mid + i, y + i, color);
            plot(graphics, x + mid - i, y + size - 1 - i, color);
            plot(graphics, x + mid + i, y + size - 1 - i, color);
        }
    }

    private static void renderHexFrame(GuiGraphics graphics, int x, int y, int size, int color) {
        int inset = Math.max(3, size / 4);
        graphics.fill(x + inset, y, x + size - inset, y + 1, color);
        graphics.fill(x + inset, y + size - 1, x + size - inset, y + size, color);
        graphics.fill(x, y + inset, x + 1, y + size - inset, color);
        graphics.fill(x + size - 1, y + inset, x + size, y + size - inset, color);
        for (int i = 0; i < inset; i++) {
            plot(graphics, x + inset - i - 1, y + i + 1, color);
            plot(graphics, x + size - inset + i, y + i + 1, color);
            plot(graphics, x + inset - i - 1, y + size - i - 2, color);
            plot(graphics, x + size - inset + i, y + size - i - 2, color);
        }
    }

    private static void renderSquareFrame(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x, y, x + size, y + 1, color);
        graphics.fill(x, y + size - 1, x + size, y + size, color);
        graphics.fill(x, y, x + 1, y + size, color);
        graphics.fill(x + size - 1, y, x + size, y + size, color);
    }

    private static void renderUltimateFrame(GuiGraphics graphics, int x, int y, int size, int color) {
        renderSquareFrame(graphics, x, y, size, color);
        graphics.fill(x + 3, y + 3, x + size - 3, y + 4, color);
        graphics.fill(x + 3, y + size - 4, x + size - 3, y + size - 3, color);
    }

    private static void renderCornerFrame(GuiGraphics graphics, int x, int y, int size, int color) {
        int length = Math.max(4, size / 3);
        graphics.fill(x, y, x + length, y + 1, color);
        graphics.fill(x, y, x + 1, y + length, color);
        graphics.fill(x + size - length, y, x + size, y + 1, color);
        graphics.fill(x + size - 1, y, x + size, y + length, color);
        graphics.fill(x, y + size - 1, x + length, y + size, color);
        graphics.fill(x, y + size - length, x + 1, y + size, color);
        graphics.fill(x + size - length, y + size - 1, x + size, y + size, color);
        graphics.fill(x + size - 1, y + size - length, x + size, y + size, color);
    }

    private static void plot(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 1, y + 1, color);
    }
}
