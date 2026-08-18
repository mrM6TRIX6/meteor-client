/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import net.minecraft.client.gui.DrawContext;

public class DrawEvent {
    
    private static final DrawEvent INSTANCE = new DrawEvent();
    
    public DrawContext context;
    public float partialTicks;
    public Layer layer;
    
    public static DrawEvent get(DrawContext context, float partialTicks, Layer layer) {
        INSTANCE.context = context;
        INSTANCE.partialTicks = partialTicks;
        INSTANCE.layer = layer;
        
        return INSTANCE;
    }
    
    public enum Layer {
        GAME,
        SCREEN_BACKGROUND,
        CHAT_OVERLAY
    }
    
}
