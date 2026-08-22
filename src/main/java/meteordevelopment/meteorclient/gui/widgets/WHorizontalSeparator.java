/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.gui.DrawContext;

public class WHorizontalSeparator extends WWidget {

    protected String text;
    protected double textWidth;

    public WHorizontalSeparator() {
        this(null);
    }

    public WHorizontalSeparator(String text) {
        this.text = text;
    }

    @Override
    protected void onCalculateSize() {
        if (text != null) {
            textWidth = GuiConstants.textWidth(text);
        }

        width = 1;
        height = text != null ? GuiConstants.textHeight() : GuiConstants.scale(3);
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        int edges = GuiConstants.color(GuiConstants.SEPARATOR_EDGES);
        int center = GuiConstants.color(GuiConstants.SEPARATOR_CENTER);

        if (text == null) {
            double s = GuiConstants.scale(1);
            double w = width / 2;

            Render2D.rect((float) x, (float) (y + s), (float) w, (float) s, edges, center, center, edges);
            Render2D.rect((float) (x + w), (float) (y + s), (float) w, (float) s, center, edges, edges, center);
            return;
        }

        double s = GuiConstants.scale(2);
        double h = GuiConstants.scale(1);

        double textStart = Math.round(width / 2.0 - textWidth / 2.0 - s);
        double textEnd = s + textStart + textWidth + s;

        double offsetY = Math.round(height / 2.0);

        Render2D.rect((float) x, (float) (y + offsetY), (float) textStart, (float) h, edges, center, center, edges);
        text(text, x + textStart + s, y, GuiConstants.SEPARATOR_TEXT);
        Render2D.rect((float) (x + textEnd), (float) (y + offsetY), (float) (width - textEnd), (float) h, center, edges, edges, center);
    }

}
