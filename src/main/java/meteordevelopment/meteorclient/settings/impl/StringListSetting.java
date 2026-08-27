/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StringListSetting extends Setting<List<String>> {
    
    public final Class<? extends WTextBox.Renderer> renderer;
    public final CharFilter filter;
    public final boolean wide;
    
    public StringListSetting(String name, String description, List<String> defaultValue, Consumer<List<String>> onChanged, Consumer<Setting<List<String>>> onModuleActivated, IVisible visible, Class<? extends WTextBox.Renderer> renderer, CharFilter filter, boolean wide) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        
        this.renderer = renderer;
        this.filter = filter;
        this.wide = wide;
    }
    
    @Override
    protected List<String> parseImpl(String str) {
        return Utils.splitWithEscape(str);
    }
    
    @Override
    protected boolean isValueValid(List<String> value) {
        return true;
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        JsonArray valueArray = new JsonArray();
        
        for (int i = 0; i < this.value.size(); i++) {
            valueArray.add(get().get(i));
        }
        
        jsonObject.add("value", valueArray);
        
        return jsonObject;
    }
    
    @Override
    public List<String> load(JsonObject jsonObject) {
        get().clear();
        
        JsonArray valueArray = jsonObject.get("value").getAsJsonArray();
        for (JsonElement element : valueArray) {
            get().add(element.getAsString());
        }
        
        return get();
    }
    
    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }
    
    public static class Builder extends SettingBuilder<Builder, List<String>, StringListSetting> {
        
        private Class<? extends WTextBox.Renderer> renderer;
        private CharFilter filter;
        private boolean wide;
        
        public Builder() {
            super(new ArrayList<>(0));
        }
        
        public Builder defaultValue(String... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }
        
        public Builder renderer(Class<? extends WTextBox.Renderer> renderer) {
            this.renderer = renderer;
            return this;
        }
        
        public Builder filter(CharFilter filter) {
            this.filter = filter;
            return this;
        }
        
        public Builder wide() {
            wide = true;
            return this;
        }
        
        @Override
        public StringListSetting build() {
            return new StringListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, renderer, filter, wide);
        }
        
    }
    
}
