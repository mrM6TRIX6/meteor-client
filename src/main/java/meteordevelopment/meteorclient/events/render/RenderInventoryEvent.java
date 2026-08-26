/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import net.minecraft.client.gui.DrawContext;

public class RenderInventoryEvent {

    private static final RenderInventoryEvent INSTANCE = new RenderInventoryEvent();

    public DrawContext context;
    public int x;
    public int y;
    public int width;
    public int height;

    public static RenderInventoryEvent get(DrawContext context, int x, int y, int width, int height) {
        INSTANCE.context = context;
        INSTANCE.x = x;
        INSTANCE.y = y;
        INSTANCE.width = width;
        INSTANCE.height = height;

        return INSTANCE;
    }

}
