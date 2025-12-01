/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IBlockData;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.misc.ICopyable;
import meteordevelopment.meteorclient.utils.misc.IGetter;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class BlockDataSetting<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> extends Setting<Map<Block, T>> {
    
    public final IGetter<T> defaultData;
    
    public BlockDataSetting(String name, String description, Map<Block, T> defaultValue, Consumer<Map<Block, T>> onChanged, Consumer<Setting<Map<Block, T>>> onModuleActivated, IGetter<T> defaultData, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        
        this.defaultData = defaultData;
    }
    
    @Override
    public void resetImpl() {
        value = new HashMap<>(defaultValue);
    }
    
    @Override
    protected Map<Block, T> parseImpl(String str) {
        return new HashMap<>(0);
    }
    
    @Override
    protected boolean isValueValid(Map<Block, T> value) {
        return true;
    }
    
    @Override
    protected JsonObject save(JsonObject jsonObject) {
        JsonObject valueJson = new JsonObject();
        
        for (Block block : get().keySet()) {
            valueJson.add(Registries.BLOCK.getId(block).toString(), get().get(block).toJson());
        }
        jsonObject.add("value", valueJson);
        
        return jsonObject;
    }
    
    @Override
    protected Map<Block, T> load(JsonObject jsonObject) {
        get().clear();
        
        JsonObject valueJson = jsonObject.get("value").getAsJsonObject();
        for (String key : valueJson.keySet()) {
            get().put(Registries.BLOCK.get(Identifier.of(key)), defaultData.get().copy().fromJson(valueJson.get(key).getAsJsonObject()));
        }
        
        return get();
    }
    
    public static class Builder<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> extends SettingBuilder<Builder<T>, Map<Block, T>, BlockDataSetting<T>> {
        
        private IGetter<T> defaultData;
        
        public Builder() {
            super(new HashMap<>(0));
        }
        
        public Builder<T> defaultData(IGetter<T> defaultData) {
            this.defaultData = defaultData;
            return this;
        }
        
        @Override
        public BlockDataSetting<T> build() {
            return new BlockDataSetting<>(name, description, defaultValue, onChanged, onModuleActivated, defaultData, visible);
        }
        
    }
    
}
