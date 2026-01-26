/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SettingGroup implements ISerializable<SettingGroup>, Iterable<Setting<?>> {
    
    private final Settings parent;
    private final String name;
    private boolean sectionExpanded;
    
    final List<Setting<?>> settings = new ArrayList<>(1);
    
    SettingGroup(Settings parent, String name, boolean sectionExpanded) {
        this.parent = parent;
        this.name = name;
        this.sectionExpanded = sectionExpanded;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isSectionExpanded() {
        return sectionExpanded;
    }
    
    public void setSectionExpanded(boolean sectionExpanded) {
        this.sectionExpanded = sectionExpanded;
    }
    
    public Setting<?> get(String name) {
        for (Setting<?> setting : this) {
            if (setting.name.equals(name)) {
                return setting;
            }
        }
        
        return null;
    }
    
    public <T extends Setting<?>> T add(T setting) {
        parent.forEach(group ->
            group.forEach(existing -> {
                if (existing.name.equalsIgnoreCase(setting.name)) {
                    throw new IllegalArgumentException("Setting with name '%s' already exists".formatted(setting.name));
                }
            })
        );
        
        settings.add(setting);
        
        return setting;
    }
    
    public Setting<?> getByIndex(int index) {
        return settings.get(index);
    }
    
    @Override
    public @NotNull Iterator<Setting<?>> iterator() {
        return settings.iterator();
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("sectionExpanded", sectionExpanded);
        
        JsonArray settingsArray = new JsonArray();
        for (Setting<?> setting : this) {
            if (setting.wasChanged()) {
                settingsArray.add(setting.toJson());
            }
        }
        jsonObject.add("settings", settingsArray);
        
        return jsonObject;
    }
    
    @Override
    public SettingGroup fromJson(JsonObject jsonObject) {
        sectionExpanded = jsonObject.get("sectionExpanded").getAsBoolean();
        
        JsonArray settingsArray = jsonObject.get("settings").getAsJsonArray();
        for (JsonElement element : settingsArray) {
            JsonObject settingJson = element.getAsJsonObject();
            
            Setting<?> setting = get(settingJson.get("name").getAsString());
            if (setting != null) {
                setting.fromJson(settingJson);
            }
        }
        
        return this;
    }
    
}
