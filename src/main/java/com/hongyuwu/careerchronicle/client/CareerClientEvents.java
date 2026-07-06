package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.career.CareerProgressionMath;
import com.hongyuwu.careerchronicle.network.C2SUseSkillPacket;
import com.hongyuwu.careerchronicle.network.NetworkHandler;
import com.hongyuwu.careerchronicle.player.CareerDataNbt;
import com.hongyuwu.careerchronicle.player.CareerDataSnapshot;
import com.hongyuwu.careerchronicle.registry.CareerEntities;
import com.hongyuwu.careerchronicle.skill.CareerLoadoutService;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class CareerClientEvents {
    private static final int CAREER_HUD_MARGIN = 4;
    private static final int CAREER_XP_BAR_WIDTH = 82;
    private static final int CAREER_XP_BAR_WIDTH_NARROW = 64;
    private static final int CAREER_XP_BAR_HEIGHT = 4;
    private static final int SKILL_SLOT_SIZE = 14;
    private static final int SKILL_SLOT_SIZE_NARROW = 12;
    private static final int SKILL_ICON_SIZE = 10;
    private static final int SKILL_ICON_SIZE_NARROW = 8;
    private static final int NARROW_THRESHOLD = 320;

    public static final KeyMapping OPEN_CAREER_SCREEN = new KeyMapping(
            "key.careerchronicle.open_career",
            InputConstants.KEY_C,
            "key.categories.careerchronicle"
    );
    public static final KeyMapping USE_SKILL_1 = skillKey("key.careerchronicle.skill_1", InputConstants.KEY_Z);
    public static final KeyMapping USE_SKILL_2 = skillKey("key.careerchronicle.skill_2", InputConstants.KEY_X);
    public static final KeyMapping USE_SKILL_3 = skillKey("key.careerchronicle.skill_3", InputConstants.KEY_V);
    public static final KeyMapping USE_SKILL_4 = skillKey("key.careerchronicle.skill_4", InputConstants.KEY_B);

    private CareerClientEvents() {
    }

    private static KeyMapping skillKey(String key, int code) {
        return new KeyMapping(key, code, "key.categories.careerchronicle");
    }

    @Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_CAREER_SCREEN);
            event.register(USE_SKILL_1);
            event.register(USE_SKILL_2);
            event.register(USE_SKILL_3);
            event.register(USE_SKILL_4);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(CareerEntities.CAREER_PROJECTILE.get(), CareerProjectileRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void onCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
            if (CameraShakeManager.isShaking()) {
                float partial = (float) event.getPartialTick();
                event.setYaw(event.getYaw() + CameraShakeManager.getYawOffset(partial));
                event.setPitch(event.getPitch() + CameraShakeManager.getPitchOffset(partial));
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            ClientCareerData.tick();
            CameraShakeManager.tick();
            HitFlashOverlay.tick();
            while (OPEN_CAREER_SCREEN.consumeClick()) {
                CareerClientScreens.openEntryScreen();
            }
            consumeSkill(USE_SKILL_1, 0);
            consumeSkill(USE_SKILL_2, 1);
            consumeSkill(USE_SKILL_3, 2);
            consumeSkill(USE_SKILL_4, 3);
        }

        @SubscribeEvent
        public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (!VanillaGuiOverlay.HOTBAR.id().equals(event.getOverlay().id())
                    || minecraft.player == null
                    || minecraft.options.hideGui
                    || minecraft.screen != null) {
                return;
            }
            int w = event.getWindow().getGuiScaledWidth();
            int h = event.getWindow().getGuiScaledHeight();
            renderCareerHud(event.getGuiGraphics(), w, h);
            HitFlashOverlay.render(event.getGuiGraphics(), w, h);
        }
    }

    private static void consumeSkill(KeyMapping keyMapping, int slot) {
        while (keyMapping.consumeClick()) {
            if (ClientCareerData.snapshot().skillLoadout().size() <= slot) {
                return;
            }
            ResourceLocation skillId = ClientCareerData.snapshot().skillLoadout().get(slot);
            if (skillId == null) {
                return;
            }
            NetworkHandler.CHANNEL.sendToServer(new C2SUseSkillPacket(skillId));
        }
    }

    private static void renderCareerHud(GuiGraphics graphics, int width, int height) {
        CareerDataSnapshot snapshot = ClientCareerData.snapshot();
        if (CareerDataNbt.UNSELECTED_RACE.equals(snapshot.race())) {
            return;
        }
        renderCareerXpBar(graphics, width, height, snapshot);
        renderSkillBar(graphics, width, height, snapshot);
        renderResourceBars(graphics, width, height, snapshot);
    }

    private static boolean isNarrow(int width) {
        return width < NARROW_THRESHOLD;
    }

    private static int xpBarWidth(int width) {
        return isNarrow(width) ? CAREER_XP_BAR_WIDTH_NARROW : CAREER_XP_BAR_WIDTH;
    }

    private static int slotSize(int width) {
        return isNarrow(width) ? SKILL_SLOT_SIZE_NARROW : SKILL_SLOT_SIZE;
    }

    private static int iconSize(int width) {
        return isNarrow(width) ? SKILL_ICON_SIZE_NARROW : SKILL_ICON_SIZE;
    }

    private static void renderCareerXpBar(GuiGraphics graphics, int width, int height, CareerDataSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font == null) {
            return;
        }
        int x = CAREER_HUD_MARGIN;
        int y = CAREER_HUD_MARGIN;
        int barWidth = xpBarWidth(width);
        int nextXp = CareerProgressionMath.xpForNextLevel(snapshot.careerLevel());
        int xp = Math.max(0, snapshot.careerXp());
        int filled = Math.min(barWidth - 2, Math.max(0, (int) ((long) xp * (barWidth - 2) / nextXp)));
        boolean pendingChoice = CareerProgressionMath.hasPendingSegmentChoice(snapshot.classHistory().size(), snapshot.careerLevel());
        int panelHeight = pendingChoice ? 22 : 15;

        graphics.fill(x - 2, y - 2, x + barWidth + 2, y + panelHeight, 0x5005080D);

        String label = "Lv." + snapshot.careerLevel() + " " + xp + "/" + nextXp;
        graphics.drawString(minecraft.font, label, x, y, 0xCCE8F0FF, false);

        int barY = y + 10;
        graphics.fill(x, barY, x + barWidth, barY + CAREER_XP_BAR_HEIGHT, 0x99101820);
        graphics.fill(x, barY, x + filled, barY + CAREER_XP_BAR_HEIGHT, 0xCC55C7F7);

        if (pendingChoice) {
            graphics.drawString(minecraft.font, "▲", x + barWidth + 3, y, 0xFFFFD37A, false);
        }
    }

    private static void renderSkillBar(GuiGraphics graphics, int width, int height, CareerDataSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font == null) {
            return;
        }
        int activeSlots = CareerLoadoutService.ACTIVE_SLOT_COUNT;
        int slot = slotSize(width);
        int icon = iconSize(width);
        int gap = isNarrow(width) ? 1 : 2;
        int x = CAREER_HUD_MARGIN;
        int y = CAREER_HUD_MARGIN + 17;
        for (int i = 0; i < activeSlots; i++) {
            int slotX = x + i * (slot + gap);
            ResourceLocation skillId = snapshot.skillLoadout().size() > i
                    ? snapshot.skillLoadout().get(i)
                    : null;
            int color = skillId == null ? 0x4020242A : 0x66162332;
            graphics.fill(slotX, y, slotX + slot, y + slot, color);
            int iconOffset = (slot - icon) / 2;
            SkillIconRenderer.render(graphics, skillId, slotX + iconOffset, y + iconOffset, icon);
            if (skillId != null) {
                int cooldown = ClientCareerData.cooldown(skillId);
                if (cooldown > 0) {
                    graphics.fill(slotX, y, slotX + slot, y + slot, 0x99000000);
                    graphics.drawCenteredString(minecraft.font, Integer.toString(cooldown / 20 + 1),
                            slotX + slot / 2, y + (slot - 8) / 2, 0xFFFFFFFF);
                }
            }
        }

        int specialX = x + activeSlots * (slot + gap) + gap;
        renderSpecialSlot(graphics, minecraft, specialX, y, slot, icon, snapshot.ultimateSlot(), 0x44FF3030);
        specialX += slot + gap;
        renderSpecialSlot(graphics, minecraft, specialX, y, slot, icon, snapshot.raceSlot(), 0x4430A0FF);
    }

    private static void renderSpecialSlot(GuiGraphics graphics, Minecraft minecraft, int x, int y,
                                           int slot, int icon, ResourceLocation skillId, int borderColor) {
        int bgColor = skillId == null ? 0x3020242A : 0x55162332;
        graphics.fill(x, y, x + slot, y + slot, bgColor);
        graphics.fill(x, y, x + slot, y + 1, borderColor);
        graphics.fill(x, y + slot - 1, x + slot, y + slot, borderColor);
        graphics.fill(x, y, x + 1, y + slot, borderColor);
        graphics.fill(x + slot - 1, y, x + slot, y + slot, borderColor);
        if (skillId != null) {
            int iconOffset = (slot - icon) / 2;
            SkillIconRenderer.render(graphics, skillId, x + iconOffset, y + iconOffset, icon);
            int cooldown = ClientCareerData.cooldown(skillId);
            if (cooldown > 0 && minecraft.font != null) {
                graphics.fill(x, y, x + slot, y + slot, 0x99000000);
                graphics.drawCenteredString(minecraft.font, Integer.toString(cooldown / 20 + 1),
                        x + slot / 2, y + (slot - 8) / 2, 0xFFFFFFFF);
            }
        }
    }

    private static void renderResourceBars(GuiGraphics graphics, int width, int height, CareerDataSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font == null || snapshot.maxMana() <= 0 && snapshot.maxStamina() <= 0) {
            return;
        }
        int totalSlots = CareerLoadoutService.ACTIVE_SLOT_COUNT + 2;
        int slot = slotSize(width);
        int gap = isNarrow(width) ? 1 : 2;
        int totalWidth = totalSlots * slot + (totalSlots - 1) * gap;
        int x = CAREER_HUD_MARGIN;
        int y = CAREER_HUD_MARGIN + 17 + slot + 2;
        drawResourceBar(graphics, x, y, totalWidth, snapshot.mana(), snapshot.maxMana(), 0xAA4CA7FF);
        drawResourceBar(graphics, x, y + 4, totalWidth, snapshot.stamina(), snapshot.maxStamina(), 0xAAFFC34C);
    }

    private static void drawResourceBar(GuiGraphics graphics, int x, int y, int width, int value, int max, int color) {
        int safeMax = Math.max(1, max);
        int filled = Math.min(width, Math.max(0, (int) ((long) Math.max(0, value) * width / safeMax)));
        graphics.fill(x, y, x + width, y + 3, 0x66101820);
        graphics.fill(x, y, x + filled, y + 3, color);
    }

    private static String slotKey(int slot) {
        return slot == 0 ? "Z" : slot == 1 ? "X" : slot == 2 ? "V" : "B";
    }

}
