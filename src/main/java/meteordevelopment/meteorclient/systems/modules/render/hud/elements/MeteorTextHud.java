/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.elements;

import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElementInfo;

public class MeteorTextHud {
    
    public static final HUDElementInfo<TextHUD> INFO = new HUDElementInfo<>(HUD.GROUP, "Text", "Displays arbitrary text with Starscript.", MeteorTextHud::create);
    
    public static final HUDElementInfo<TextHUD>.Preset FPS;
    public static final HUDElementInfo<TextHUD>.Preset TPS;
    public static final HUDElementInfo<TextHUD>.Preset PING;
    public static final HUDElementInfo<TextHUD>.Preset SPEED;
    public static final HUDElementInfo<TextHUD>.Preset GAME_MODE;
    public static final HUDElementInfo<TextHUD>.Preset DURABILITY;
    public static final HUDElementInfo<TextHUD>.Preset POSITION;
    public static final HUDElementInfo<TextHUD>.Preset OPPOSITE_POSITION;
    public static final HUDElementInfo<TextHUD>.Preset LOOKING_AT;
    public static final HUDElementInfo<TextHUD>.Preset LOOKING_AT_WITH_POSITION;
    public static final HUDElementInfo<TextHUD>.Preset BREAKING_PROGRESS;
    public static final HUDElementInfo<TextHUD>.Preset SERVER;
    public static final HUDElementInfo<TextHUD>.Preset BIOME;
    public static final HUDElementInfo<TextHUD>.Preset WORLD_TIME;
    public static final HUDElementInfo<TextHUD>.Preset REAL_TIME;
    public static final HUDElementInfo<TextHUD>.Preset ROTATION;
    public static final HUDElementInfo<TextHUD>.Preset MODULE_ENABLED;
    public static final HUDElementInfo<TextHUD>.Preset MODULE_ENABLED_WITH_INFO;
    public static final HUDElementInfo<TextHUD>.Preset WATERMARK;
    public static final HUDElementInfo<TextHUD>.Preset BARITONE;
    
    static {
        addPreset("Empty", null);
        FPS = addPreset("FPS", "FPS: #1{fps}", 0);
        TPS = addPreset("TPS", "TPS: #1{round(server.tps, 1)}");
        PING = addPreset("Ping", "Ping: #1{ping}");
        SPEED = addPreset("Speed", "Speed: #1{round(player.speed, 1)}", 0);
        GAME_MODE = addPreset("GameMode", "Game mode: #1{player.gamemode}", 0);
        DURABILITY = addPreset("Durability", "Durability: #1{player.hand_or_offhand.durability}");
        POSITION = addPreset("Position", "Pos: #1{floor(camera.pos.x)}, {floor(camera.pos.y)}, {floor(camera.pos.z)}", 0);
        OPPOSITE_POSITION = addPreset("Opposite Position", "{player.opposite_dimension != \"End\" ? player.opposite_dimension + \":\" : \"\"} #1{player.opposite_dimension != \"End\" ? \"\" + floor(camera.opposite_dim_pos.x) + \", \" + floor(camera.opposite_dim_pos.y) + \", \" + floor(camera.opposite_dim_pos.z) : \"\"}", 0);
        LOOKING_AT = addPreset("LookingAt", "Looking at: #1{crosshair_target.value}", 0);
        LOOKING_AT_WITH_POSITION = addPreset("Looking at with position", "Looking at: #1{crosshair_target.value} {crosshair_target.type != \"miss\" ? \"(\" + \"\" + floor(crosshair_target.value.pos.x) + \", \" + floor(crosshair_target.value.pos.y) + \", \" + floor(crosshair_target.value.pos.z) + \")\" : \"\"}", 0);
        BREAKING_PROGRESS = addPreset("BreakingProgress", "Breaking progress: #1{round(player.breaking_progress * 100)}%", 0);
        SERVER = addPreset("Server", "Server: #1{server}");
        BIOME = addPreset("Biome", "Biome: #1{player.biome}", 0);
        WORLD_TIME = addPreset("WorldTime", "Time: #1{server.time}");
        REAL_TIME = addPreset("RealTime", "Time: #1{time}");
        ROTATION = addPreset("Rotation", "{camera.direction} #1({round(camera.yaw, 1)}, {round(camera.pitch, 1)})", 0);
        MODULE_ENABLED = addPreset("ModuleEnabled", "Kill Aura: {meteor.is_module_active(\"kill-aura\") ? #2 \"ON\" : #3 \"OFF\"}", 0);
        MODULE_ENABLED_WITH_INFO = addPreset("Module enabled with info", "Kill Aura: {meteor.is_module_active(\"kill-aura\") ? #2 \"ON\" : #3 \"OFF\"} #1{meteor.get_module_info(\"kill-aura\")}", 0);
        WATERMARK = addPreset("Watermark", "{meteor.name} #1{meteor.version}");
        BARITONE = addPreset("Baritone", "Baritone: #1{baritone.process_name}");
    }
    
    private static TextHUD create() {
        return new TextHUD(INFO);
    }
    
    private static HUDElementInfo<TextHUD>.Preset addPreset(String name, String text, int updateDelay) {
        return INFO.addPreset(name, textHud -> {
            if (text != null) {
                textHud.setText(text);
            }
            if (updateDelay != -1) {
                textHud.setUpdateDelay(updateDelay);
            }
        });
    }
    
    private static HUDElementInfo<TextHUD>.Preset addPreset(String name, String text) {
        return addPreset(name, text, -1);
    }
    
}
