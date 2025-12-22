/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProvidedStringSetting extends StringSetting {
    
    public final Supplier<String[]> supplier;
    
    public ProvidedStringSetting(String name, String title, String description, String defaultValue, Consumer<String> onChanged, Consumer<Setting<String>> onModuleActivated, IVisible visible, Class<? extends WTextBox.Renderer> renderer, boolean wide, Supplier<String[]> supplier) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible, "", renderer, null, wide);
        
        this.supplier = supplier;
    }
    
    public static class Builder extends Setting.SettingBuilder<Builder, String, ProvidedStringSetting> {
        
        private Class<? extends WTextBox.Renderer> renderer;
        private Supplier<String[]> supplier;
        private boolean wide;
        
        public Builder() {
            super(null);
        }
        
        public Builder renderer(Class<? extends WTextBox.Renderer> renderer) {
            this.renderer = renderer;
            return this;
        }
        
        public Builder supplier(Supplier<String[]> supplier) {
            this.supplier = supplier;
            return this;
        }
        
        public Builder wide() {
            wide = true;
            return this;
        }
        
        @Override
        public ProvidedStringSetting build() {
            return new ProvidedStringSetting(name, title, description, defaultValue, onChanged, onModuleActivated, visible, renderer, wide, supplier);
        }
        
    }
    
}
