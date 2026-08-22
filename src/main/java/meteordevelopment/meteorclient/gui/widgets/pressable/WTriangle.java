/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.GuiConstants;
import net.minecraft.client.gui.DrawContext;

public class WTriangle extends WPressable {

    public double rotation;

    @Override
    protected void onCalculateSize() {
        double s = GuiConstants.textHeight();

        width = s;
        height = s;
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        icon(GuiConstants.TRIANGLE, x, y, width, height, rotation, GuiConstants.BACKGROUND.get(pressed, mouseOver));
    }

}
