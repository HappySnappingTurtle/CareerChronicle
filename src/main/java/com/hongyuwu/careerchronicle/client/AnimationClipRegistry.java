package com.hongyuwu.careerchronicle.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阶段3-任务4-设计文档-接入驱动与移除旧库.md §二. In-memory lookup table of {@link AnimationClip}s by
 * their {@code id}, populated by scanning {@code assets/<namespace>/custom_animation/*.json} via
 * {@link #reload}. Deliberately a new directory, not the old {@code player_animation/} one --
 * that was player-animation-lib's own auto-discovery path and this task removes that library
 * entirely (阶段3-任务4-设计文档 §二 "移除 player-animation-lib").
 */
public final class AnimationClipRegistry {

    private static final String DIRECTORY = "custom_animation";
    private static final Map<String, AnimationClip> CLIPS = new ConcurrentHashMap<>();

    private AnimationClipRegistry() {
    }

    /** Wires {@link #reload} into Minecraft's normal client resource reload cycle (initial load
     * at startup + every {@code /reload}) via the correct Forge 1.20.1 hook, confirmed by
     * decompiling the real event class (not assumed) -- {@code RegisterClientReloadListenersEvent}
     * lives under {@code net.minecraftforge.client.event}, not the more generic
     * {@code net.minecraftforge.event} package one might guess first. */
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) AnimationClipRegistry::reload);
    }

    /** Manual registration (used by {@link #reload} and directly by tests). Overwrites any
     * existing entry with the same id -- the natural behavior a {@code /reload} needs. */
    public static void register(AnimationClip clip) {
        CLIPS.put(clip.id(), clip);
    }

    public static AnimationClip get(String id) {
        return CLIPS.get(id);
    }

    /**
     * Rescans {@code assets/<namespace>/custom_animation/*.json} across every loaded resource
     * pack and replaces the registry contents. A malformed individual file is logged and skipped
     * (via {@link AnimationClipParser}'s own no-throw contract) rather than aborting the whole
     * reload -- one bad animation file must not take every other animation down with it.
     */
    public static void reload(ResourceManager resourceManager) {
        Map<String, AnimationClip> loaded = new ConcurrentHashMap<>();
        Map<ResourceLocation, Resource> found =
                resourceManager.listResources(DIRECTORY, location -> location.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            AnimationClip clip = parseOne(entry.getKey(), entry.getValue());
            if (clip != null) {
                loaded.put(clip.id(), clip);
            }
        }
        CLIPS.clear();
        CLIPS.putAll(loaded);
        CareerChronicleMod.LOGGER.info("[CareerChronicle] AnimationClipRegistry loaded {} custom animation(s).",
                loaded.size());
    }

    private static AnimationClip parseOne(ResourceLocation path, Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                CareerChronicleMod.LOGGER.error(
                        "[CareerChronicle] AnimationClipRegistry: '{}' is not a JSON object, skipping.", path);
                return null;
            }
            return AnimationClipParser.parse(root.getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            CareerChronicleMod.LOGGER.error(
                    "[CareerChronicle] AnimationClipRegistry: failed to read/parse '{}', skipping.", path, e);
            return null;
        }
    }
}
