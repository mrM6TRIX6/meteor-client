/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.IGetter;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class Setting<T> implements IGetter<T>, ISerializable<T> {
    
    private static final List<String> NO_SUGGESTIONS = new ArrayList<>(0);
    
    public final String name, title, description;
    private final IVisible visible;
    
    protected final T defaultValue;
    protected T value;
    
    public final Consumer<Setting<T>> onModuleActivated;
    private final Consumer<T> onChanged;
    
    public Module module;
    public boolean lastWasVisible;
    
    public Setting(String name, String title, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Setting '%s' contains invalid characters in name.".formatted(name));
        }
        
        this.name = name;
        this.title = title != null ? title : Utils.nameToTitle(name);
        this.description = description;
        this.defaultValue = defaultValue;
        this.onChanged = onChanged;
        this.onModuleActivated = onModuleActivated;
        this.visible = visible;
        
        resetImpl();
    }
    
    @Override
    public T get() {
        return value;
    }
    
    public boolean set(T value) {
        if (!isValueValid(value)) {
            return false;
        }
        this.value = value;
        onChanged();
        return true;
    }
    
    protected void resetImpl() {
        value = defaultValue;
    }
    
    public void reset() {
        resetImpl();
        onChanged();
    }
    
    public T getDefaultValue() {
        return defaultValue;
    }
    
    public boolean parse(String str) {
        T newValue = parseImpl(str);
        
        if (newValue != null) {
            if (isValueValid(newValue)) {
                value = newValue;
                onChanged();
            }
        }
        
        return newValue != null;
    }
    
    public boolean wasChanged() {
        return !Objects.equals(value, defaultValue);
    }
    
    public void onChanged() {
        if (onChanged != null) {
            onChanged.accept(value);
        }
    }
    
    public void onActivated() {
        if (onModuleActivated != null) {
            onModuleActivated.accept(this);
        }
    }
    
    public boolean isVisible() {
        return visible == null || visible.isVisible();
    }
    
    protected abstract T parseImpl(String str);
    
    protected abstract boolean isValueValid(T value);
    
    public Iterable<Identifier> getIdentifierSuggestions() {
        return null;
    }
    
    public List<String> getSuggestions() {
        return NO_SUGGESTIONS;
    }
    
    protected abstract JsonObject save(JsonObject jsonObject);
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("name", name);
        
        save(jsonObject);
        return jsonObject;
    }
    
    protected abstract T load(JsonObject jsonObject);
    
    @Override
    public T fromJson(JsonObject jsonObject) {
        T value = load(jsonObject);
        onChanged();
        
        return value;
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Setting<?> setting = (Setting<?>) o;
        return Objects.equals(name, setting.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Nullable
    public static <T> T parseId(Registry<T> registry, String name) {
        name = name.trim();
        
        Identifier id;
        if (name.contains(":")) {
            id = Identifier.of(name);
        } else {
            id = Identifier.of("minecraft", name);
        }
        if (registry.containsId(id)) {
            return registry.get(id);
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public abstract static class SettingBuilder<B, V, S> {
        
        protected String name = "undefined-" + String.valueOf(hashCode()).substring(0, 5), title, description = "";
        protected V defaultValue;
        protected IVisible visible;
        protected Consumer<V> onChanged;
        protected Consumer<Setting<V>> onModuleActivated;
        
        protected SettingBuilder(V defaultValue) {
            this.defaultValue = defaultValue;
        }
        
        public B name(String name) {
            this.name = name;
            return (B) this;
        }
        
        public B title(String title) {
            this.title = title;
            return (B) this;
        }
        
        public B description(String description) {
            this.description = description;
            return (B) this;
        }
        
        public B defaultValue(V defaultValue) {
            this.defaultValue = defaultValue;
            return (B) this;
        }
        
        public B visible(IVisible visible) {
            this.visible = visible;
            return (B) this;
        }
        
        public B onChanged(Consumer<V> onChanged) {
            this.onChanged = onChanged;
            return (B) this;
        }
        
        public B onModuleActivated(Consumer<Setting<V>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return (B) this;
        }
        
        public abstract S build();
        
    }
    
}
