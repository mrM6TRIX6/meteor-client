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
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EntityTypeListSetting extends Setting<Set<EntityType<?>>> {
    
    public final Predicate<EntityType<?>> filter;
    private List<String> suggestions;
    private final static List<String> groups = List.of("animal", "wateranimal", "monster", "ambient", "misc");
    
    public EntityTypeListSetting(String name, String title, String description, Set<EntityType<?>> defaultValue, Consumer<Set<EntityType<?>>> onChanged, Consumer<Setting<Set<EntityType<?>>>> onModuleActivated, IVisible visible, Predicate<EntityType<?>> filter) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        
        this.filter = filter;
    }
    
    @Override
    public void resetImpl() {
        value = new ObjectOpenHashSet<>(defaultValue);
    }
    
    @Override
    protected Set<EntityType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        Set<EntityType<?>> entities = new ObjectOpenHashSet<>(values.length);
        
        try {
            for (String value : values) {
                EntityType<?> entity = parseId(Registries.ENTITY_TYPE, value);
                if (entity != null) {
                    entities.add(entity);
                } else {
                    String lowerValue = value.trim().toLowerCase();
                    if (!groups.contains(lowerValue)) {
                        continue;
                    }
                    
                    for (EntityType<?> entityType : Registries.ENTITY_TYPE) {
                        if (filter != null && !filter.test(entityType)) {
                            continue;
                        }
                        
                        switch (lowerValue) {
                            case "animal" -> {
                                if (entityType.getSpawnGroup() == SpawnGroup.CREATURE) {
                                    entities.add(entityType);
                                }
                            }
                            case "wateranimal" -> {
                                if (entityType.getSpawnGroup() == SpawnGroup.WATER_AMBIENT
                                    || entityType.getSpawnGroup() == SpawnGroup.WATER_CREATURE
                                    || entityType.getSpawnGroup() == SpawnGroup.UNDERGROUND_WATER_CREATURE
                                    || entityType.getSpawnGroup() == SpawnGroup.AXOLOTLS) {
                                    entities.add(entityType);
                                }
                            }
                            case "monster" -> {
                                if (entityType.getSpawnGroup() == SpawnGroup.MONSTER) {
                                    entities.add(entityType);
                                }
                            }
                            case "ambient" -> {
                                if (entityType.getSpawnGroup() == SpawnGroup.AMBIENT) {
                                    entities.add(entityType);
                                }
                            }
                            case "misc" -> {
                                if (entityType.getSpawnGroup() == SpawnGroup.MISC) {
                                    entities.add(entityType);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        
        return entities;
    }
    
    @Override
    protected boolean isValueValid(Set<EntityType<?>> value) {
        return true;
    }
    
    @Override
    public List<String> getSuggestions() {
        if (suggestions == null) {
            suggestions = new ArrayList<>(groups);
            for (EntityType<?> entityType : Registries.ENTITY_TYPE) {
                if (filter == null || filter.test(entityType)) {
                    suggestions.add(Registries.ENTITY_TYPE.getId(entityType).toString());
                }
            }
        }
        
        return suggestions;
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        JsonArray valueArray = new JsonArray();
        for (EntityType<?> entityType : get()) {
            valueArray.add(Registries.ENTITY_TYPE.getId(entityType).toString());
        }
        jsonObject.add("value", valueArray);
        
        return jsonObject;
    }
    
    @Override
    public Set<EntityType<?>> load(JsonObject jsonObject) {
        get().clear();
        
        JsonArray valueArray = jsonObject.get("value").getAsJsonArray();
        for (JsonElement element : valueArray) {
            EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(element.getAsString()));
            if (filter == null || filter.test(type)) {
                get().add(type);
            }
        }
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, Set<EntityType<?>>, EntityTypeListSetting> {
        
        private Predicate<EntityType<?>> filter;
        
        public Builder() {
            super(new ObjectOpenHashSet<>(0));
        }
        
        public Builder defaultValue(EntityType<?>... defaults) {
            return defaultValue(defaults != null ? new ObjectOpenHashSet<>(defaults) : new ObjectOpenHashSet<>(0));
        }
        
        public Builder onlyAttackable() {
            filter = EntityUtils::isAttackable;
            return this;
        }
        
        public Builder filter(Predicate<EntityType<?>> filter) {
            this.filter = filter;
            return this;
        }
        
        @Override
        public EntityTypeListSetting build() {
            return new EntityTypeListSetting(name, title, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
        
    }
    
}
