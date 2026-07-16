package com.hongyuwu.careerchronicle.data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record RegistrySnapshot(
        long version,
        Map<ResourceLocation, RaceDef> races,
        Map<ResourceLocation, ClassDef> classes,
        Map<ResourceLocation, SkillDef> skills,
        Map<ResourceLocation, FusionDef> fusions,
        Map<ResourceLocation, HiddenUnlockDef> hiddenUnlocks,
        Map<ResourceLocation, XpSourceDef> xpSources,
        Map<String, List<FxComponent>> fxTemplates,
        // 0.4-07: non-fatal validation warnings (CareerRegistryValidator's
        // warnings channel -- e.g. "castable skill declares no fx") queryable
        // from the snapshot itself, not just the server log. Previously these
        // were only LOGGER.warn()'d in CareerDataReloadListener, with no
        // programmatic way for a GameTest/tool to ask "what does the current
        // registry currently warn about" without scraping log text.
        List<String> validationWarnings
) {
    public static final RegistrySnapshot EMPTY = new RegistrySnapshot(
            0L,
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
    );

    public RegistrySnapshot {
        races = Map.copyOf(races);
        classes = Map.copyOf(classes);
        skills = Map.copyOf(skills);
        fusions = Map.copyOf(fusions);
        hiddenUnlocks = Map.copyOf(hiddenUnlocks);
        xpSources = Map.copyOf(xpSources);
        fxTemplates = Map.copyOf(fxTemplates);
        validationWarnings = List.copyOf(validationWarnings);
    }

    public Optional<RaceDef> race(ResourceLocation id) {
        return Optional.ofNullable(races.get(id));
    }

    public Optional<ClassDef> careerClass(ResourceLocation id) {
        return Optional.ofNullable(classes.get(id));
    }

    public Optional<SkillDef> skill(ResourceLocation id) {
        return Optional.ofNullable(skills.get(id));
    }

    public Optional<XpSourceDef> xpSource(ResourceLocation id) {
        return Optional.ofNullable(xpSources.get(id));
    }
}
