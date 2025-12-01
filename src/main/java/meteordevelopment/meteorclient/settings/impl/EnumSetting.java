/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EnumSetting<T extends Enum<?>> extends Setting<T> {
    
    private final T[] values;
    private final List<String> suggestions;
    
    public EnumSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        
        values = (T[]) defaultValue.getDeclaringClass().getEnumConstants();
        suggestions = new ArrayList<>(values.length);
        for (T value : values) {
            suggestions.add(value.toString());
        }
    }
    
    @Override
    protected T parseImpl(String str) {
        for (T possibleValue : values) {
            if (str.equalsIgnoreCase(possibleValue.toString())) {
                return possibleValue;
            }
        }
        
        return null;
    }
    
    @Override
    protected boolean isValueValid(T value) {
        return true;
    }
    
    @Override
    public List<String> getSuggestions() {
        return suggestions;
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        jsonObject.addProperty("value", get().toString());
        
        return jsonObject;
    }
    
    @Override
    public T load(JsonObject jsonObject) {
        parse(jsonObject.get("value").getAsString());
        
        return get();
    }
    
    public static class Builder<T extends Enum<?>> extends SettingBuilder<Builder<T>, T, EnumSetting<T>> {
        
        public Builder() {
            super(null);
        }
        
        @Override
        public EnumSetting<T> build() {
            return new EnumSetting<>(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
