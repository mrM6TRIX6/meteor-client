/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui;

import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import static meteordevelopment.meteorclient.renderer.RenderUtils.getWindowHeight;
import static meteordevelopment.meteorclient.renderer.RenderUtils.getWindowWidth;

/**
 * Holds the tooltip requested by widgets during a frame and draws it on top of everything once the frame is done.
 */
public class GuiTooltips {

    private GuiTooltips() {}

    private static String tooltip, lastTooltip;
    private static WTooltip widget;
    private static double animProgress;

    /**
     * Called by widgets while they render, the tooltip is drawn after the whole widget tree.
     */
    public static void set(String text) {
        tooltip = text;
    }

    public static WWidget widget() {
        return widget;
    }

    public static boolean render(DrawContext context, double mouseX, double mouseY, double delta) {
        animProgress += (tooltip != null ? 1 : -1) * delta * 14;
        animProgress = MathHelper.clamp(animProgress, 0, 1);

        boolean rendered = false;

        if (animProgress > 0) {
            if (tooltip != null && !tooltip.equals(lastTooltip)) {
                widget = new WTooltip(tooltip);
                widget.init();
            }

            if (widget != null) {
                double deltaX = -widget.x + mouseX + 12;
                double deltaY = -widget.y + mouseY + 12;

                if (mouseX + 12 + widget.width > getWindowWidth()) {
                    deltaX = -widget.x + getWindowWidth() - widget.width;
                }
                if (mouseY + 12 + widget.height > getWindowHeight()) {
                    deltaY = -widget.y + getWindowHeight() - widget.height;
                }

                widget.move(deltaX, deltaY);

                // Fades on top of the screen's own fade.
                float preAlpha = GuiConstants.alpha;
                GuiConstants.alpha = (float) (preAlpha * animProgress);
                widget.render(context, mouseX, mouseY, delta);
                GuiConstants.alpha = preAlpha;

                lastTooltip = tooltip;
                rendered = true;
            }
        }

        tooltip = null;
        return rendered;
    }

}
