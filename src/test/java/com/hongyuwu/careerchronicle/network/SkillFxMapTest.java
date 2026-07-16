package com.hongyuwu.careerchronicle.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Static checks on tools/skill_fx_map.json — the single input driving
 * fill_skill_fx.py's batch fill (0.4-05a). Gradle's `test` task runs with
 * workingDir = project root, so these paths resolve without any extra
 * configuration.
 */
class SkillFxMapTest {
    private static final Path MAP_PATH = Path.of("tools/skill_fx_map.json");
    private static final Path SKILLS_DIR = Path.of(
            "src/main/resources/data/careerchronicle/careerchronicle/skills");

    private static final Set<String> TEN_FAMILIES = Set.of(
            "fire", "frost", "holy", "dark", "melee", "defense", "bow", "stealth", "arcane", "nature");

    // CareerSounds.java's 24 registered sound ids, fully namespaced.
    private static final Set<String> REGISTERED_SOUNDS = Set.of(
            "careerchronicle:skill.cast.fire", "careerchronicle:skill.cast.frost",
            "careerchronicle:skill.cast.holy", "careerchronicle:skill.cast.dark",
            "careerchronicle:skill.cast.blade", "careerchronicle:skill.cast.shield",
            "careerchronicle:skill.cast.arrow", "careerchronicle:skill.cast.shadow",
            "careerchronicle:skill.cast.arcane", "careerchronicle:skill.cast.nature",
            "careerchronicle:skill.hit.fire", "careerchronicle:skill.hit.frost",
            "careerchronicle:skill.hit.holy", "careerchronicle:skill.hit.dark",
            "careerchronicle:skill.hit.physical",
            "careerchronicle:ui.chronicle_open", "careerchronicle:ui.skill_equip",
            "careerchronicle:ui.tab_flip", "careerchronicle:ui.deny",
            "careerchronicle:event.level_up", "careerchronicle:event.segment_choice",
            "careerchronicle:event.fusion_unlock", "careerchronicle:event.hidden_unlock",
            "careerchronicle:event.skill_upgrade"
    );

    private static final Set<String> FIVE_HIT_SOUNDS = Set.of(
            "careerchronicle:skill.hit.fire", "careerchronicle:skill.hit.frost",
            "careerchronicle:skill.hit.holy", "careerchronicle:skill.hit.dark",
            "careerchronicle:skill.hit.physical"
    );

    private static JsonObject mapping;

    @BeforeAll
    static void loadMapping() throws IOException {
        assertTrue(Files.exists(MAP_PATH), "tools/skill_fx_map.json not found at " + MAP_PATH.toAbsolutePath()
                + " — run `python3 tools/fill_skill_fx.py` first, or check the JUnit working directory");
        try (Reader reader = Files.newBufferedReader(MAP_PATH, StandardCharsets.UTF_8)) {
            mapping = JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static List<JsonObject> skillRows() {
        JsonArray skills = mapping.getAsJsonArray("skills");
        return skills.asList().stream().map(JsonElement::getAsJsonObject).toList();
    }

    // D1: 覆盖完整性
    @Test
    void mapping_coversExactlyTheSkillsDirectory() throws IOException {
        List<JsonObject> rows = skillRows();
        assertEquals(73, rows.size(), "expected 73 skill rows");

        Set<String> mappedIds = rows.stream()
                .map(row -> row.get("skill_id").getAsString())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> onDiskIds;
        try (var files = Files.list(SKILLS_DIR)) {
            onDiskIds = files
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> "careerchronicle:" + p.getFileName().toString().replace(".json", ""))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        assertEquals(onDiskIds, mappedIds);
    }

    // D2: 家族合法性
    @Test
    void everyRow_hasValidFamily_andNeverUsesIceAlias() {
        for (JsonObject row : skillRows()) {
            String family = row.get("family").getAsString();
            assertTrue(TEN_FAMILIES.contains(family),
                    row.get("skill_id").getAsString() + " has invalid family '" + family + "'");
            assertNotEquals("ice", family,
                    row.get("skill_id").getAsString() + " uses raw 'ice' instead of the 'frost' family alias");
        }
    }

    // D3: 音效引用合法性
    @Test
    void everyRow_castAndHitSound_areRegisteredSoundIds() {
        for (JsonObject row : skillRows()) {
            String castSound = row.get("cast_sound").getAsString();
            String hitSound = row.get("hit_sound").getAsString();
            assertTrue(REGISTERED_SOUNDS.contains(castSound),
                    "unregistered cast_sound: " + castSound);
            assertTrue(REGISTERED_SOUNDS.contains(hitSound),
                    "unregistered hit_sound: " + hitSound);
        }
    }

    // D4: 命中音折叠闭合
    @Test
    void hitSound_foldsToExactlyFiveValues() {
        Set<String> distinctHitSounds = skillRows().stream()
                .map(row -> row.get("hit_sound").getAsString())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertEquals(FIVE_HIT_SOUNDS, distinctHitSounds);
    }

    // D5: D2 方案 A 一致性（已拍板）
    @Test
    void reservedSounds_excludedFromEverySkillRow_andExactlyTwoEntries() {
        JsonArray reserved = mapping.getAsJsonArray("reserved_sounds");
        Set<String> reservedSet = reserved.asList().stream().map(JsonElement::getAsString)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertEquals(Set.of("careerchronicle:skill.cast.arcane", "careerchronicle:skill.cast.nature"), reservedSet);

        for (JsonObject row : skillRows()) {
            String castSound = row.get("cast_sound").getAsString();
            assertFalse(reservedSet.contains(castSound),
                    row.get("skill_id").getAsString() + " uses a reserved (备案待消费) sound: " + castSound);
        }
    }
}
