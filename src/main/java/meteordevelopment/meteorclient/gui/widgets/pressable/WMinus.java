/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.GuiConstants;
import net.minecraft.client.gui.DrawContext;

public class WMinus extends WPressable {

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        double s = GuiConstants.textHeight();

        width = pad + s + pad;
        height = pad + s + pad;
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        double pad = pad();
        double s = GuiConstants.scale(3);

        renderBackground(pressed, mouseOver);
        rect(x + pad, y + height / 2 - s / 2, width - pad * 2, s, GuiConstants.MINUS);
    }

}
