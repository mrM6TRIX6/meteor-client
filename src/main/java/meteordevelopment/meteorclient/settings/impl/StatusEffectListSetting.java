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
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StatusEffectListSetting extends Setting<List<StatusEffect>> {
    
    public StatusEffectListSetting(String name, String title, String description, List<StatusEffect> defaultValue, Consumer<List<StatusEffect>> onChanged, Consumer<Setting<List<StatusEffect>>> onModuleActivated, IVisible visible) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }
    
    @Override
    protected List<StatusEffect> parseImpl(String str) {
        String[] values = str.split(",");
        List<StatusEffect> effects = new ArrayList<>(values.length);
        
        try {
            for (String value : values) {
                StatusEffect effect = parseId(Registries.STATUS_EFFECT, value);
                if (effect != null) {
                    effects.add(effect);
                }
            }
        } catch (Exception ignored) {}
        
        return effects;
    }
    
    @Override
    protected boolean isValueValid(List<StatusEffect> value) {
        return true;
    }
    
    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.STATUS_EFFECT.getIds();
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        JsonArray valueArray = new JsonArray();
        
        for (StatusEffect effect : get()) {
            Identifier id = Registries.STATUS_EFFECT.getId(effect);
            if (id != null) {
                valueArray.add(id.toString());
            }
        }
        
        jsonObject.add("value", valueArray);
        
        return jsonObject;
    }
    
    @Override
    public List<StatusEffect> load(JsonObject jsonObject) {
        get().clear();
        
        for (JsonElement element : jsonObject.get("value").getAsJsonArray()) {
            StatusEffect effect = Registries.STATUS_EFFECT.get(Identifier.of(element.getAsString()));
            if (effect != null) {
                get().add(effect);
            }
        }
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, List<StatusEffect>, StatusEffectListSetting> {
        
        public Builder() {
            super(new ArrayList<>(0));
        }
        
        public Builder defaultValue(StatusEffect... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }
        
        @Override
        public StatusEffectListSetting build() {
            return new StatusEffectListSetting(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
