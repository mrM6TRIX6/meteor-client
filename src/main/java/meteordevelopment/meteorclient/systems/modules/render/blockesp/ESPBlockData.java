/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.utils.IScreenFactory;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.IBlockData;
import meteordevelopment.meteorclient.settings.impl.BlockDataSetting;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.misc.ICopyable;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;

public class ESPBlockData implements ICopyable<ESPBlockData>, ISerializable<ESPBlockData>, IChangeable, IBlockData<ESPBlockData>, IScreenFactory {
    
    public ShapeMode shapeMode;
    public SettingColor lineColor;
    public SettingColor sideColor;
    
    public boolean tracer;
    public SettingColor tracerColor;
    
    private boolean changed;
    
    public ESPBlockData(ShapeMode shapeMode, SettingColor lineColor, SettingColor sideColor, boolean tracer, SettingColor tracerColor) {
        this.shapeMode = shapeMode;
        this.lineColor = lineColor;
        this.sideColor = sideColor;
        
        this.tracer = tracer;
        this.tracerColor = tracerColor;
    }
    
    @Override
    public WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<ESPBlockData> setting) {
        return new ESPBlockDataScreen(theme, this, block, setting);
    }
    
    @Override
    public WidgetScreen createScreen(GuiTheme theme) {
        return new ESPBlockDataScreen(theme, this, null, null);
    }
    
    @Override
    public boolean isChanged() {
        return changed;
    }
    
    public void changed() {
        changed = true;
    }
    
    public void tickRainbow() {
        lineColor.update();
        sideColor.update();
        tracerColor.update();
    }
    
    @Override
    public ESPBlockData set(ESPBlockData value) {
        shapeMode = value.shapeMode;
        lineColor.set(value.lineColor);
        sideColor.set(value.sideColor);
        
        tracer = value.tracer;
        tracerColor.set(value.tracerColor);
        
        changed = value.changed;
        
        return this;
    }
    
    @Override
    public ESPBlockData copy() {
        return new ESPBlockData(shapeMode, new SettingColor(lineColor), new SettingColor(sideColor), tracer, new SettingColor(tracerColor));
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("shapeMode", shapeMode.name());
        jsonObject.add("lineColor", lineColor.toJson());
        jsonObject.add("sideColor", sideColor.toJson());
        
        jsonObject.addProperty("tracer", tracer);
        jsonObject.add("tracerColor", tracerColor.toJson());
        
        jsonObject.addProperty("changed", changed);
        
        return jsonObject;
    }
    
    @Override
    public ESPBlockData fromJson(JsonObject jsonObject) {
        shapeMode = ShapeMode.valueOf(jsonObject.get("shapeMode").getAsString());
        lineColor.fromJson(jsonObject.get("lineColor").getAsJsonObject());
        sideColor.fromJson(jsonObject.get("sideColor").getAsJsonObject());
        
        tracer = jsonObject.get("tracer").getAsBoolean();
        tracerColor.fromJson(jsonObject.get("tracerColor").getAsJsonObject());
        
        changed = jsonObject.get("changed").getAsBoolean();
        
        return this;
    }
    
}
