/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.MyPotion;

import java.util.function.Consumer;

public class PotionChoiceSetting extends EnumChoiceSetting<MyPotion> {
    
    public PotionChoiceSetting(String name, String title, String description, MyPotion defaultValue, Consumer<MyPotion> onChanged, Consumer<Setting<MyPotion>> onModuleActivated, IVisible visible) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    public static class Builder extends EnumChoiceSetting.Builder<MyPotion> {
        
        @Override
        public EnumChoiceSetting<MyPotion> build() {
            return new PotionChoiceSetting(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
}
