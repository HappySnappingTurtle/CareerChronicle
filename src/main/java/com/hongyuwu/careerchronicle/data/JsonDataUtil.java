package com.hongyuwu.careerchronicle.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

final class JsonDataUtil {
    private JsonDataUtil() {
    }

    static ResourceLocation id(String raw) {
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        if (parsed == null) {
            throw new RegistryValidationException("Invalid resource id: " + raw);
        }
        return parsed;
    }

    static String string(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new RegistryValidationException("Missing string field: " + key);
        }
        return json.get(key).getAsString();
    }

    static String optionalString(JsonObject json, String key, String fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        if (!json.get(key).isJsonPrimitive()) {
            throw new RegistryValidationException("Expected string field: " + key);
        }
        return json.get(key).getAsString();
    }

    static int optionalInt(JsonObject json, String key, int fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        if (!json.get(key).isJsonPrimitive()) {
            throw new RegistryValidationException("Expected integer field: " + key);
        }
        return json.get(key).getAsInt();
    }

    static double optionalDouble(JsonObject json, String key, double fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        if (!json.get(key).isJsonPrimitive()) {
            throw new RegistryValidationException("Expected number field: " + key);
        }
        return json.get(key).getAsDouble();
    }

    static boolean optionalBoolean(JsonObject json, String key, boolean fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        if (!json.get(key).isJsonPrimitive()) {
            throw new RegistryValidationException("Expected boolean field: " + key);
        }
        return json.get(key).getAsBoolean();
    }

    static List<ResourceLocation> idList(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }
        if (!json.get(key).isJsonArray()) {
            throw new RegistryValidationException("Expected array field: " + key);
        }
        List<ResourceLocation> ids = new ArrayList<>();
        JsonArray array = json.getAsJsonArray(key);
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) {
                throw new RegistryValidationException("Expected resource id string in array field: " + key);
            }
            ids.add(id(element.getAsString()));
        }
        return ids;
    }

    static List<String> stringList(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }
        if (!json.get(key).isJsonArray()) {
            throw new RegistryValidationException("Expected array field: " + key);
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString());
            }
        }
        return result;
    }

    static Map<ResourceLocation, Integer> idIntMap(JsonObject json, String key) {
        if (!json.has(key)) {
            return Map.of();
        }
        if (!json.get(key).isJsonObject()) {
            throw new RegistryValidationException("Expected object field: " + key);
        }
        Map<ResourceLocation, Integer> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(key).entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) {
                throw new RegistryValidationException("Expected integer count for " + key + "." + entry.getKey());
            }
            int count = entry.getValue().getAsInt();
            if (count <= 0) {
                throw new RegistryValidationException("Count must be positive for " + key + "." + entry.getKey());
            }
            values.put(id(entry.getKey()), count);
        }
        return values;
    }

    static List<JsonObject> objectList(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }
        if (!json.get(key).isJsonArray()) {
            throw new RegistryValidationException("Expected array field: " + key);
        }
        List<JsonObject> objects = new ArrayList<>();
        JsonArray array = json.getAsJsonArray(key);
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new RegistryValidationException("Expected object in array field: " + key);
            }
            objects.add(element.getAsJsonObject());
        }
        return objects;
    }
}
