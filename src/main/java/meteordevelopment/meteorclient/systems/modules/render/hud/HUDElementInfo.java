/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HUDElementInfo<T extends HUDElement> {
    
    public final HUDGroup group;
    public final String name;
    public final String description;
    
    public final Supplier<T> factory;
    public final List<Preset> presets;
    
    public HUDElementInfo(HUDGroup group, String name, String description, Supplier<T> factory) {
        this.group = group;
        this.name = name.replace(" ", "");
        this.description = description;
        
        this.factory = factory;
        this.presets = new ArrayList<>();
    }
    
    public Preset addPreset(String title, Consumer<T> callback) {
        Preset preset = new Preset(this, title, callback);
        
        presets.add(preset);
        presets.sort(Comparator.comparing(p -> p.name));
        
        return preset;
    }
    
    public boolean hasPresets() {
        return !presets.isEmpty();
    }
    
    public HUDElement create() {
        return factory.get();
    }
    
    public class Preset {
        
        public final HUDElementInfo<?> info;
        public final String name;
        public final Consumer<T> callback;
        
        public Preset(HUDElementInfo<?> info, String name, Consumer<T> callback) {
            this.info = info;
            this.name = name;
            this.callback = callback;
        }
        
    }
    
}
