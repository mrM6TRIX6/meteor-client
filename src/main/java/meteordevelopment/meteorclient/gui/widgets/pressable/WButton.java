/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.GuiConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class WButton extends WPressable {

    protected String text;
    protected double textWidth;

    protected Identifier texture;

    public WButton(String text) {
        this(text, null);
    }

    public WButton(Identifier texture) {
        this(null, texture);
    }

    public WButton(String text, Identifier texture) {
        this.text = text;
        this.texture = texture;
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        if (text != null) {
            textWidth = GuiConstants.textWidth(text);

            width = pad + textWidth + pad;
            height = pad + GuiConstants.textHeight() + pad;
        } else {
            double s = GuiConstants.textHeight();

            width = pad + s + pad;
            height = pad + s + pad;
        }
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        double pad = pad();

        renderBackground(pressed, mouseOver);

        if (text != null) {
            text(text, x + width / 2 - textWidth / 2, y + pad, GuiConstants.TEXT);
        } else {
            double s = GuiConstants.textHeight();
            icon(texture, x + width / 2 - s / 2, y + pad, s, GuiConstants.TEXT);
        }
    }

    public void set(String text) {
        if (this.text == null || Math.round(GuiConstants.textWidth(text)) != textWidth) {
            invalidate();
        }

        this.text = text;
    }

    public String getText() {
        return text;
    }

}
