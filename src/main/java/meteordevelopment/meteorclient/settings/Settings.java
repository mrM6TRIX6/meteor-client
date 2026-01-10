/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.settings.impl.ColorListSetting;
import meteordevelopment.meteorclient.settings.impl.ColorSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Settings implements ISerializable<Settings>, Iterable<SettingGroup> {
    
    private SettingGroup defaultGroup;
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
    
    public void registerColorSettings(Module module) {
        for (SettingGroup group : this) {
            for (Setting<?> setting : group) {
                setting.module = module;
                
                if (setting instanceof ColorSetting) {
                    RainbowColors.addSetting((Setting<SettingColor>) setting);
                } else if (setting instanceof ColorListSetting) {
                    RainbowColors.addSettingList((Setting<List<SettingColor>>) setting);
                }
            }
        }
    }
    
    public void unregisterColorSettings() {
        for (SettingGroup group : this) {
            for (Setting<?> setting : group) {
                if (setting instanceof ColorSetting) {
                    RainbowColors.removeSetting((Setting<SettingColor>) setting);
                } else if (setting instanceof ColorListSetting) {
                    RainbowColors.removeSettingList((Setting<List<SettingColor>>) setting);
                }
            }
        }
    }
    
    public void tick(WContainer settings, GuiTheme theme) {
        for (SettingGroup group : groups) {
            for (Setting<?> setting : group) {
                boolean visible = setting.isVisible();
                
                if (visible != setting.lastWasVisible) {
                    settings.clear();
                    settings.add(theme.settings(this)).expandX();
                }
                
                setting.lastWasVisible = visible;
            }
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
