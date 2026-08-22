/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import net.minecraft.client.gui.DrawContext;

public class WTooltip extends WContainer implements WRoot {

    private boolean valid;

    protected String text;

    public WTooltip(String text) {
        this.text = text;
    }

    @Override
    public void init() {
        add(new WLabel(text)).pad(4);
    }

    @Override
    public void invalidate() {
        valid = false;
    }

    @Override
    public boolean render(DrawContext context, double mouseX, double mouseY, double delta) {
        if (text == null) {
            return false;
        }

        if (!valid) {
            calculateSize();
            calculateWidgetPositions();

            valid = true;
        }

        return super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        rect(GuiConstants.BACKGROUND.get());
    }

}
