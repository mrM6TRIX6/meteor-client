/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.lang.reflect.AccessFlag;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EnchantmentListSetting extends Setting<Set<RegistryKey<Enchantment>>> {
    
    public EnchantmentListSetting(String name, String title, String description, Set<RegistryKey<Enchantment>> defaultValue, Consumer<Set<RegistryKey<Enchantment>>> onChanged, Consumer<Setting<Set<RegistryKey<Enchantment>>>> onModuleActivated, IVisible visible) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    public void resetImpl() {
        value = new ObjectOpenHashSet<>(defaultValue);
    }
    
    @Override
    protected Set<RegistryKey<Enchantment>> parseImpl(String str) {
        String[] values = str.split(",");
        Set<RegistryKey<Enchantment>> enchs = new ObjectOpenHashSet<>(values.length);
        
        for (String value : values) {
            String name = value.trim();
            
            Identifier id;
            if (name.contains(":")) {
                id = Identifier.of(name);
            } else {
                id = Identifier.ofVanilla(name);
            }
            
            enchs.add(RegistryKey.of(RegistryKeys.ENCHANTMENT, id));
        }
        
        return enchs;
    }
    
    @Override
    protected boolean isValueValid(Set<RegistryKey<Enchantment>> value) {
        return true;
    }
    
    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Optional.ofNullable(mc.getNetworkHandler())
            .flatMap(networkHandler -> networkHandler.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT))
            .map(Registry::getIds).orElse(Set.of());
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        JsonArray valueArray = new JsonArray();
        
        for (RegistryKey<Enchantment> enchantment : get()) {
            valueArray.add(enchantment.getValue().toString());
        }
        
        jsonObject.add("value", valueArray);
        
        return jsonObject;
    }
    
    @Override
    public Set<RegistryKey<Enchantment>> load(JsonObject jsonObject) {
        get().clear();
        
        for (JsonElement element : jsonObject.get("value").getAsJsonArray()) {
            get().add(RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(element.getAsString())));
        }
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, Set<RegistryKey<Enchantment>>, EnchantmentListSetting> {
        
        private static final Set<RegistryKey<Enchantment>> VANILLA_DEFAULTS;
        
        public Builder() {
            super(new ObjectOpenHashSet<>());
        }
        
        public Builder vanillaDefaults() {
            return defaultValue(VANILLA_DEFAULTS);
        }
        
        @SafeVarargs
        public final Builder defaultValue(RegistryKey<Enchantment>... defaults) {
            return defaultValue(defaults != null ? new ObjectOpenHashSet<>(defaults) : new ObjectOpenHashSet<>());
        }
        
        @Override
        public EnchantmentListSetting build() {
            return new EnchantmentListSetting(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
        static {
            // noinspection unchecked, rawtypes
            VANILLA_DEFAULTS = (Set) Arrays.stream(Enchantments.class.getDeclaredFields())
                .filter(field -> field.accessFlags().containsAll(List.of(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.FINAL)))
                .filter(field -> field.getType() == RegistryKey.class)
                .map(field -> {
                    try {
                        return field.get(null);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .map(RegistryKey.class::cast)
                .filter(registryKey -> registryKey.getRegistryRef() == RegistryKeys.ENCHANTMENT)
                .collect(Collectors.toSet());
        }
    }
    
}
