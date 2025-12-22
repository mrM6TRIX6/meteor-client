/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.utils.IScreenFactory;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.ICopyable;
import meteordevelopment.meteorclient.utils.misc.ISerializable;

import java.util.function.Consumer;

public class GenericSetting<T extends ICopyable<T> & ISerializable<T> & IScreenFactory> extends Setting<T> {
    
    public GenericSetting(String name, String title, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    public void resetImpl() {
        if (value == null) {
            value = defaultValue.copy();
        }
        value.set(defaultValue);
    }
    
    @Override
    protected T parseImpl(String str) {
        return defaultValue.copy();
    }
    
    @Override
    protected boolean isValueValid(T value) {
        return true;
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        jsonObject.add("value", get().toJson());
        
        return jsonObject;
    }
    
    @Override
    public T load(JsonObject jsonObject) {
        get().fromJson(jsonObject.get("value").getAsJsonObject());
        
        return get();
    }
    
    public static class Builder<T extends ICopyable<T> & ISerializable<T> & IScreenFactory> extends SettingBuilder<Builder<T>, T, GenericSetting<T>> {
        
        public Builder() {
            super(null);
        }
        
        @Override
        public GenericSetting<T> build() {
            return new GenericSetting<>(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
