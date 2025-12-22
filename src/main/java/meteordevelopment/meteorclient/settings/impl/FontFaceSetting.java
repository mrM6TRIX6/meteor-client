/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;

import java.util.List;
import java.util.function.Consumer;

public class FontFaceSetting extends Setting<FontFace> {
    
    public FontFaceSetting(String name, String title, String description, FontFace defaultValue, Consumer<FontFace> onChanged, Consumer<Setting<FontFace>> onModuleActivated, IVisible visible) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    @Override
    protected FontFace parseImpl(String str) {
        String[] split = str.replace(" ", "").split("-");
        if (split.length != 2) {
            return null;
        }
        
        for (FontFamily family : Fonts.FONT_FAMILIES) {
            if (family.getName().replace(" ", "").equals(split[0])) {
                try {
                    return family.get(FontInfo.Type.valueOf(split[1]));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        
        return null;
    }
    
    @Override
    public List<String> getSuggestions() {
        return List.of("JetBrainsMono-Regular", "Arial-Bold");
    }
    
    @Override
    protected boolean isValueValid(FontFace value) {
        if (value == null) {
            return false;
        }
        
        for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
            if (fontFamily.hasType(value.info.type())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected JsonObject save(JsonObject jsonObject) {
        jsonObject.addProperty("family", get().info.family());
        jsonObject.addProperty("type", get().info.type().toString());
        return jsonObject;
    }
    
    @Override
    protected FontFace load(JsonObject jsonObject) {
        String family = jsonObject.get("family").getAsString();
        FontInfo.Type type;
        
        try {
            type = FontInfo.Type.valueOf(jsonObject.get("type").getAsString());
        } catch (IllegalArgumentException ignored) {
            set(Fonts.DEFAULT_FONT);
            return get();
        }
        
        boolean changed = false;
        for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
            if (fontFamily.getName().equals(family)) {
                set(fontFamily.get(type));
                changed = true;
            }
        }
        if (!changed) {
            set(Fonts.DEFAULT_FONT);
        }
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, FontFace, FontFaceSetting> {
        
        public Builder() {
            super(Fonts.DEFAULT_FONT);
        }
        
        @Override
        public FontFaceSetting build() {
            return new FontFaceSetting(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
