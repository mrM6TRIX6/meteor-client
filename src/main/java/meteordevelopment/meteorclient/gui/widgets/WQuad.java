/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.DrawContext;

public class WQuad extends WWidget {

    public Color color;

    public WQuad(Color color) {
        this.color = color;
    }

    @Override
    protected void onCalculateSize() {
        double s = GuiConstants.scale(32);

        width = s;
        height = s;
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        rect(color);
    }

}
