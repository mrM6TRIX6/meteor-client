/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.renderer.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class WItem extends WWidget {

    protected ItemStack itemStack;

    public WItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    protected void onCalculateSize() {
        double s = GuiConstants.scale(32);

        width = s;
        height = s;
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        if (!itemStack.isEmpty()) {
            RenderUtils.drawItem(context, itemStack, (int) x, (int) y, (float) GuiConstants.scale(2), true);
        }
    }

    public void set(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

}
