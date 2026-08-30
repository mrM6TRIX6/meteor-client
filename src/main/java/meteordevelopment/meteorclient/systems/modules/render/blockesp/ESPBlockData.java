/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.engine.ShapeMode;
import meteordevelopment.meteorclient.settings.IBlockData;
import meteordevelopment.meteorclient.settings.IGeneric;
import meteordevelopment.meteorclient.settings.impl.BlockDataSetting;
import meteordevelopment.meteorclient.settings.impl.GenericSetting;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.Block;

public class ESPBlockData implements IGeneric<ESPBlockData>, IChangeable, IBlockData<ESPBlockData> {
    
    public ShapeMode shapeMode;
    public Color lineColor;
    public Color sideColor;
    
    public boolean tracer;
    public Color tracerColor;
    
    private boolean changed;
    
    public ESPBlockData(ShapeMode shapeMode, Color lineColor, Color sideColor, boolean tracer, Color tracerColor) {
        this.shapeMode = shapeMode;
        this.lineColor = lineColor;
        this.sideColor = sideColor;
        
        this.tracer = tracer;
        this.tracerColor = tracerColor;
    }
    
    @Override
    public WidgetScreen createScreen(Block block, BlockDataSetting<ESPBlockData> setting) {
        return new ESPBlockDataScreen(this, block, setting);
    }
    
    @Override
    public WidgetScreen createScreen(GenericSetting<ESPBlockData> setting) {
        return new ESPBlockDataScreen(this, setting);
    }
    
    @Override
    public boolean isChanged() {
        return changed;
    }
    
    public void changed() {
        changed = true;
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
        return new ESPBlockData(shapeMode, new Color(lineColor), new Color(sideColor), tracer, new Color(tracerColor));
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
