/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A single line of text, or multiple lines when the text contains new lines or a max width is set.
 */
public class WLabel extends WPressable {

    public Color color;

    protected String text;
    protected boolean title;
    protected double maxWidth;

    /**
     * Only filled when the text is split into multiple lines, empty otherwise.
     */
    protected final List<String> lines = new ArrayList<>(2);

    public WLabel(String text) {
        this(text, false, 0);
    }

    public WLabel(String text, boolean title) {
        this(text, title, 0);
    }

    public WLabel(String text, double maxWidth) {
        this(text, false, maxWidth);
    }

    public WLabel(String text, boolean title, double maxWidth) {
        this.text = text;
        this.title = title;
        this.maxWidth = maxWidth;
    }

    protected boolean isMultiline() {
        return maxWidth != 0 || text.contains("\n");
    }

    @Override
    protected void onCalculateSize() {
        lines.clear();

        if (!isMultiline()) {
            width = GuiConstants.textWidth(text, title);
            height = GuiConstants.textHeight(title);
            return;
        }

        String[] textLines = text.split("\n");
        double maxLineWidth = 0;

        if (this.maxWidth == 0) {
            for (String line : textLines) {
                lines.add(line);
                maxLineWidth = Math.max(maxLineWidth, GuiConstants.textWidth(line, title));
            }
        } else {
            StringBuilder sb = new StringBuilder();

            double lineWidth = 0;
            double spaceWidth = GuiConstants.textWidth(" ", title);
            double maxWidth = GuiConstants.scale(this.maxWidth);

            int iInLine = 0;

            for (String line : textLines) {
                for (String word : line.split(" ")) {
                    double wordWidth = GuiConstants.textWidth(word, title);

                    double toAdd = wordWidth;
                    if (iInLine > 0) {
                        toAdd += spaceWidth;
                    }

                    if (lineWidth + toAdd > maxWidth) {
                        lines.add(sb.toString());
                        sb.setLength(0);

                        sb.append(word);
                        lineWidth = wordWidth;
                        iInLine = 1;
                    } else {
                        if (iInLine > 0) {
                            sb.append(' ');
                            lineWidth += spaceWidth;
                        }

                        sb.append(word);
                        lineWidth += wordWidth;
                        iInLine++;
                    }

                    maxLineWidth = Math.max(maxLineWidth, lineWidth);
                }

                lines.add(sb.toString());
                sb.setLength(0);
                lineWidth = 0;
                iInLine = 0;
            }

            if (!sb.isEmpty()) {
                lines.add(sb.toString());
            }
        }

        width = maxLineWidth;
        height = GuiConstants.textHeight(title) * lines.size();
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        Color color = this.color != null ? this.color : (title ? GuiConstants.TITLE_TEXT : GuiConstants.TEXT);

        if (lines.isEmpty()) {
            if (!text.isEmpty()) {
                text(text, x, y, color, title);
            }
            return;
        }

        double h = GuiConstants.textHeight(title);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isEmpty()) {
                text(line, x, y + h * i, color, title);
            }
        }
    }

    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        if (action != null) {
            return super.onMouseClicked(click, doubled);
        }
        return false;
    }

    @Override
    public boolean onMouseReleased(Click click) {
        if (action != null) {
            return super.onMouseReleased(click);
        }
        return false;
    }

    public void set(String text) {
        if (this.text.equals(text)) {
            return;
        }

        boolean multiline = isMultiline();
        this.text = text;

        if (multiline || isMultiline() || Math.round(GuiConstants.textWidth(text, title)) != Math.round(width)) {
            invalidate();
        }
    }

    public String get() {
        return text;
    }

    public WLabel color(Color color) {
        this.color = color;
        return this;
    }

}
