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
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SoundEventListSetting extends Setting<List<SoundEvent>> {
    
    public SoundEventListSetting(String name, String description, List<SoundEvent> defaultValue, Consumer<List<SoundEvent>> onChanged, Consumer<Setting<List<SoundEvent>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }
    
    @Override
    protected List<SoundEvent> parseImpl(String str) {
        String[] values = str.split(",");
        List<SoundEvent> sounds = new ArrayList<>(values.length);
        
        try {
            for (String value : values) {
                SoundEvent sound = parseId(Registries.SOUND_EVENT, value);
                if (sound != null) {
                    sounds.add(sound);
                }
            }
        } catch (Exception ignored) {
        }
        
        return sounds;
    }
    
    @Override
    protected boolean isValueValid(List<SoundEvent> value) {
        return true;
    }
    
    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.SOUND_EVENT.getIds();
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        JsonArray valueArray = new JsonArray();
        
        for (SoundEvent sound : get()) {
            Identifier id = Registries.SOUND_EVENT.getId(sound);
            if (id != null) {
                valueArray.add(id.toString());
            }
        }
        
        jsonObject.add("value", valueArray);
        
        return jsonObject;
    }
    
    @Override
    public List<SoundEvent> load(JsonObject jsonObject) {
        get().clear();
        
        for (JsonElement element : jsonObject.get("value").getAsJsonArray()) {
            SoundEvent soundEvent = Registries.SOUND_EVENT.get(Identifier.of(element.getAsString()));
            if (soundEvent != null) {
                get().add(soundEvent);
            }
        }
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, List<SoundEvent>, SoundEventListSetting> {
        
        public Builder() {
            super(new ArrayList<>(0));
        }
        
        public Builder defaultValue(SoundEvent... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }
        
        @Override
        public SoundEventListSetting build() {
            return new SoundEventListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
