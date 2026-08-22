/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StringListSetting extends Setting<List<String>> {
    
    public final Class<? extends WTextBox.Renderer> renderer;
    public final CharFilter filter;
    
    public StringListSetting(String name, String description, List<String> defaultValue, Consumer<List<String>> onChanged, Consumer<Setting<List<String>>> onModuleActivated, IVisible visible, Class<? extends WTextBox.Renderer> renderer, CharFilter filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        
        this.renderer = renderer;
        this.filter = filter;
    }
    
    @Override
    protected List<String> parseImpl(String str) {
        return Arrays.asList(str.split(","));
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
    
    public static void fillTable(WTable table, StringListSetting setting) {
        table.clear();
        
        ArrayList<String> strings = new ArrayList<>(setting.get());
        CharFilter filter = setting.filter == null ? (text, c) -> true : setting.filter;
        
        for (int i = 0; i < setting.get().size(); i++) {
            int msgI = i;
            String message = setting.get().get(i);
            
            WTextBox textBox = table.add(new WTextBox(message, filter, setting.renderer)).expandX().widget();
            textBox.action = () -> strings.set(msgI, textBox.get());
            textBox.actionOnUnfocused = () -> setting.set(strings);
            
            WMinus delete = table.add(new WMinus()).widget();
            delete.action = () -> {
                strings.remove(msgI);
                setting.set(strings);
                
                fillTable(table, setting);
            };
            
            table.row();
        }
        
        if (!setting.get().isEmpty()) {
            table.add(new WHorizontalSeparator()).expandX();
            table.row();
        }
        
        WButton add = table.add(new WButton("Add")).expandX().widget();
        add.action = () -> {
            strings.add("");
            setting.set(strings);
            
            fillTable(table, setting);
        };
        
        WButton reset = table.add(new WButton(GuiConstants.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            
            fillTable(table, setting);
        };
        reset.tooltip = "Reset";
    }
    
    public static class Builder extends SettingBuilder<Builder, List<String>, StringListSetting> {
        
        private Class<? extends WTextBox.Renderer> renderer;
        private CharFilter filter;
        
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
        
        @Override
        public StringListSetting build() {
            return new StringListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, renderer, filter);
        }
        
    }
    
}
