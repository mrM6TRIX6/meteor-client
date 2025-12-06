/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class StatusEffectAmplifierMapSetting extends Setting<Reference2IntMap<StatusEffect>> {
    
    public static final Reference2IntMap<StatusEffect> EMPTY_STATUS_EFFECT_MAP = createStatusEffectMap();
    
    public StatusEffectAmplifierMapSetting(String name, String description, Reference2IntMap<StatusEffect> defaultValue, Consumer<Reference2IntMap<StatusEffect>> onChanged, Consumer<Setting<Reference2IntMap<StatusEffect>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    public void resetImpl() {
        value = new Reference2IntOpenHashMap<>(defaultValue);
    }
    
    @Override
    protected Reference2IntMap<StatusEffect> parseImpl(String str) {
        String[] values = str.split(",");
        Reference2IntMap<StatusEffect> effects = new Reference2IntOpenHashMap<>(EMPTY_STATUS_EFFECT_MAP);
        
        try {
            for (String value : values) {
                String[] split = value.split(" ");
                
                StatusEffect effect = parseId(Registries.STATUS_EFFECT, split[0]);
                int level = Integer.parseInt(split[1]);
                
                effects.put(effect, level);
            }
        } catch (Exception ignored) {}
        
        return effects;
    }
    
    @Override
    protected boolean isValueValid(Reference2IntMap<StatusEffect> value) {
        return true;
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        JsonObject valueJson = new JsonObject();
        
        for (StatusEffect statusEffect : get().keySet()) {
            Identifier id = Registries.STATUS_EFFECT.getId(statusEffect);
            if (id != null) {
                valueJson.addProperty(id.toString(), get().getInt(statusEffect));
            }
        }
        
        jsonObject.add("value", valueJson);
        
        return jsonObject;
    }
    
    @Override
    public Reference2IntMap<StatusEffect> load(JsonObject jsonObject) {
        get().clear();
        
        JsonObject valueJson = jsonObject.get("value").getAsJsonObject();
        for (String key : valueJson.keySet()) {
            StatusEffect statusEffect = Registries.STATUS_EFFECT.get(Identifier.of(key));
            if (statusEffect != null) {
                get().put(statusEffect, valueJson.get(key).getAsInt());
            }
        }
        
        return get();
    }
    
    private static Reference2IntMap<StatusEffect> createStatusEffectMap() {
        Reference2IntMap<StatusEffect> map = new Reference2IntArrayMap<>(Registries.STATUS_EFFECT.getIds().size());
        
        Registries.STATUS_EFFECT.forEach(potion -> map.put(potion, 0));
        
        return map;
    }
    
    public static class Builder extends SettingBuilder<Builder, Reference2IntMap<StatusEffect>, StatusEffectAmplifierMapSetting> {
        
        public Builder() {
            super(new Reference2IntOpenHashMap<>(0));
        }
        
        @Override
        public StatusEffectAmplifierMapSetting build() {
            return new StatusEffectAmplifierMapSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
