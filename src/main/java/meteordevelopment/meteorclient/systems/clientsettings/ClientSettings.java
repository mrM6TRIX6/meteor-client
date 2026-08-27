/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.clientsettings;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.renderer.color.SettingColor;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.impl.*;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;

import java.util.ArrayList;
import java.util.List;

public class ClientSettings extends System<ClientSettings> {
    
    public final Settings settings = new Settings();
    
    private final SettingGroup sgVisual = settings.createGroup("Visual");
    private final SettingGroup sgModules = settings.createGroup("Modules");
    private final SettingGroup sgChat = settings.createGroup("Chat");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    
    // Visual
    
    public final Setting<Double> rainbowSpeed = sgVisual.add(new DoubleSetting.Builder()
        .name("RainbowSpeed")
        .description("The global rainbow speed.")
        .defaultValue(0.5)
        .range(0, 10)
        .sliderMax(5)
        .build()
    );
    
    public final Setting<Boolean> titleScreenCredits = sgVisual.add(new BoolSetting.Builder()
        .name("TitleScreenCredits")
        .description("Show Meteor credits on title screen.")
        .defaultValue(true)
        .build()
    );
    
    public final Setting<Boolean> titleScreenSplashes = sgVisual.add(new BoolSetting.Builder()
        .name("TitleScreenSplashes")
        .description("Show Meteor splash texts on title screen.")
        .defaultValue(true)
        .build()
    );
    
    public final Setting<Boolean> customWindowTitle = sgVisual.add(new BoolSetting.Builder()
        .name("CustomWindowTitle")
        .description("Show custom text in the window title.")
        .defaultValue(false)
        .onModuleActivated(setting -> mc.updateWindowTitle())
        .onChanged(value -> mc.updateWindowTitle())
        .build()
    );
    
    public final Setting<String> customWindowTitleText = sgVisual.add(new StringSetting.Builder()
        .name("WindowTitleText")
        .description("The text it displays in the window title.")
        .visible(customWindowTitle::get)
        .defaultValue("Minecraft {mc_version} - {player}")
        .onChanged(value -> mc.updateWindowTitle())
        .build()
    );
    
    public final Setting<SettingColor> friendColor = sgVisual.add(new ColorSetting.Builder()
        .name("FriendColor")
        .description("The color used to show friends.")
        .defaultValue(new SettingColor(0, 255, 180))
        .build()
    );
    
    public final Setting<Boolean> syncListSettingWidths = sgVisual.add(new BoolSetting.Builder()
        .name("SyncListSettingWidths")
        .description("Prevents the list setting screens from moving around as you add & remove elements.")
        .defaultValue(false)
        .build()
    );
    
    // Modules
    
    public final Setting<Integer> moduleSearchCount = sgModules.add(new IntSetting.Builder()
        .name("ModuleSearchCount")
        .description("Amount of modules and settings to be shown in the module search bar.")
        .defaultValue(8)
        .min(1).sliderMax(12)
        .build()
    );
    
    // Chat
    
    public final Setting<Boolean> chatFeedback = sgChat.add(new BoolSetting.Builder()
        .name("ChatFeedback")
        .description("Sends chat feedback when meteor performs certain actions.")
        .defaultValue(true)
        .build()
    );
    
    public final Setting<Boolean> deleteChatFeedback = sgChat.add(new BoolSetting.Builder()
        .name("DeleteChatFeedback")
        .description("Delete previous matching chat feedback to keep chat clear.")
        .visible(chatFeedback::get)
        .defaultValue(true)
        .build()
    );
    
    // Misc
    
    public final Setting<Integer> rotationHoldTicks = sgMisc.add(new IntSetting.Builder()
        .name("RotationHold")
        .description("Hold long to hold server side rotation when not sending any packets.")
        .defaultValue(4)
        .build()
    );
    
    public final Setting<Boolean> useTeamColor = sgMisc.add(new BoolSetting.Builder()
        .name("UseTeamColor")
        .description("Uses player's team color for rendering things like esp and tracers.")
        .defaultValue(true)
        .build()
    );
    
    public List<String> dontShowAgainPrompts = new ArrayList<>();
    
    public ClientSettings() {
        super("client-settings");
    }
    
    public static ClientSettings get() {
        return Systems.get(ClientSettings.class);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.add("settings", settings.toJson());
        jsonObject.add("dontShowAgainPrompts", JsonUtils.listToJson(dontShowAgainPrompts));
        
        return jsonObject;
    }
    
    @Override
    public ClientSettings fromJson(JsonObject jsonObject) {
        if (jsonObject.has("settings")) {
            settings.fromJson(jsonObject.get("settings").getAsJsonObject());
        }
        
        if (jsonObject.has("dontShowAgainPrompts")) {
            dontShowAgainPrompts = JsonUtils.listFromJson(jsonObject, "dontShowAgainPrompts");
        }
        
        return this;
    }
    
}
