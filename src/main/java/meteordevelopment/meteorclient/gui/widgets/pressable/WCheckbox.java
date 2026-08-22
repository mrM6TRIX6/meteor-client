/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.GuiConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class WCheckbox extends WPressable {

    public boolean checked;

    private double animProgress;

    public WCheckbox(boolean checked) {
        this.checked = checked;
        this.animProgress = checked ? 1 : 0;
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        double s = GuiConstants.textHeight();

        width = pad + s + pad;
        height = pad + s + pad;
    }

    @Override
    protected void onPressed(int button) {
        checked = !checked;
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        animProgress += (checked ? 1 : -1) * delta * 14;
        animProgress = MathHelper.clamp(animProgress, 0, 1);

        renderBackground(pressed, mouseOver);

        if (animProgress > 0) {
            double cs = (width - GuiConstants.scale(2)) / 1.75 * animProgress;
            rect(x + (width - cs) / 2, y + (height - cs) / 2, cs, cs, GuiConstants.CHECKBOX);
        }
    }

}
