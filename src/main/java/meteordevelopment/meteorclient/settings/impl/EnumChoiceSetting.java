/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.ITagged;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class EnumChoiceSetting<T extends Enum<T> & ITagged> extends Setting<T> {
    
    private final T[] values;
    private final List<String> suggestions;
    
    public EnumChoiceSetting(String name, String title, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        
        values = defaultValue.getDeclaringClass().getEnumConstants();
        suggestions = Arrays.stream(values).map(Objects::toString).toList();
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
    
    public static class Builder<T extends Enum<T> & ITagged> extends SettingBuilder<Builder<T>, T, EnumChoiceSetting<T>> {
        
        public Builder() {
            super(null);
        }
        
        @Override
        public EnumChoiceSetting<T> build() {
            return new EnumChoiceSetting<>(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
