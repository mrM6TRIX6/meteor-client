/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.gui.DrawContext;

public class WVerticalSeparator extends WWidget {

    @Override
    protected void onCalculateSize() {
        width = GuiConstants.scale(3);
        height = 1;
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        int edges = GuiConstants.color(GuiConstants.SEPARATOR_EDGES);
        int center = GuiConstants.color(GuiConstants.SEPARATOR_CENTER);

        double s = GuiConstants.scale(1);
        double offsetX = Math.round(width / 2.0);
        double h = height / 2;

        Render2D.rect((float) (x + offsetX), (float) y, (float) s, (float) h, edges, edges, center, center);
        Render2D.rect((float) (x + offsetX), (float) (y + h), (float) s, (float) h, center, center, edges, edges);
    }

}
