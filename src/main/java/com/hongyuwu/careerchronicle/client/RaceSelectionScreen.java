package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.RaceDef;
import com.hongyuwu.careerchronicle.data.RegistrySnapshot;
import com.hongyuwu.careerchronicle.network.C2SSelectRacePacket;
import com.hongyuwu.careerchronicle.network.NetworkHandler;
import com.hongyuwu.careerchronicle.player.CareerDataNbt;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class RaceSelectionScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 390;
    private static final int MAX_PANEL_HEIGHT = 340;
    private static final int RACE_ROW_HEIGHT = 36;
    private static final List<ResourceLocation> FALLBACK_RACES = List.of(
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "human"),
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "elf"),
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "dwarf"),
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "orc")
    );

    public RaceSelectionScreen() {
        super(Component.translatable("careerchronicle.ui.race_title"));
    }

    public static boolean shouldChooseRace() {
        return CareerDataNbt.UNSELECTED_RACE.equals(ClientCareerData.snapshot().race());
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        int left = panelLeft();
        int top = panelTop();
        int pw = panelWidth();
        int ph = panelHeight();
        int y = top + 50;
        RegistrySnapshot registry = CareerRegistry.snapshot();
        for (ResourceLocation raceId : races(registry)) {
            addRenderableWidget(Button.builder(displayRace(registry, raceId), button -> {
                        NetworkHandler.CHANNEL.sendToServer(new C2SSelectRacePacket(raceId));
                        button.active = false;
                    })
                    .bounds(left + 32, y, 68, 18)
                    .build());
            y += RACE_ROW_HEIGHT;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + pw - 70, top + ph - 22, 56, 16)
                .build());
    }

    public void refreshFromServer() {
        if (!shouldChooseRace() && minecraft != null) {
            minecraft.setScreen(new CareerScreen());
            return;
        }
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = panelLeft();
        int top = panelTop();
        int pw = panelWidth();
        int ph = panelHeight();
        graphics.fill(left, top, left + pw, top + ph, 0xD010141A);
        graphics.fill(left, top, left + pw, top + 20, 0xE02B1F3B);
        graphics.drawString(this.font, this.title, left + 10, top + 6, 0xFFE8F0FF, false);
        graphics.drawString(this.font, Component.translatable("careerchronicle.ui.race_intro"),
                left + 14, top + 28, 0xFFC8D4E6, false);

        RegistrySnapshot registry = CareerRegistry.snapshot();
        int y = top + 52;
        int textX = left + 108;
        if (pw < 280) textX = left + 80;
        for (ResourceLocation raceId : races(registry)) {
            SkillIconRenderer.renderRace(graphics, raceId, left + 14, y, 18);
            graphics.drawString(this.font, displayRace(registry, raceId).copy().withStyle(ChatFormatting.GOLD),
                    textX, y, 0xFFFFD37A, false);
            graphics.drawString(this.font, traitsText(registry, raceId),
                    textX, y + 11, 0xFFE8F0FF, false);
            graphics.drawString(this.font, traitsDescText(registry, raceId),
                    textX, y + 22, 0xFF92A4B8, false);
            y += RACE_ROW_HEIGHT;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private List<ResourceLocation> races(RegistrySnapshot registry) {
        if (registry.races().isEmpty()) {
            return FALLBACK_RACES;
        }
        String currentBiome = currentBiomeId();
        if (currentBiome.isEmpty()) {
            return List.copyOf(registry.races().keySet());
        }
        List<ResourceLocation> filtered = new ArrayList<>();
        for (var entry : registry.races().entrySet()) {
            if (entry.getValue().matchesBiome(currentBiome)) {
                filtered.add(entry.getKey());
            }
        }
        if (filtered.isEmpty()) {
            return List.copyOf(registry.races().keySet());
        }
        return filtered;
    }

    private static String currentBiomeId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return "";
        return mc.level.getBiome(mc.player.blockPosition())
                .unwrapKey()
                .filter(key -> key.isFor(Registries.BIOME))
                .map(key -> key.location().toString())
                .orElse("");
    }

    private Component displayRace(RegistrySnapshot registry, ResourceLocation raceId) {
        RaceDef race = registry.races().get(raceId);
        return race == null
                ? Component.translatable("careerchronicle.race." + raceId.getPath())
                : Component.translatable(race.displayKey());
    }

    private Component traitsText(RegistrySnapshot registry, ResourceLocation raceId) {
        RaceDef race = registry.races().get(raceId);
        if (race == null || race.traits().isEmpty()) {
            return Component.translatable("careerchronicle.ui.no_traits");
        }
        return Component.translatable("careerchronicle.ui.traits",
                Component.translatable("careerchronicle.trait." + race.traits().get(0).getPath()));
    }

    private Component traitsDescText(RegistrySnapshot registry, ResourceLocation raceId) {
        RaceDef race = registry.races().get(raceId);
        if (race == null || race.traits().isEmpty()) {
            return Component.empty();
        }
        return Component.translatable("careerchronicle.trait." + race.traits().get(0).getPath() + ".desc");
    }

    private int panelWidth() {
        return Math.min(MAX_PANEL_WIDTH, this.width - 20);
    }

    private int panelHeight() {
        return Math.min(MAX_PANEL_HEIGHT, this.height - 16);
    }

    private int panelLeft() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (this.height - panelHeight()) / 2;
    }
}
