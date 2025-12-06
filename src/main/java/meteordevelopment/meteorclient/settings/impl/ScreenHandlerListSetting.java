/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ScreenHandlerListSetting extends Setting<List<ScreenHandlerType<?>>> {
    
    public ScreenHandlerListSetting(String name, String description, List<ScreenHandlerType<?>> defaultValue, Consumer<List<ScreenHandlerType<?>>> onChanged, Consumer<Setting<List<ScreenHandlerType<?>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }
    
    @Override
    protected List<ScreenHandlerType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        List<ScreenHandlerType<?>> handlers = new ArrayList<>(values.length);
        
        try {
            for (String value : values) {
                ScreenHandlerType<?> handler = parseId(Registries.SCREEN_HANDLER, value);
                if (handler != null) {
                    handlers.add(handler);
                }
            }
        } catch (Exception ignored) {}
        
        return handlers;
    }
    
    @Override
    protected boolean isValueValid(List<ScreenHandlerType<?>> value) {
        return true;
    }
    
    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.SCREEN_HANDLER.getIds();
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        JsonArray valueArray = new JsonArray();
        
        for (ScreenHandlerType<?> type : get()) {
            Identifier id = Registries.SCREEN_HANDLER.getId(type);
            if (id != null) {
                valueArray.add(id.toString());
            }
        }
        
        jsonObject.add("value", valueArray);
        
        return jsonObject;
    }
    
    @Override
    public List<ScreenHandlerType<?>> load(JsonObject jsonObject) {
        get().clear();
        
        JsonArray valueArray = jsonObject.get("value").getAsJsonArray();
        for (JsonElement element : valueArray) {
            ScreenHandlerType<?> type = Registries.SCREEN_HANDLER.get(Identifier.of(element.getAsString()));
            if (type != null) {
                get().add(type);
            }
        }
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, List<ScreenHandlerType<?>>, ScreenHandlerListSetting> {
        
        public Builder() {
            super(new ArrayList<>(0));
        }
        
        public Builder defaultValue(ScreenHandlerType<?>... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }
        
        @Override
        public ScreenHandlerListSetting build() {
            return new ScreenHandlerListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
