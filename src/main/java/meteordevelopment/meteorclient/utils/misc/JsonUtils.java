/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class JsonUtils {
    
    private static final Gson GSON = new Gson();
    
    private JsonUtils() {}
    
    public static <T extends ISerializable<?>> JsonArray listToJson(Iterable<T> list) {
        JsonArray jsonArray = new JsonArray();
        
        for (T item : list) {
            jsonArray.add(item.toJson());
        }
        
        return jsonArray;
    }
    
    public static JsonArray listToJson(List<String> list) {
        JsonArray jsonArray = new JsonArray();
        
        for (String string : list) {
            jsonArray.add(string);
        }
        
        return jsonArray;
    }
    
    public static <T> List<T> listFromJson(JsonArray jsonArray, ToValue<T> toItem) {
        List<T> list = new ArrayList<>(jsonArray.size());
        
        for (JsonElement element : jsonArray) {
            T value = toItem.toValue(element);
            if (value != null) {
                list.add(value);
            }
        }
        
        return list;
    }
    
    public static List<String> listFromJson(JsonObject jsonObject, String key) {
        List<String> list = new ArrayList<>();
        
        for (JsonElement element : jsonObject.get(key).getAsJsonArray()) {
            list.add(element.getAsString());
        }
        
        return list;
    }
    
    public static boolean toClipboard(System<?> system) {
        return toClipboard(system.getName(), system.toJson());
    }
    
    public static boolean toClipboard(String name, JsonObject jsonObject) {
        String preClipboard = mc.keyboard.getClipboard();
        try {
            String jsonString = GSON.toJson(jsonObject);
            mc.keyboard.setClipboard(Base64.getEncoder().encodeToString(jsonString.getBytes(StandardCharsets.UTF_8)));
            return true;
        } catch (Exception e) {
            MeteorClient.LOG.error("Error copying {} JSON to clipboard!", name);
            
            OkPrompt.create()
                .title(String.format("Error copying %s JSON to clipboard!", name))
                .message("This shouldn't happen, please report it.")
                .id("json-copying")
                .show();
            
            mc.keyboard.setClipboard(preClipboard);
            return false;
        }
    }
    
    public static boolean fromClipboard(System<?> system) {
        JsonObject clipboard = fromClipboard(system.toJson());
        
        if (clipboard != null) {
            system.fromJson(clipboard);
            return true;
        }
        
        return false;
    }
    
    public static JsonObject fromClipboard(JsonObject schema) {
        try {
            String base64Data = mc.keyboard.getClipboard().trim();
            byte[] data = Base64.getDecoder().decode(base64Data);
            String jsonString = new String(data, StandardCharsets.UTF_8);
            
            JsonObject pasted = GSON.fromJson(jsonString, JsonObject.class);
            
            for (String key : schema.keySet()) {
                if (!pasted.has(key)) {
                    return null;
                }
            }
            
            if (!pasted.get("name").getAsString().equals(schema.get("name").getAsString())) {
                return null;
            }
            
            return pasted;
        } catch (Exception e) {
            MeteorClient.LOG.error("Invalid JSON data pasted!");
            
            OkPrompt.create()
                .title("Error pasting JSON data!")
                .message("Please check that the data you pasted is valid.")
                .id("json-pasting")
                .show();
            
            return null;
        }
    }
    
    public interface ToValue<T> {
        
        T toValue (JsonElement element);
        
    }
    
    public interface ToKey<T> {
        
        T toKey(String string);
        
    }
    
}
