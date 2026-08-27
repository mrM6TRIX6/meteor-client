/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.renderer.color.SettingColor;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.ColorSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;

/**
 * @author Walaryne
 */
public class Ambience extends Module {
    
    private final SettingGroup sgSky = settings.createGroup("Sky");
    private final SettingGroup sgWorld = settings.createGroup("World");
    
    // Sky
    
    public final Setting<Boolean> endSky = sgSky.add(new BoolSetting.Builder()
        .name("EndSky")
        .description("Makes the sky like the end.")
        .defaultValue(false)
        .build()
    );
    
    public final Setting<Boolean> customSkyColor = sgSky.add(new BoolSetting.Builder()
        .name("CustomSkyColor")
        .description("Whether the sky color should be changed.")
        .defaultValue(false)
        .build()
    );
    
    public final Setting<SettingColor> overworldSkyColor = sgSky.add(new ColorSetting.Builder()
        .name("OverworldSkyColor")
        .description("The color of the overworld sky.")
        .defaultValue(new SettingColor(0, 125, 255))
        .visible(customSkyColor::get)
        .build()
    );
    
    public final Setting<SettingColor> netherSkyColor = sgSky.add(new ColorSetting.Builder()
        .name("NetherSkyColor")
        .description("The color of the nether sky.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customSkyColor::get)
        .build()
    );
    
    public final Setting<SettingColor> endSkyColor = sgSky.add(new ColorSetting.Builder()
        .name("EndSkyColor")
        .description("The color of the end sky.")
        .defaultValue(new SettingColor(65, 30, 90))
        .visible(customSkyColor::get)
        .build()
    );
    
    public final Setting<Boolean> customCloudColor = sgSky.add(new BoolSetting.Builder()
        .name("CustomCloudColor")
        .description("Whether the clouds color should be changed.")
        .defaultValue(false)
        .build()
    );
    
    public final Setting<SettingColor> cloudColor = sgSky.add(new ColorSetting.Builder()
        .name("CloudColor")
        .description("The color of the clouds.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customCloudColor::get)
        .build()
    );
    
    public final Setting<Boolean> changeLightningColor = sgSky.add(new BoolSetting.Builder()
        .name("CustomLightningColor")
        .description("Whether the lightning color should be changed.")
        .defaultValue(false)
        .build()
    );
    
    public final Setting<SettingColor> lightningColor = sgSky.add(new ColorSetting.Builder()
        .name("LightningColor")
        .description("The color of the lightning.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(changeLightningColor::get)
        .build()
    );
    
    // World
    
    public final Setting<Boolean> customGrassColor = sgWorld.add(new BoolSetting.Builder()
        .name("CustomGrassColor")
        .description("Whether the grass color should be changed.")
        .defaultValue(false)
        .onChanged(val -> reload())
        .build()
    );
    
    public final Setting<SettingColor> grassColor = sgWorld.add(new ColorSetting.Builder()
        .name("GrassColor")
        .description("The color of the grass.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customGrassColor::get)
        .onChanged(val -> reload())
        .build()
    );
    
    public final Setting<Boolean> customFoliageColor = sgWorld.add(new BoolSetting.Builder()
        .name("CustomFoliageColor")
        .description("Whether the foliage color should be changed.")
        .defaultValue(false)
        .onChanged(val -> reload())
        .build()
    );
    
    public final Setting<SettingColor> foliageColor = sgWorld.add(new ColorSetting.Builder()
        .name("FoliageColor")
        .description("The color of the foliage.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customFoliageColor::get)
        .onChanged(val -> reload())
        .build()
    );
    
    public final Setting<Boolean> customWaterColor = sgWorld.add(new BoolSetting.Builder()
        .name("CustomWaterColor")
        .description("Whether the water color should be changed.")
        .defaultValue(false)
        .onChanged(val -> reload())
        .build()
    );
    
    public final Setting<SettingColor> waterColor = sgWorld.add(new ColorSetting.Builder()
        .name("WaterColor")
        .description("The color of the water.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customWaterColor::get)
        .onChanged(val -> reload())
        .build()
    );
    
    public final Setting<Boolean> customLavaColor = sgWorld.add(new BoolSetting.Builder()
        .name("CustomLavaColor")
        .description("Whether the lava color should be changed.")
        .defaultValue(false)
        .onChanged(val -> reload())
        .build()
    );
    
    public final Setting<SettingColor> lavaColor = sgWorld.add(new ColorSetting.Builder()
        .name("LavaColor")
        .description("The color of the lava.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customLavaColor::get)
        .onChanged(val -> reload())
        .build()
    );
    
    public final Setting<Boolean> customFogColor = sgWorld.add(new BoolSetting.Builder()
        .name("CustomFogColor")
        .description("Whether the fog color should be changed.")
        .defaultValue(false)
        .build()
    );
    
    public final Setting<SettingColor> fogColor = sgWorld.add(new ColorSetting.Builder()
        .name("FogColor")
        .description("The color of the fog.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customFogColor::get)
        .build()
    );
    
    public Ambience() {
        super(Category.RENDER, "Ambience", "Change the color of various pieces of the environment.");
    }
    
    @Override
    public void onActivate() {
        reload();
    }
    
    @Override
    public void onDeactivate() {
        reload();
    }
    
    private void reload() {
        if (mc.worldRenderer != null && isActive()) {
            mc.worldRenderer.reload();
        }
    }
    
    public SettingColor skyColor() {
        switch (PlayerUtils.getDimension()) {
            case OVERWORLD -> {
                return overworldSkyColor.get();
            }
            case NETHER -> {
                return netherSkyColor.get();
            }
            case END -> {
                return endSkyColor.get();
            }
        }
        return null;
    }
    
}
