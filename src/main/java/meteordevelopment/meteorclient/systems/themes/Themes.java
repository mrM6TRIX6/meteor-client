/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.themes;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;

import java.util.ArrayList;
import java.util.List;

public class Themes extends System<Themes> {
    
    // Why is it here
    private List<String> dontShowAgainPrompts = new ArrayList<>();
    
    public Themes() {
        super("themes");
    }
    
    public static Themes get() {
        return Systems.get(Themes.class);
    }
    
    public List<String> getDontShowAgainPrompts() {
        return dontShowAgainPrompts;
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.add("dontShowAgainPrompts", JsonUtils.listToJson(dontShowAgainPrompts));
        
        return jsonObject;
    }
    
    @Override
    public Themes fromJson(JsonObject jsonObject) {
        if (jsonObject.has("dontShowAgainPrompts")) {
            dontShowAgainPrompts = JsonUtils.listFromJson(jsonObject, "dontShowAgainPrompts");
        }
        
        return this;
    }
    
}
