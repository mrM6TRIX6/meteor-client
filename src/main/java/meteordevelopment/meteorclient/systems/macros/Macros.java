/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.macros;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Macros extends System<Macros> implements Iterable<Macro> {
    
    private List<Macro> macros = new ArrayList<>();
    
    public Macros() {
        super("macros");
    }
    
    public static Macros get() {
        return Systems.get(Macros.class);
    }
    
    public void add(Macro macro) {
        macros.add(macro);
        MeteorClient.EVENT_BUS.subscribe(macro);
        save();
    }
    
    public Macro get(String name) {
        for (Macro macro : macros) {
            if (macro.name.get().equalsIgnoreCase(name)) {
                return macro;
            }
        }
        
        return null;
    }
    
    public List<Macro> getAll() {
        return macros;
    }
    
    public int getCount() {
        return macros.size();
    }
    
    public void remove(Macro macro) {
        if (macros.remove(macro)) {
            MeteorClient.EVENT_BUS.unsubscribe(macro);
            save();
        }
    }
    
    public void clear() {
        macros.clear();
        save();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.RELEASE) {
            return;
        }
        
        for (Macro macro : macros) {
            if (macro.onAction(true, event.key(), event.modifiers())) {
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onButton(MouseClickEvent event) {
        if (event.action == KeyAction.RELEASE) {
            return;
        }
        
        for (Macro macro : macros) {
            if (macro.onAction(false, event.button(), 0)) {
                return;
            }
        }
    }
    
    public boolean isEmpty() {
        return macros.isEmpty();
    }
    
    @Override
    public @NotNull Iterator<Macro> iterator() {
        return macros.iterator();
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.add("macros", JsonUtils.listToJson(macros));
        
        return jsonObject;
    }
    
    @Override
    public Macros fromJson(JsonObject jsonObject) {
        for (Macro macro : macros) {
            MeteorClient.EVENT_BUS.unsubscribe(macro);
        }
        
        macros = JsonUtils.listFromJson(jsonObject.get("macros").getAsJsonArray(), Macro::new);
        
        for (Macro macro : macros) {
            MeteorClient.EVENT_BUS.subscribe(macro);
        }
        
        return this;
    }
    
}
