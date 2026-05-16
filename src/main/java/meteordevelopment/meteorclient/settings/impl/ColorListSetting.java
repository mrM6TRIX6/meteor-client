/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ColorListSetting extends Setting<List<SettingColor>> {
    
    public ColorListSetting(String name, String description, List<SettingColor> defaultValue, Consumer<List<SettingColor>> onChanged, Consumer<Setting<List<SettingColor>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    protected List<SettingColor> parseImpl(String str) {
        List<SettingColor> colors = new ArrayList<>();
        try {
            String[] colorsStr = str.replaceAll("\\s+", "").split(";");
            for (String colorStr : colorsStr) {
                String[] strs = colorStr.split(",");
                colors.add(new SettingColor(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]), Integer.parseInt(strs[2]), Integer.parseInt(strs[3])));
            }
        } catch (IndexOutOfBoundsException | NumberFormatException ignored) {}
        return colors;
    }
    
    @Override
    protected boolean isValueValid(List<SettingColor> value) {
        return true;
    }
    
    @Override
    protected void resetImpl() {
        value = new ArrayList<>(defaultValue.size());
        
        for (SettingColor settingColor : defaultValue) {
            value.add(new SettingColor(settingColor));
        }
    }
    
    @Override
    protected JsonObject save(JsonObject jsonObject) {
        jsonObject.add("value", JsonUtils.listToJson(get()));
        
        return jsonObject;
    }
    
    @Override
    protected List<SettingColor> load(JsonObject jsonObject) {
        get().clear();
        
        for (JsonElement element : jsonObject.get("value").getAsJsonArray()) {
            get().add(new SettingColor().fromJson(element.getAsJsonObject()));
        }
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, List<SettingColor>, ColorListSetting> {
        
        public Builder() {
            super(new ArrayList<>());
        }
        
        @Override
        public ColorListSetting build() {
            return new ColorListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
