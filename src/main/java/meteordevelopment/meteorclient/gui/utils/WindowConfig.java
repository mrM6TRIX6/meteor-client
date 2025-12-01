/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.utils;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.utils.misc.ISerializable;

import java.util.Optional;

public class WindowConfig implements ISerializable<WindowConfig> {
    
    public boolean expanded = true;
    public double x = -1;
    public double y = -1;
    
    // Saving
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("expanded", expanded);
        jsonObject.addProperty("x", x);
        jsonObject.addProperty("y", y);
        
        return jsonObject;
    }
    
    @Override
    public WindowConfig fromJson(JsonObject jsonObject) {
        Optional.of(jsonObject.get("expanded").getAsBoolean()).ifPresent(bool -> expanded = bool);
        Optional.of(jsonObject.get("x").getAsDouble()).ifPresent(x1 -> x = x1);
        Optional.of(jsonObject.get("y").getAsDouble()).ifPresent(y1 -> y = y1);
        
        return this;
    }
    
}
