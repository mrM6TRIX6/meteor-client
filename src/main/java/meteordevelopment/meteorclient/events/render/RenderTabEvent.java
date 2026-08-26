/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import net.minecraft.client.gui.DrawContext;

public class RenderTabEvent {

    private static final RenderTabEvent INSTANCE = new RenderTabEvent();

    public DrawContext context;
    public int width;

    public static RenderTabEvent get(DrawContext context, int width) {
        INSTANCE.context = context;
        INSTANCE.width = width;

        return INSTANCE;
    }

}
