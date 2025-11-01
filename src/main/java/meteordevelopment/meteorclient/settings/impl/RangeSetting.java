/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Range;
import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;

public class RangeSetting extends Setting<Range> {
    
    public final int min, max;
    public final int sliderMin, sliderMax;
    public final boolean noSlider;
    
    private RangeSetting(String name, String description, Range defaultValue, Consumer<Range> onChanged, Consumer<Setting<Range>> onModuleActivated, IVisible visible, int min, int max, int sliderMin, int sliderMax, boolean noSlider) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        
        this.min = min;
        this.max = max;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
        this.noSlider = noSlider;
    }
    
    @Override
    protected Range parseImpl(String str) {
        try {
            return Range.parse(str);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    protected boolean isValueValid(Range value) {
        return value.min >= min && value.max <= max && value.min <= value.max;
    }
    
    @Override
    public NbtCompound save(NbtCompound tag) {
        tag.putInt("min", get().min);
        tag.putInt("max", get().max);
        return tag;
    }
    
    @Override
    public Range load(NbtCompound tag) {
        int min = tag.getInt("min", 0);
        int max = tag.getInt("max", 0);
        set(Range.of(min, max));
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, Range, RangeSetting> {
        
        private int min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
        private int sliderMin = 0, sliderMax = 10;
        private boolean noSlider = false;
        
        public Builder() {
            super(Range.of(0, 10));
        }
        
        public Builder min(int min) {
            this.min = min;
            return this;
        }
        
        public Builder max(int max) {
            this.max = max;
            return this;
        }
        
        public Builder range(int min, int max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
            return this;
        }
        
        public Builder sliderMin(int min) {
            this.sliderMin = min;
            return this;
        }
        
        public Builder sliderMax(int max) {
            this.sliderMax = max;
            return this;
        }
        
        public Builder sliderRange(int min, int max) {
            this.sliderMin = min;
            this.sliderMax = max;
            return this;
        }
        
        public Builder noSlider() {
            noSlider = true;
            return this;
        }
        
        @Override
        public RangeSetting build() {
            return new RangeSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, min, max, Math.max(sliderMin, min), Math.min(sliderMax, max), noSlider);
        }
        
    }
    
}
