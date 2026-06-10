/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


/**
 * Choice setting, based on {@link IDisplayName} enum constants.
 *
 * @param <T> enum, which implements {@link IDisplayName}
 */
public class EnumChoiceSetting<T extends Enum<T> & IDisplayName> extends Setting<T> {
    
    private final List<T> choices;
    
    public EnumChoiceSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        
        choices = Arrays.asList(defaultValue.getDeclaringClass().getEnumConstants());
    }
    
    public List<T> getChoices() {
        return choices;
    }
    
    @Override
    protected T parseImpl(String str) {
        for (T choice : choices) {
            if (str.equalsIgnoreCase(choice.toString())) {
                return choice;
            }
        }
        
        return null;
    }
    
    @Override
    protected boolean isValueValid(T value) {
        /*
         * Since value is guaranteed to be an enum (T) constant used for this setting,
         * there's no need to double-check whether it is contained in the list.
         */
        return true;
    }
    
    @Override
    public List<String> getSuggestions() {
        return choices.stream()
            .map(Object::toString)
            .toList();
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
    
    public static class Builder<T extends Enum<T> & IDisplayName> extends SettingBuilder<Builder<T>, T, EnumChoiceSetting<T>> {
        
        public Builder() {
            super(null);
        }
        
        @Override
        public EnumChoiceSetting<T> build() {
            return new EnumChoiceSetting<>(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
