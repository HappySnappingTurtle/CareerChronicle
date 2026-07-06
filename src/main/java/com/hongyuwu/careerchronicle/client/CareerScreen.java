package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.career.CareerProgressionMath;
import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.ClassDef;
import com.hongyuwu.careerchronicle.data.FusionDef;
import com.hongyuwu.careerchronicle.data.HiddenUnlockDef;
import com.hongyuwu.careerchronicle.data.RaceDef;
import com.hongyuwu.careerchronicle.data.RegistrySnapshot;
import com.hongyuwu.careerchronicle.data.SkillDef;
import com.hongyuwu.careerchronicle.network.C2SSelectClassPacket;
import com.hongyuwu.careerchronicle.network.NetworkHandler;
import com.hongyuwu.careerchronicle.player.CareerDataNbt;
import com.hongyuwu.careerchronicle.player.CareerDataSnapshot;
import com.hongyuwu.careerchronicle.skill.SkillEquipmentRequirements;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class CareerScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 420;
    private static final int MAX_PANEL_HEIGHT = 300;
    private static final int MANUAL_CONTENT_TOP = 36;
    private static final int MANUAL_ROW_HEIGHT = 38;
    private static final List<ResourceLocation> FALLBACK_CLASSES = List.of(
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fire_mage"),
            ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "archer")
    );
    private ResourceLocation selectedClassId;
    private ManualPage manualPage = ManualPage.OVERVIEW;
    private int manualScrollOffset;

    public CareerScreen() {
        super(Component.translatable("careerchronicle.ui.title"));
    }

    @Override
    protected void init() {
        if (RaceSelectionScreen.shouldChooseRace() && minecraft != null) {
            minecraft.setScreen(new RaceSelectionScreen());
            return;
        }
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xD010141A);
        graphics.fill(left, top, left + panelWidth(), top + 22, 0xE0223344);
        graphics.drawString(this.font, this.title, left + 12, top + 7, 0xFFE8F0FF, false);

        CareerDataSnapshot snapshot = ClientCareerData.snapshot();
        RegistrySnapshot registry = CareerRegistry.snapshot();
        if (manualPage == ManualPage.OVERVIEW) {
            renderOverviewSimple(graphics, registry, snapshot, left, top);
        } else {
            renderManualPage(graphics, registry, snapshot, left, top);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        CareerDataSnapshot snapshot = ClientCareerData.snapshot();
        RegistrySnapshot registry = CareerRegistry.snapshot();
        int left = panelLeft();
        int top = panelTop();

        addOverviewTabs(left, top);
        if (manualPage != ManualPage.OVERVIEW) {
            addDoneButton(left, top);
            return;
        }

        int pw = panelWidth();
        int ph = panelHeight();
        int leftColW = pw * 30 / 100;
        int rightX = left + leftColW + 2;
        int rightW = pw - leftColW - 10;
        int y = top + 42;
        int maxVisible = Math.max(3, (ph - 52) / 18);
        List<ResourceLocation> classes = classOptions(registry, snapshot);
        int shown = 0;
        for (ResourceLocation classId : classes) {
            if (shown >= maxVisible) break;
            boolean selectable = canSelectClass(registry, snapshot, classId);
            boolean previewable = canPreviewClass(registry, snapshot, classId);
            Component label = selectable ? displayClass(registry, classId) :
                    displayClass(registry, classId).copy().withStyle(ChatFormatting.DARK_GRAY);
            int btnW = Math.min(leftColW - 12, 88);
            Button classButton = Button.builder(label, button -> { selectedClassId = classId; rebuildWidgets(); })
                    .bounds(left + 6, y, btnW, 15)
                    .build();
            classButton.active = previewable;
            if (!selectable) {
                classButton.setTooltip(Tooltip.create(Component.translatable("careerchronicle.ui.class_unavailable_tooltip",
                        displayClass(registry, classId), classUnavailableReason(registry, snapshot, classId))));
            }
            addRenderableWidget(classButton);
            y += 18;
            shown++;
        }

        ResourceLocation previewClassId = previewClassId(registry, snapshot);
        if (previewClassId != null) {
            int chooseBtnY = top + ph - 20;
            Button chooseButton = Button.builder(Component.translatable("careerchronicle.ui.choose_preview_class"),
                            button -> {
                                ResourceLocation toSelect = previewClassId(CareerRegistry.snapshot(), ClientCareerData.snapshot());
                                if (toSelect != null) {
                                    NetworkHandler.CHANNEL.sendToServer(new C2SSelectClassPacket(toSelect));
                                }
                                selectedClassId = null;
                                scheduleRefresh();
                            })
                    .bounds(rightX, chooseBtnY, rightW, 16)
                    .build();
            chooseButton.active = canSelectClass(registry, snapshot, previewClassId);
            if (!chooseButton.active) {
                chooseButton.setTooltip(Tooltip.create(Component.translatable("careerchronicle.ui.class_unavailable_tooltip",
                        displayClass(registry, previewClassId),
                        classUnavailableReason(registry, snapshot, previewClassId))));
            }
            addRenderableWidget(chooseButton);
        }

        addDoneButton(left, top);
    }

    private void addDoneButton(int left, int top) {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + panelWidth() - 70, top + 3, 58, 16)
                .build());
    }

    private void scheduleRefresh() {
        refreshFromServer();
    }

    public void refreshFromServer() {
        rebuildWidgets();
    }

    private CareerDataSnapshot snapshot() {
        return ClientCareerData.snapshot();
    }

    private List<ResourceLocation> allowedClasses(RegistrySnapshot registry, CareerDataSnapshot snapshot) {
        if (registry.classes().isEmpty()) {
            return FALLBACK_CLASSES;
        }
        RaceDef race = registry.races().get(snapshot.race());
        if (race != null && !race.allowedClasses().isEmpty()) {
            return race.allowedClasses();
        }
        return List.copyOf(registry.classes().keySet());
    }

    private List<ResourceLocation> classOptions(RegistrySnapshot registry, CareerDataSnapshot snapshot) {
        if (registry.classes().isEmpty()) {
            return FALLBACK_CLASSES;
        }
        var raceDef = registry.races().get(snapshot.race());
        List<ResourceLocation> allowed = raceDef != null && !raceDef.allowedClasses().isEmpty()
                ? raceDef.allowedClasses() : List.copyOf(registry.classes().keySet());

        List<ResourceLocation> available = new ArrayList<>();
        List<ResourceLocation> locked = new ArrayList<>();
        for (ResourceLocation classId : allowed) {
            ClassDef careerClass = registry.classes().get(classId);
            if (careerClass == null) continue;
            if (careerClass.hidden()) {
                if (careerClass.unlockFlag() == null || !snapshot.hiddenFlags().contains(careerClass.unlockFlag())) {
                    continue;
                }
            }
            if (careerClass.requiredAttributes().isEmpty()) {
                available.add(classId);
            } else {
                locked.add(classId);
            }
        }
        available.addAll(locked);
        return available;
    }

    private boolean canSelectClass(RegistrySnapshot registry, CareerDataSnapshot snapshot, ResourceLocation classId) {
        if (!canPreviewClass(registry, snapshot, classId)) {
            return false;
        }
        int selectedSegments = snapshot.classHistory().size();
        return CareerProgressionMath.hasAlphaSegmentSlot(selectedSegments)
                && snapshot.careerLevel() >= CareerProgressionMath.requiredLevelForNextSegment(selectedSegments);
    }

    private boolean canPreviewClass(RegistrySnapshot registry, CareerDataSnapshot snapshot, ResourceLocation classId) {
        if (registry.classes().isEmpty()) {
            return true;
        }
        if (!registry.classes().containsKey(classId)) {
            return false;
        }
        if (CareerDataNbt.UNSELECTED_RACE.equals(snapshot.race())) {
            return false;
        }
        ClassDef careerClass = registry.classes().get(classId);
        if (careerClass != null && careerClass.hidden()) {
            return careerClass.unlockFlag() != null && snapshot.hiddenFlags().contains(careerClass.unlockFlag());
        }
        RaceDef race = registry.races().get(snapshot.race());
        return race == null || race.allowedClasses().isEmpty() || race.allowedClasses().contains(classId);
    }

    private Component classUnavailableReason(
            RegistrySnapshot registry,
            CareerDataSnapshot snapshot,
            ResourceLocation classId
    ) {
        if (!registry.classes().containsKey(classId)) {
            return Component.translatable("careerchronicle.ui.class_unavailable_missing");
        }
        if (CareerDataNbt.UNSELECTED_RACE.equals(snapshot.race())) {
            return Component.translatable("careerchronicle.message.select_race_first");
        }
        ClassDef careerClass = registry.classes().get(classId);
        if (careerClass != null && careerClass.hidden()
                && (careerClass.unlockFlag() == null || !snapshot.hiddenFlags().contains(careerClass.unlockFlag()))) {
            return Component.translatable("careerchronicle.ui.class_unavailable_hidden");
        }
        RaceDef race = registry.races().get(snapshot.race());
        if (race != null && !race.allowedClasses().isEmpty() && !race.allowedClasses().contains(classId)) {
            return Component.translatable("careerchronicle.ui.class_unavailable_race",
                    displayRace(registry, snapshot.race()));
        }
        int selectedSegments = snapshot.classHistory().size();
        if (!CareerProgressionMath.hasAlphaSegmentSlot(selectedSegments)) {
            return Component.translatable("careerchronicle.ui.class_unavailable_segments_full",
                    CareerProgressionMath.MAX_ALPHA_SEGMENTS);
        }
        int requiredLevel = CareerProgressionMath.requiredLevelForNextSegment(selectedSegments);
        if (snapshot.careerLevel() < requiredLevel) {
            return Component.translatable("careerchronicle.ui.class_unavailable_level",
                    CareerProgressionMath.nextSegmentIndex(selectedSegments), requiredLevel);
        }
        if (careerClass != null && !careerClass.requiredAttributes().isEmpty()) {
            for (var entry : careerClass.requiredAttributes().entrySet()) {
                int current = snapshot.attributes().getOrDefault(entry.getKey(), 5);
                if (current < entry.getValue()) {
                    String attrName = Component.translatable("careerchronicle.attr." + entry.getKey()).getString();
                    return Component.translatable("careerchronicle.ui.class_unavailable_attr",
                            attrName + " " + current + "/" + entry.getValue());
                }
            }
        }
        return Component.literal("");
    }

    private void renderManualPage(GuiGraphics graphics, RegistrySnapshot registry, CareerDataSnapshot snapshot, int left, int top) {
        int x = left + 14;
        int y = top + MANUAL_CONTENT_TOP;
        int width = panelWidth() - 28;
        int rows = manualVisibleRows();
        List<ManualEntry> entries = manualEntries(registry, snapshot);
        int localOffset = clampManualScroll(manualScrollOffset, entries.size());
        graphics.drawString(this.font, Component.translatable(manualPage.titleKey), x, y, 0xFFFFD37A, false);
        graphics.drawString(this.font, Component.translatable(manualPage.hintKey), x + 118, y, 0xFF92A4B8, false);
        y += 16;

        if (entries.isEmpty()) {
            graphics.fill(x, y, x + width, y + 38, 0x66182330);
            graphics.drawString(this.font, Component.translatable("careerchronicle.ui.manual_empty"),
                    x + 8, y + 14, 0xFF92A4B8, false);
            return;
        }

        int to = Math.min(entries.size(), localOffset + rows);
        for (int i = localOffset; i < to; i++) {
            ManualEntry entry = entries.get(i);
            int rowY = y + (i - localOffset) * MANUAL_ROW_HEIGHT;
            graphics.fill(x, rowY, x + width, rowY + MANUAL_ROW_HEIGHT - 3, entry.accentColor() & 0x55FFFFFF);
            graphics.fill(x, rowY, x + 3, rowY + MANUAL_ROW_HEIGHT - 3, entry.accentColor());
            if (entry.iconType() == ManualIcon.RACE) {
                SkillIconRenderer.renderRace(graphics, entry.iconId(), x + 8, rowY + 7, 20);
            } else if (entry.iconType() == ManualIcon.CLASS) {
                SkillIconRenderer.renderClass(graphics, entry.iconId(), x + 8, rowY + 7, 20);
            } else if (entry.iconType() == ManualIcon.SKILL) {
                SkillIconRenderer.render(graphics, entry.iconId(), x + 8, rowY + 7, 20);
            } else {
                graphics.drawString(this.font, entry.fallbackIcon(), x + 14, rowY + 13, entry.accentColor(), false);
            }
            graphics.drawString(this.font, trim(entry.title().getString(), 34), x + 36, rowY + 6, 0xFFE8F0FF, false);
            graphics.drawString(this.font, trim(entry.lineOne().getString(), 64), x + 36, rowY + 17, 0xFFC8D4E6, false);
            graphics.drawString(this.font, trim(entry.lineTwo().getString(), 64), x + 36, rowY + 28, 0xFF92A4B8, false);
        }
        renderManualScrollbar(graphics, left, top, entries.size(), localOffset);
    }

    private List<ManualEntry> manualEntries(RegistrySnapshot registry, CareerDataSnapshot snapshot) {
        return switch (manualPage) {
            case RACES -> raceManualEntries(registry, snapshot);
            case CLASSES -> classManualEntries(registry, snapshot);
            case SKILLS -> skillManualEntries(registry, snapshot);
            case CLUES -> clueManualEntries(registry, snapshot);
            case FUSIONS -> fusionManualEntries(registry, snapshot);
            default -> List.of();
        };
    }

    private List<ManualEntry> raceManualEntries(RegistrySnapshot registry, CareerDataSnapshot snapshot) {
        List<ManualEntry> entries = new ArrayList<>();
        for (RaceDef race : registry.races().values()) {
            boolean selected = race.id().equals(snapshot.race());
            entries.add(new ManualEntry(
                    ManualIcon.RACE,
                    race.id(),
                    Component.translatable(race.displayKey()),
                    Component.translatable("careerchronicle.ui.manual_race_classes", joinDisplayNames(registry, race.allowedClasses(), ManualIcon.CLASS, 42)),
                    Component.translatable("careerchronicle.ui.manual_race_traits", joinTraitNames(race.traits(), 42)),
                    selected ? 0xFF55C7F7 : 0xFF4C6078,
                    Component.literal("")
            ));
        }
        return sortedManualEntries(entries);
    }

    private List<ManualEntry> classManualEntries(RegistrySnapshot registry, CareerDataSnapshot snapshot) {
        List<ManualEntry> entries = new ArrayList<>();
        for (ClassDef careerClass : registry.classes().values()) {
            boolean knownHidden = !careerClass.hidden()
                    || careerClass.unlockFlag() != null && snapshot.hiddenFlags().contains(careerClass.unlockFlag());
            if (!knownHidden) {
                continue;
            }
            int count = classCounts(snapshot.classHistory()).getOrDefault(careerClass.id(), 0);
            entries.add(new ManualEntry(
                    ManualIcon.CLASS,
                    careerClass.id(),
                    Component.translatable(careerClass.displayKey()),
                    Component.translatable("careerchronicle.ui.manual_class_skills",
                            joinDisplayNames(registry, careerClass.grantsSkills(), ManualIcon.SKILL, 44)),
                    Component.translatable("careerchronicle.ui.manual_class_tags",
                            joinIds(careerClass.tags(), 44), count),
                    careerClass.hidden() ? 0xFFFF78E8 : count > 0 ? 0xFFFFD37A : 0xFF4C6078,
                    Component.literal("")
            ));
        }
        return sortedManualEntries(entries);
    }

    private List<ManualEntry> skillManualEntries(RegistrySnapshot registry, CareerDataSnapshot snapshot) {
        List<ManualEntry> entries = new ArrayList<>();
        for (SkillDef skill : registry.skills().values()) {
            if (!snapshot.unlockedSkills().contains(skill.id())) {
                continue;
            }
            entries.add(new ManualEntry(
                    ManualIcon.SKILL,
                    skill.id(),
                    Component.translatable(skill.displayKey()),
                    Component.translatable("careerchronicle.ui.manual_skill_meta",
                            skillTypeName(registry, skill.id()), skillCooldownText(registry, skill.id()), skillResourceText(registry, skill.id())),
                    Component.translatable("careerchronicle.ui.skill_equipment_requirement",
                            SkillEquipmentRequirements.requirementText(skill)),
                    skillTypeAccent(skill.type()),
                    Component.literal("")
            ));
        }
        return sortedManualEntries(entries);
    }

    private List<ManualEntry> clueManualEntries(RegistrySnapshot registry, CareerDataSnapshot snapshot) {
        List<ManualEntry> entries = new ArrayList<>();
        for (HiddenUnlockDef hiddenUnlock : registry.hiddenUnlocks().values()) {
            boolean revealed = snapshot.hiddenFlags().contains(hiddenUnlock.unlockFlag());
            entries.add(new ManualEntry(
                    ManualIcon.NONE,
                    null,
                    Component.translatable(hiddenUnlock.displayKey()),
                    Component.translatable(revealed ? hiddenUnlock.revealedKey() : hiddenUnlock.clueKey()),
                    Component.translatable("careerchronicle.ui.manual_clue_requirements",
                            requirementSummary(registry, hiddenUnlock.requiredClassCounts(), hiddenUnlock.requiredTagScores())),
                    revealed ? 0xFFFF78E8 : 0xFF4C6078,
                    Component.literal(revealed ? "!" : "?")
            ));
        }
        return sortedManualEntries(entries);
    }

    private List<ManualEntry> fusionManualEntries(RegistrySnapshot registry, CareerDataSnapshot snapshot) {
        List<ManualEntry> entries = new ArrayList<>();
        for (FusionDef fusion : registry.fusions().values()) {
            SkillDef skill = registry.skills().get(fusion.unlockSkill());
            boolean unlocked = snapshot.unlockedSkills().contains(fusion.unlockSkill());
            entries.add(new ManualEntry(
                    skill == null ? ManualIcon.NONE : ManualIcon.SKILL,
                    fusion.unlockSkill(),
                    skill == null ? Component.translatable("careerchronicle.skill." + fusion.unlockSkill().getPath()) : Component.translatable(skill.displayKey()),
                    Component.translatable("careerchronicle.ui.manual_fusion_requires",
                            requirementSummary(registry, fusion.requiredClassCounts(), fusion.requiredTagScores())),
                    Component.translatable(unlocked ? "careerchronicle.ui.manual_fusion_unlocked" : "careerchronicle.ui.manual_fusion_locked"),
                    unlocked ? 0xFFFFCF5A : 0xFF4C6078,
                    Component.literal("*")
            ));
        }
        return sortedManualEntries(entries);
    }

    private List<ManualEntry> sortedManualEntries(List<ManualEntry> entries) {
        entries.sort(Comparator.comparing(entry -> entry.title().getString()));
        return entries;
    }

    private int skillTypeAccent(String type) {
        return switch (type) {
            case "fusion" -> 0xFFFFCF5A;
            case "hidden" -> 0xFFFF78E8;
            case "passive" -> 0xFF8DEFA2;
            case "ultimate" -> 0xFFFF645A;
            case "race" -> 0xFF9BD2FF;
            default -> 0xFF6BCBFF;
        };
    }

    private String joinDisplayNames(RegistrySnapshot registry, List<ResourceLocation> ids, ManualIcon type, int maxLength) {
        if (ids.isEmpty()) {
            return "-";
        }
        List<String> names = new ArrayList<>();
        for (ResourceLocation id : ids) {
            if (type == ManualIcon.CLASS) {
                names.add(displayClass(registry, id).getString());
            } else if (type == ManualIcon.SKILL) {
                names.add(displaySkill(registry, id).getString());
            } else {
                names.add(id.getPath());
            }
        }
        return trim(String.join(", ", names), maxLength);
    }

    private String joinTraitNames(List<ResourceLocation> traits, int maxLength) {
        if (traits.isEmpty()) {
            return "-";
        }
        List<String> names = new ArrayList<>();
        for (ResourceLocation trait : traits) {
            names.add(Component.translatable("careerchronicle.trait." + trait.getPath()).getString());
        }
        return trim(String.join(", ", names), maxLength);
    }

    private String requirementSummary(
            RegistrySnapshot registry,
            Map<ResourceLocation, Integer> classRequirements,
            Map<ResourceLocation, Integer> tagRequirements
    ) {
        List<String> parts = new ArrayList<>();
        classRequirements.forEach((classId, count) ->
                parts.add(displayClass(registry, classId).getString() + " x" + count));
        tagRequirements.forEach((tagId, score) ->
                parts.add(Component.translatable("careerchronicle.tag." + tagId.getPath()).getString() + " +" + score));
        return parts.isEmpty() ? "-" : trim(String.join(", ", parts), 56);
    }

    private void renderManualScrollbar(GuiGraphics graphics, int left, int top, int entryCount, int scrollOffset) {
        int rows = manualVisibleRows();
        if (entryCount <= rows) {
            return;
        }
        int x = left + panelWidth() - 18;
        int y = top + MANUAL_CONTENT_TOP + 16;
        int height = rows * MANUAL_ROW_HEIGHT - 4;
        int thumbHeight = Math.max(12, height * rows / entryCount);
        int maxOffset = maxManualScroll(entryCount);
        int thumbY = y + (height - thumbHeight) * scrollOffset / Math.max(1, maxOffset);
        graphics.fill(x, y, x + 2, y + height, 0x662A3440);
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xCC8FA6C8);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (manualPage != ManualPage.OVERVIEW) {
            RegistrySnapshot registry = CareerRegistry.snapshot();
            int entryCount = manualEntries(registry, snapshot()).size();
            if (isInManualList(mouseX, mouseY) && entryCount > manualVisibleRows()) {
                int nextOffset = Mth.clamp(manualScrollOffset + (delta > 0.0D ? -1 : 1), 0, maxManualScroll(entryCount));
                if (nextOffset != manualScrollOffset) {
                    manualScrollOffset = nextOffset;
                    rebuildWidgets();
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean isInManualList(double mouseX, double mouseY) {
        int left = panelLeft();
        int top = panelTop();
        return mouseX >= left + 14
                && mouseX <= left + panelWidth() - 14
                && mouseY >= top + MANUAL_CONTENT_TOP + 16
                && mouseY <= top + panelHeight() - 10;
    }

    private int manualVisibleRows() {
        return Math.max(1, (panelHeight() - MANUAL_CONTENT_TOP - 32) / MANUAL_ROW_HEIGHT);
    }

    private int clampManualScroll(int scrollOffset, int entryCount) {
        return Mth.clamp(scrollOffset, 0, maxManualScroll(entryCount));
    }

    private int maxManualScroll(int entryCount) {
        return Math.max(0, entryCount - manualVisibleRows());
    }

    private ResourceLocation previewClassId(RegistrySnapshot registry, CareerDataSnapshot snapshot) {
        List<ResourceLocation> allowed = classOptions(registry, snapshot);
        if (selectedClassId != null && allowed.contains(selectedClassId)) {
            return selectedClassId;
        }
        return allowed.isEmpty() ? null : allowed.get(0);
    }

    private Component displayRace(RegistrySnapshot registry, ResourceLocation raceId) {
        if (CareerDataNbt.UNSELECTED_RACE.equals(raceId)) {
            return Component.translatable("careerchronicle.race.unselected").withStyle(ChatFormatting.GRAY);
        }
        RaceDef race = registry.races().get(raceId);
        return race == null ? Component.translatable("careerchronicle.race." + raceId.getPath()) :
                Component.translatable(race.displayKey());
    }

    private Component displayClass(RegistrySnapshot registry, ResourceLocation classId) {
        ClassDef careerClass = registry.classes().get(classId);
        return careerClass == null ? Component.translatable("careerchronicle.class." + classId.getPath()) :
                Component.translatable(careerClass.displayKey());
    }

    private Component displaySkill(RegistrySnapshot registry, ResourceLocation skillId) {
        return registry.skill(skillId)
                .map(skill -> Component.translatable(skill.displayKey()))
                .orElseGet(() -> Component.translatable("careerchronicle.skill." + skillId.getPath()));
    }

    private Component skillTooltip(RegistrySnapshot registry, ResourceLocation skillId, boolean equipmentBlocked) {
        Component base = Component.translatable("careerchronicle.ui.skill_tooltip",
                displaySkill(registry, skillId),
                skillTypeName(registry, skillId),
                skillCooldownText(registry, skillId),
                skillResourceText(registry, skillId));
        SkillDef skill = registry.skills().get(skillId);
        if (skill == null) {
            return base;
        }
        if (!skill.requirements().hasEquipmentTags()) {
            return equipmentBlocked
                    ? base.copy().append("\n").append(Component.translatable("careerchronicle.ui.skill_equipment_unmet",
                    SkillEquipmentRequirements.requirementText(skill)).withStyle(ChatFormatting.YELLOW))
                    : base;
        }
        Component tooltip = base.copy().append("\n").append(Component.translatable(
                "careerchronicle.ui.skill_equipment_requirement",
                SkillEquipmentRequirements.requirementText(skill)
        ));
        if (equipmentBlocked) {
            tooltip = tooltip.copy().append("\n").append(Component.translatable("careerchronicle.ui.skill_equipment_unmet",
                    SkillEquipmentRequirements.requirementText(skill)).withStyle(ChatFormatting.YELLOW));
        }
        return tooltip;
    }

    private Component skillTypeName(RegistrySnapshot registry, ResourceLocation skillId) {
        return registry.skill(skillId)
                .map(skill -> Component.translatable("careerchronicle.skill_type." + skill.type()))
                .orElseGet(() -> Component.translatable("careerchronicle.skill_type.active"));
    }

    private Component skillCooldownText(RegistrySnapshot registry, ResourceLocation skillId) {
        return registry.skill(skillId)
                .map(skill -> Component.translatable("careerchronicle.ui.skill_cooldown_seconds",
                        formatSeconds(skill.cooldownTicks())))
                .orElseGet(() -> Component.translatable("careerchronicle.ui.skill_cooldown_seconds", "0.0"));
    }

    private Component skillResourceText(RegistrySnapshot registry, ResourceLocation skillId) {
        SkillDef skill = registry.skills().get(skillId);
        if (skill == null || skill.resourceCost() <= 0 || "none".equals(skill.resource())) {
            return Component.translatable("careerchronicle.resource.none");
        }
        return Component.translatable("careerchronicle.ui.skill_resource_cost",
                skill.resourceCost(), Component.translatable("careerchronicle.resource." + skill.resource()));
    }

    private String formatSeconds(int cooldownTicks) {
        return String.format(java.util.Locale.ROOT, "%.1f", cooldownTicks / 20.0F);
    }

    private String joinIds(List<ResourceLocation> ids, int maxLength) {
        if (ids.isEmpty()) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        for (ResourceLocation id : ids) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(Component.translatable("careerchronicle.tag." + id.getPath()).getString());
            if (builder.length() > maxLength) {
                return builder.substring(0, Math.max(0, maxLength - 3)) + "...";
            }
        }
        return builder.toString();
    }

    private Map<ResourceLocation, Integer> classCounts(List<ResourceLocation> history) {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (ResourceLocation classId : history) {
            counts.merge(classId, 1, Integer::sum);
        }
        return counts;
    }

    private String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private int nextSegment(CareerDataSnapshot snapshot) {
        return CareerProgressionMath.nextSegmentIndex(snapshot.classHistory().size());
    }

    private void addOverviewTabs(int left, int top) {
        int pw = panelWidth();
        int tabW = Math.min(38, (pw - 80) / ManualPage.values().length);
        int x = left + 80;
        for (ManualPage page : ManualPage.values()) {
            Component label = Component.translatable(page.labelKey);
            if (manualPage == page) {
                label = label.copy().withStyle(ChatFormatting.GOLD);
            }
            Button btn = Button.builder(label, button -> {
                if (manualPage != page) { manualPage = page; manualScrollOffset = 0; rebuildWidgets(); }
            }).bounds(x, top + 3, tabW, 14).build();
            addRenderableWidget(btn);
            x += tabW + 2;
        }
    }

    private void renderOverviewSimple(GuiGraphics graphics, RegistrySnapshot registry, CareerDataSnapshot snapshot, int left, int top) {
        int pw = panelWidth();
        int ph = panelHeight();
        int leftColW = pw * 30 / 100;
        int rightX = left + leftColW + 2;
        int rightW = pw - leftColW - 10;

        graphics.drawString(this.font, Component.translatable("careerchronicle.ui.race",
                displayRace(registry, snapshot.race())), left + 8, top + 24, 0xFFE8F0FF, false);
        String levelInfo = "Lv." + snapshot.careerLevel() + " " + snapshot.careerXp() + "/" +
                CareerProgressionMath.xpForNextLevel(snapshot.careerLevel());
        graphics.drawString(this.font, levelInfo, left + pw - this.font.width(levelInfo) - 8, top + 24, 0xFFB8C8E0, false);

        graphics.fill(left + 4, top + 36, left + leftColW - 2, top + ph - 4, 0x44182330);
        graphics.fill(rightX - 2, top + 36, left + pw - 4, top + ph - 4, 0x44182330);

        ResourceLocation classId = previewClassId(registry, snapshot);
        if (classId == null) {
            graphics.drawString(this.font, Component.translatable("careerchronicle.ui.no_class_preview"),
                    rightX + 6, top + 50, 0xFF92A4B8, false);
            return;
        }

        ClassDef classDef = registry.classes().get(classId);
        SkillIconRenderer.renderClass(graphics, classId, rightX + 4, top + 42, 16);
        graphics.drawString(this.font, displayClass(registry, classId), rightX + 24, top + 44, 0xFFE8F0FF, false);

        if (classDef != null) {
            String tags = joinIds(classDef.tags(), 30);
            graphics.drawString(this.font, tags, rightX + 6, top + 58, 0xFF92A4B8, false);

            int skillY = top + 72;
            for (ResourceLocation skillId : classDef.grantsSkills()) {
                if (skillY > top + ph - 36) break;
                SkillDef skill = registry.skills().get(skillId);
                if (skill == null) continue;
                String name = Component.translatable(skill.displayKey()).getString();
                String cd = String.format("%.0fs", skill.cooldownTicks() / 20.0);
                String res = skill.resourceCost() > 0 ?
                        skill.resourceCost() + Component.translatable("careerchronicle.resource_short." + skill.resource()).getString() : "";
                SkillIconRenderer.render(graphics, skillId, rightX + 4, skillY, 10);
                graphics.drawString(this.font, trim(name + " " + cd + " " + res, 24), rightX + 18, skillY + 1, 0xFFB8C8E0, false);
                skillY += 12;
            }

            boolean selectable = canSelectClass(registry, snapshot, classId);
            if (!selectable) {
                Component reason = classUnavailableReason(registry, snapshot, classId);
                graphics.drawString(this.font, trim(reason.getString(), 24),
                        rightX + 6, top + ph - 34, 0xFFFF8C7A, false);
            }
        }
    }


    private int panelWidth() {
        return Math.min(MAX_PANEL_WIDTH, this.width - 10);
    }

    private int panelHeight() {
        return Math.min(MAX_PANEL_HEIGHT, this.height - 8);
    }

    private int panelLeft() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (this.height - panelHeight()) / 2;
    }

}
