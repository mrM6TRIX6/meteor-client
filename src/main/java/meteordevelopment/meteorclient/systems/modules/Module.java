/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.clientsettings.ClientSettings;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class Module implements ISerializable<Module>, Comparable<Module> {
    
    protected final MinecraftClient mc;
    
    public final Category category;
    public final String name;
    public final String description;
    public final String[] aliases;
    public final Color color;
    
    public final MeteorAddon addon;
    public final Settings settings = new Settings();
    
    private boolean active;
    
    public boolean serialize = true;
    public boolean runInMainMenu = false;
    public boolean autoSubscribe = true;
    
    public final Keybind keybind = Keybind.none();
    public boolean toggleOnBindRelease = false;
    public boolean chatFeedback = true;
    public boolean favorite = false;
    
    public Module(Category category, String name, String description, String... aliases) {
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Module '%s' contains invalid characters in name.".formatted(name));
        }
        
        this.mc = MinecraftClient.getInstance();
        this.category = category;
        this.name = name;
        this.description = description;
        this.aliases = aliases;
        this.color = Color.fromHsv(Utils.random(0.0, 360.0), 0.35, 1);
        
        String classname = this.getClass().getName();
        for (MeteorAddon addon : AddonManager.ADDONS) {
            if (classname.startsWith(addon.getPackage())) {
                this.addon = addon;
                return;
            }
        }
        
        this.addon = null;
    }
    
    public Module(Category category, String name, String description) {
        this(category, name, description, new String[0]);
    }
    
    public WWidget getWidget(GuiTheme theme) {
        return null;
    }
    
    public void onActivate() {}
    
    public void onDeactivate() {}
    
    public void toggle() {
        if (!active) {
            active = true;
            Modules.get().addActive(this);
            
            settings.onActivated();
            
            if (runInMainMenu || Utils.canUpdate()) {
                if (autoSubscribe) {
                    MeteorClient.EVENT_BUS.subscribe(this);
                }
                onActivate();
            }
        } else {
            if (runInMainMenu || Utils.canUpdate()) {
                if (autoSubscribe) {
                    MeteorClient.EVENT_BUS.unsubscribe(this);
                }
                onDeactivate();
            }
            
            active = false;
            Modules.get().removeActive(this);
        }
    }
    
    public void enable() {
        if (!isActive()) {
            toggle();
        }
    }
    
    public void disable() {
        if (isActive()) {
            toggle();
        }
    }
    
    public void sendToggledMsg() {
        if (ClientSettings.get().chatFeedback.get() && chatFeedback) {
            ChatUtils.forceNextPrefixClass(getClass());
            ChatUtils.sendMsg(this.hashCode(), Formatting.GRAY, "Toggled (highlight)%s(default) %s(default).", name, isActive() ? Formatting.GREEN + "on" : Formatting.RED + "off");
        }
    }
    
    public void info(Text message) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.sendMsg(name, message);
    }
    
    public void info(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.infoPrefix(name, message, args);
    }
    
    public void warning(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.warningPrefix(name, message, args);
    }
    
    public void error(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.errorPrefix(name, message, args);
    }
    
    public boolean isActive() {
        return active;
    }
    
    public String getInfoString() {
        return null;
    }
    
    @Override
    public JsonObject toJson() {
        if (!serialize) {
            return null;
        }
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("name", name);
        jsonObject.add("keybind", keybind.toJson());
        jsonObject.addProperty("toggleOnKeyRelease", toggleOnBindRelease);
        jsonObject.addProperty("chatFeedback", chatFeedback);
        jsonObject.addProperty("favorite", favorite);
        jsonObject.add("settings", settings.toJson());
        jsonObject.addProperty("active", active);
        
        return jsonObject;
    }
    
    @Override
    public Module fromJson(JsonObject jsonObject) {
        // General
        keybind.fromJson(jsonObject.get("keybind").getAsJsonObject());
        toggleOnBindRelease = jsonObject.get("toggleOnKeyRelease").getAsBoolean();
        chatFeedback = !jsonObject.has("chatFeedback") || jsonObject.get("chatFeedback").getAsBoolean();
        favorite = jsonObject.get("favorite").getAsBoolean();
        
        // Settings
        JsonElement settingsJson = jsonObject.get("settings");
        if (settingsJson instanceof JsonObject) {
            settings.fromJson((JsonObject) settingsJson);
        }
        
        boolean active = jsonObject.get("active").getAsBoolean();
        if (active != isActive()) {
            toggle();
        }
        
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Module module = (Module) o;
        if (name == null && module.name == null) return true;
        if (name == null || module.name == null) return false;
        
        return name.equalsIgnoreCase(module.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }
    
    @Override
    public int compareTo(@NotNull Module o) {
        return name.compareToIgnoreCase(o.name);
    }
    
}
