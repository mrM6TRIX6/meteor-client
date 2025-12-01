/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.macros;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.impl.KeybindSetting;
import meteordevelopment.meteorclient.settings.impl.StringListSetting;
import meteordevelopment.meteorclient.settings.impl.StringSetting;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import org.meteordev.starscript.Script;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Macro implements ISerializable<Macro> {
    
    public final Settings settings = new Settings();
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    public final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name of the macro.")
        .build()
    );
    
    public final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("messages")
        .description("The messages for the macro to send.")
        .onChanged(v -> dirty = true)
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );
    
    public final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("keybind")
        .description("The bind to run the macro.")
        .build()
    );
    
    private final List<Script> scripts = new ArrayList<>(1);
    private boolean dirty;
    
    public Macro() {}
    
    public Macro(JsonElement jsonElement) {
        fromJson((JsonObject) jsonElement);
    }
    
    public boolean onAction(boolean isKey, int value, int modifiers) {
        if (!keybind.get().matches(isKey, value, modifiers) || mc.currentScreen != null) {
            return false;
        }
        return onAction();
    }
    
    public boolean onAction() {
        if (dirty) {
            scripts.clear();
            
            for (String message : messages.get()) {
                Script script = MeteorStarscript.compile(message);
                if (script != null) {
                    scripts.add(script);
                }
            }
        }
        
        dirty = false;
        
        for (Script script : scripts) {
            String message = MeteorStarscript.run(script);
            
            if (message != null) {
                ChatUtils.sendPlayerMsg(message, false);
            }
        }
        
        return true;
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.add("settings", settings.toJson());
        
        return jsonObject;
    }
    
    @Override
    public Macro fromJson(JsonObject jsonObject) {
        if (jsonObject.has("settings")) {
            settings.fromJson(jsonObject.get("settings").getAsJsonObject());
        }
        
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Macro macro = (Macro) o;
        return Objects.equals(macro.name.get(), this.name.get());
    }
    
}
