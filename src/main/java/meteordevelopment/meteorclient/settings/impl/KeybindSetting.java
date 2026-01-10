/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class KeybindSetting extends Setting<Keybind> {
    
    private final Runnable action;
    public WKeybind widget;
    
    public KeybindSetting(String name, String title, String description, Keybind defaultValue, Consumer<Keybind> onChanged, Consumer<Setting<Keybind>> onModuleActivated, IVisible visible, Runnable action) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        
        this.action = action;
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onKeyBinding(KeyEvent event) {
        if (widget == null) {
            return;
        }
        if (event.action == KeyAction.PRESS && event.key() == GLFW.GLFW_KEY_ESCAPE && widget.onClear()) {
            event.cancel();
        } else if (event.action == KeyAction.RELEASE && widget.onAction(true, event.key(), event.modifiers())) {
            event.cancel();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMouseClickBinding(MouseClickEvent event) {
        if (event.action == KeyAction.PRESS && widget != null && widget.onAction(false, event.button(), 0)) {
            event.cancel();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.RELEASE && get().matches(event.input) && (module == null || module.isActive()) && action != null) {
            action.run();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseClick(MouseClickEvent event) {
        if (event.action == KeyAction.RELEASE && get().matches(event.input) && (module == null || module.isActive()) && action != null) {
            action.run();
        }
    }
    
    @Override
    public void resetImpl() {
        if (value == null) {
            value = defaultValue.copy();
        } else {
            value.set(defaultValue);
        }
        
        if (widget != null) {
            widget.reset();
        }
    }
    
    @Override
    protected Keybind parseImpl(String str) {
        try {
            return Keybind.fromKey(Integer.parseInt(str.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
    
    @Override
    protected boolean isValueValid(Keybind value) {
        return true;
    }
    
    @Override
    public JsonObject save(JsonObject jsonObject) {
        jsonObject.add("value", get().toJson());
        
        return jsonObject;
    }
    
    @Override
    public Keybind load(JsonObject jsonObject) {
        get().fromJson(jsonObject.get("value").getAsJsonObject());
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, Keybind, KeybindSetting> {
        
        private Runnable action;
        
        public Builder() {
            super(Keybind.none());
        }
        
        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }
        
        @Override
        public KeybindSetting build() {
            return new KeybindSetting(name, title, description, defaultValue, onChanged, onModuleActivated, visible, action);
        }
        
    }
    
}
