/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.Consumer;

public class BlockPosSetting extends Setting<BlockPos> {
    
    public BlockPosSetting(String name, String description, BlockPos defaultValue, Consumer<BlockPos> onChanged, Consumer<Setting<BlockPos>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    protected BlockPos parseImpl(String str) {
        List<String> values = List.of(str.split(","));
        if (values.size() != 3) {
            return null;
        }
        
        BlockPos bp = null;
        try {
            bp = new BlockPos(Integer.parseInt(values.get(0)), Integer.parseInt(values.get(1)), Integer.parseInt(values.get(2)));
        } catch (NumberFormatException ignored) {
        }
        return bp;
    }
    
    @Override
    protected boolean isValueValid(BlockPos value) {
        return true;
    }
    
    @Override
    protected JsonObject save(JsonObject jsonObject) {
        JsonArray valueArray = new JsonArray();
        valueArray.add(value.getX());
        valueArray.add(value.getY());
        valueArray.add(value.getZ());
        jsonObject.add("value", valueArray);
        
        return jsonObject;
    }
    
    @Override
    protected BlockPos load(JsonObject jsonObject) {
        if (jsonObject.has("value")) {
            JsonArray valueArray = jsonObject.get("value").getAsJsonArray();
            set(new BlockPos(valueArray.get(0).getAsInt(), valueArray.get(1).getAsInt(), valueArray.get(2).getAsInt()));
        }
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, BlockPos, BlockPosSetting> {
        
        public Builder() {
            super(new BlockPos(0, 0, 0));
        }
        
        @Override
        public BlockPosSetting build() {
            return new BlockPosSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
