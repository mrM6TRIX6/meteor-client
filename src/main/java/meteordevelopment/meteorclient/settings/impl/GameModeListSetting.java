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
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GameModeListSetting extends Setting<List<GameMode>> {
    
    public GameModeListSetting(String name, String description, List<GameMode> defaultValue, Consumer<List<GameMode>> onChanged, Consumer<Setting<List<GameMode>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    protected List<GameMode> parseImpl(String str) {
        String[] values = str.split(",");
        List<GameMode> modes = new ArrayList<>(values.length);
        for (String s : values) {
            GameMode mode = GameMode.byId(s);
            if (mode != null) {
                modes.add(mode);
            }
        }
        return modes;
    }
    
    @Override
    protected boolean isValueValid(List<GameMode> value) {
        return true;
    }
    
    @Override
    protected void resetImpl() {
        value = new ArrayList<>();
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        JsonArray valueArray = new JsonArray();
        
        for (GameMode mode : get()) {
            valueArray.add(mode.getId());
        }
        
        jsonObject.add("value", valueArray);
        
        return jsonObject;
    }
    
    @Override
    public List<GameMode> load(JsonObject jsonObject) {
        get().clear();
        
        JsonArray valueArray = jsonObject.get("value").getAsJsonArray();
        for (JsonElement element : valueArray) {
            GameMode mode = GameMode.byId(element.getAsString());
            if (mode != null) {
                get().add(mode);
            }
        }
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, List<GameMode>, GameModeListSetting> {
        
        public Builder() {
            super(new ArrayList<>(0));
        }
        
        public Builder defaultValue(List<GameMode> map) {
            this.defaultValue = map;
            return this;
        }
        
        @Override
        public GameModeListSetting build() {
            return new GameModeListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
