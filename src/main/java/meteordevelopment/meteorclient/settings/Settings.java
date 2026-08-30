/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Settings implements ISerializable<Settings>, Iterable<SettingGroup> {
    
    private SettingGroup defaultGroup;
    private boolean invalidate;
    
    public final List<SettingGroup> groups = new ArrayList<>(1);
    
    public void onActivated() {
        for (SettingGroup group : groups) {
            for (Setting<?> setting : group) {
                setting.onActivated();
            }
        }
    }
    
    public Setting<?> get(String name) {
        for (SettingGroup sg : this) {
            for (Setting<?> setting : sg) {
                if (name.equalsIgnoreCase(setting.name)) {
                    return setting;
                }
            }
        }
        
        return null;
    }
    
    public <T> Setting<T> get(String name, Class<T> clazz) {
        for (SettingGroup sg : this) {
            for (Setting<?> setting : sg) {
                Class<?> settingClass = setting.getDefaultValue().getClass();
                if (name.equalsIgnoreCase(setting.name) && clazz.equals(settingClass)) {
                    return (Setting<T>) setting;
                }
            }
        }
        
        return null;
    }
    
    public void reset() {
        for (SettingGroup group : groups) {
            for (Setting<?> setting : group) {
                setting.reset();
            }
        }
        
        invalidate();
    }
    
    public void invalidate() {
        invalidate = true;
    }
    
    public SettingGroup getGroup(String name) {
        for (SettingGroup sg : this) {
            if (sg.getName().equals(name)) {
                return sg;
            }
        }
        
        return null;
    }
    
    public int sizeGroups() {
        return groups.size();
    }
    
    public SettingGroup getDefaultGroup() {
        if (defaultGroup == null) {
            defaultGroup = createGroup("General");
        }
        return defaultGroup;
    }
    
    public SettingGroup createGroup(String name, boolean expanded) {
        groups.forEach(existing -> {
            if (existing.getName().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("Setting group with name '%s' already exists".formatted(name));
            }
        });
        
        SettingGroup group = new SettingGroup(this, name, expanded);
        groups.add(group);
        return group;
    }
    
    public SettingGroup createGroup(String name) {
        return createGroup(name, true);
    }
    
    public void tick(WContainer settings) {
        if (settings == null) {
            return;
        }
        
        for (SettingGroup group : groups) {
            for (Setting<?> setting : group) {
                boolean visible = setting.isVisible();
                
                if (visible != setting.lastWasVisible) {
                    invalidate();
                }
                
                setting.lastWasVisible = visible;
            }
        }
        
        if (invalidate) {
            settings.clear();
            settings.add(DefaultSettingsWidgetFactory.settings(this)).expandX();
            invalidate = false;
        }
    }
    
    @Override
    public @NotNull Iterator<SettingGroup> iterator() {
        return groups.iterator();
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.add("groups", JsonUtils.listToJson(groups));
        
        return jsonObject;
    }
    
    @Override
    public Settings fromJson(JsonObject jsonObject) {
        JsonArray groupsArray = jsonObject.get("groups").getAsJsonArray();
        
        for (JsonElement element : groupsArray) {
            JsonObject groupJson = element.getAsJsonObject();
            
            SettingGroup sg = getGroup(groupJson.get("name").getAsString());
            if (sg != null) {
                sg.fromJson(groupJson);
            }
        }
        
        return this;
    }
    
}
