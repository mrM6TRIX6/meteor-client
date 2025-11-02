/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.config.types;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.config.IVisible;
import meteordevelopment.meteorclient.config.Setting;
import meteordevelopment.meteorclient.utils.world.RegistryUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.lang.reflect.AccessFlag;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EnchantmentListSetting extends Setting<Set<RegistryKey<Enchantment>>> {
    
    public EnchantmentListSetting(String name, String description, Set<RegistryKey<Enchantment>> defaultValue, Consumer<Set<RegistryKey<Enchantment>>> onChanged, Consumer<Setting<Set<RegistryKey<Enchantment>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
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
        return RegistryUtils.REGISTRY_ACCESS.getOptional(RegistryKeys.ENCHANTMENT)
            .map(registryEntryLookup -> registryEntryLookup.streamEntries()
                .map(entry -> entry.registryKey().getValue())
                .collect(Collectors.toSet())
            )
            .orElse(Set.of());
    }
    
    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (RegistryKey<Enchantment> ench : get()) {
            valueTag.add(NbtString.of(ench.getValue().toString()));
        }
        tag.put("value", valueTag);
        
        return tag;
    }
    
    @Override
    public Set<RegistryKey<Enchantment>> load(NbtCompound tag) {
        get().clear();
        
        for (NbtElement tagI : tag.getListOrEmpty("value")) {
            get().add(RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(tagI.asString().orElse(""))));
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
            return new EnchantmentListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
        static {
            //noinspection unchecked,rawtypes
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
