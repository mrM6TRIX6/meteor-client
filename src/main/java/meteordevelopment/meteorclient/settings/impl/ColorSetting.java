/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.List;
import java.util.function.Consumer;

public class ColorSetting extends Setting<Color> {
    
    private static final List<String> SUGGESTIONS = List.of("0 0 0 255", "225 25 25 255", "25 225 25 255", "25 25 225 255", "255 255 255 255");
    
    public ColorSetting(String name, String description, Color defaultValue, Consumer<Color> onChanged, Consumer<Setting<Color>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    protected Color parseImpl(String str) {
        try {
            String[] strs = str.split(" ");
            return new Color(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]), Integer.parseInt(strs[2]), Integer.parseInt(strs[3]));
        } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
            return null;
        }
    }
    
    @Override
    public void resetImpl() {
        if (value == null) {
            value = new Color(defaultValue);
        } else {
            value.set(defaultValue);
        }
    }
    
    @Override
    protected boolean isValueValid(Color value) {
        value.validate();
        
        return true;
    }
    
    @Override
    public List<String> getSuggestions() {
        return SUGGESTIONS;
    }
    
    @Override
    protected JsonObject save(JsonObject jsonObject) {
        jsonObject.add("value", get().toJson());
        
        return jsonObject;
    }
    
    @Override
    public Color load(JsonObject jsonObject) {
        get().fromJson(jsonObject.get("value").getAsJsonObject());
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, Color, ColorSetting> {
        
        public Builder() {
            super(new Color());
        }
        
        @Override
        public Builder defaultValue(Color defaultValue) {
            this.defaultValue.set(defaultValue);
            return this;
        }
        
        @Override
        public ColorSetting build() {
            return new ColorSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
