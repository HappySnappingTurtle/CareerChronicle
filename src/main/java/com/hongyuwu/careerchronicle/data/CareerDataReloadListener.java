package com.hongyuwu.careerchronicle.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.CareerChronicleMod;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class CareerDataReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    public CareerDataReloadListener() {
        super(GSON, "careerchronicle");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonElements,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        RegistrySnapshot oldSnapshot = CareerRegistry.snapshot();
        try {
            RegistrySnapshot nextSnapshot = parse(oldSnapshot.version() + 1L, jsonElements);
            CareerRegistry.replace(nextSnapshot);
            CareerChronicleMod.LOGGER.info(
                    "Loaded Career Chronicle registry v{}: {} races, {} classes, {} skills, {} fusions, {} hidden unlocks, {} XP sources, {} fx templates, {} validation warnings.",
                    nextSnapshot.version(),
                    nextSnapshot.races().size(),
                    nextSnapshot.classes().size(),
                    nextSnapshot.skills().size(),
                    nextSnapshot.fusions().size(),
                    nextSnapshot.hiddenUnlocks().size(),
                    nextSnapshot.xpSources().size(),
                    nextSnapshot.fxTemplates().size(),
                    nextSnapshot.validationWarnings().size()
            );
        } catch (RuntimeException exception) {
            CareerChronicleMod.LOGGER.error(
                    "Career Chronicle registry reload failed; keeping registry v{}.",
                    oldSnapshot.version(),
                    exception
            );
        }
    }

    private static RegistrySnapshot parse(long version, Map<ResourceLocation, JsonElement> jsonElements) {
        Map<ResourceLocation, RaceDef> races = new LinkedHashMap<>();
        Map<ResourceLocation, ClassDef> classes = new LinkedHashMap<>();
        Map<ResourceLocation, SkillDef> skills = new LinkedHashMap<>();
        Map<ResourceLocation, FusionDef> fusions = new LinkedHashMap<>();
        Map<ResourceLocation, HiddenUnlockDef> hiddenUnlocks = new LinkedHashMap<>();
        Map<ResourceLocation, XpSourceDef> xpSources = new LinkedHashMap<>();
        Map<String, java.util.List<FxComponent>> fxTemplates = new LinkedHashMap<>();

        // Pass 1: fx_templates. Must fully finish before any skill is parsed
        // (Pass 2), since a skill's fx_template reference needs the complete
        // template map to resolve against -- this two-pass split is what makes
        // template lookups independent of jsonElements' (non-lexicographic,
        // effectively HashMap-ordered) iteration order (0.4-06 §2.2 B6).
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonElements.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String path = fileId.getPath();
            if (!path.startsWith("fx_templates/")) {
                continue;
            }
            try {
                String templateName = trimFolder(fileId, "fx_templates/").getPath();
                fxTemplates.put(templateName, CareerDataParsers.fxTemplate(fileId, entry.getValue()));
            } catch (RegistryValidationException exception) {
                throw new RegistryValidationException("Invalid Career Chronicle data file " + fileId + ": "
                        + exception.getMessage(), exception);
            }
        }

        // Pass 2: everything else (skills may now reference the fxTemplates built above).
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonElements.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String path = fileId.getPath();
            if (path.startsWith("fx_templates/")) {
                continue;
            }
            try {
                JsonObject json = asObject(fileId, entry.getValue());
                if (path.startsWith("races/")) {
                    ResourceLocation id = trimFolder(fileId, "races/");
                    races.put(id, CareerDataParsers.race(id, json));
                } else if (path.startsWith("classes/")) {
                    ResourceLocation id = trimFolder(fileId, "classes/");
                    classes.put(id, CareerDataParsers.careerClass(id, json));
                } else if (path.startsWith("skills/")) {
                    ResourceLocation id = trimFolder(fileId, "skills/");
                    skills.put(id, CareerDataParsers.skill(id, json, fxTemplates));
                } else if (path.startsWith("fusions/")) {
                    ResourceLocation id = trimFolder(fileId, "fusions/");
                    fusions.put(id, CareerDataParsers.fusion(id, json));
                } else if (path.startsWith("hidden_unlocks/")) {
                    ResourceLocation id = trimFolder(fileId, "hidden_unlocks/");
                    hiddenUnlocks.put(id, CareerDataParsers.hiddenUnlock(id, json));
                } else if (path.startsWith("xp_sources/")) {
                    ResourceLocation id = trimFolder(fileId, "xp_sources/");
                    xpSources.put(id, CareerDataParsers.xpSource(id, json));
                }
            } catch (RegistryValidationException exception) {
                throw new RegistryValidationException("Invalid Career Chronicle data file " + fileId + ": "
                        + exception.getMessage(), exception);
            }
        }

        java.util.List<String> warnings = new java.util.ArrayList<>();
        CareerRegistryValidator.validate(races, classes, skills, fusions, hiddenUnlocks, xpSources, warnings);
        for (String warning : warnings) {
            CareerChronicleMod.LOGGER.warn(warning);
        }
        return new RegistrySnapshot(version, races, classes, skills, fusions, hiddenUnlocks, xpSources, fxTemplates, warnings);
    }

    private static JsonObject asObject(ResourceLocation fileId, JsonElement element) {
        if (!element.isJsonObject()) {
            throw new RegistryValidationException(fileId + " must be a JSON object.");
        }
        return element.getAsJsonObject();
    }

    private static ResourceLocation trimFolder(ResourceLocation fileId, String folder) {
        return ResourceLocation.fromNamespaceAndPath(
                fileId.getNamespace(),
                fileId.getPath().substring(folder.length())
        );
    }
}
