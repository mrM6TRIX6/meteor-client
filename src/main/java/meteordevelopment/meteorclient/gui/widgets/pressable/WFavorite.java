/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.GuiConstants;
import net.minecraft.client.gui.DrawContext;

public class WFavorite extends WPressable {

    public boolean checked;

    public WFavorite(boolean checked) {
        this.checked = checked;
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
        double pad = pad();
        double s = GuiConstants.textHeight();

        icon(checked ? GuiConstants.FAVORITE_YES : GuiConstants.FAVORITE_NO, x + pad, y + pad, s, GuiConstants.FAVORITE);
    }

}
