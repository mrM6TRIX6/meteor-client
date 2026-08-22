/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.GuiTooltips;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.renderer.color.Color;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.image.BuiltImage;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Identifier;

public abstract class WWidget {

    public boolean visible = true;

    public double x, y;
    public double width, height;
    public double minWidth;

    public WWidget parent;
    public String tooltip;

    public boolean mouseOver;
    public boolean focused;
    protected double mouseOverTimer;

    public void init() {}

    public void move(double deltaX, double deltaY) {
        x = Math.round(x + deltaX);
        y = Math.round(y + deltaY);
    }

    public double pad() {
        return GuiConstants.pad();
    }

    // Layout

    public void calculateSize() {
        onCalculateSize();

        double minWidth = GuiConstants.scale(this.minWidth);
        if (width < minWidth) {
            width = minWidth;
        }

        width = Math.round(width);
        height = Math.round(height);
    }

    protected void onCalculateSize() {}

    public void calculateWidgetPositions() {
        x = Math.round(x);
        y = Math.round(y);

        onCalculateWidgetPositions();
    }

    protected void onCalculateWidgetPositions() {}

    // Rendering

    public boolean render(DrawContext context, double mouseX, double mouseY, double delta) {
        if (!visible) {
            return true;
        }

        if (isOver(mouseX, mouseY)) {
            mouseOverTimer += delta;
            if (mouseOverTimer >= 1 && tooltip != null) {
                WView view = getView();
                if (view == null || view.mouseOver) {
                    GuiTooltips.set(tooltip);
                }
            }
        } else {
            mouseOverTimer = 0;
        }

        onRender(context, mouseX, mouseY, delta);
        return false;
    }

    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {}

    /**
     * The background shared by every interactive widget: a filled rectangle with an outline around it.
     */
    protected void renderBackground(boolean pressed, boolean mouseOver) {
        renderBackground(x, y, width, height, pressed, mouseOver);
    }

    protected void renderBackground(double x, double y, double width, double height, boolean pressed, boolean mouseOver) {
        double s = GuiConstants.scale(2);

        Render2D.rect((float) (x + s), (float) (y + s), (float) (width - s * 2), (float) (height - s * 2), GuiConstants.color(GuiConstants.BACKGROUND.get(pressed, mouseOver)));
        Render2D.outline((float) x, (float) y, (float) width, (float) height, 0, (float) s, GuiConstants.color(GuiConstants.OUTLINE.get(pressed, mouseOver)));
    }

    protected void rect(double x, double y, double width, double height, Color color) {
        Render2D.rect((float) x, (float) y, (float) width, (float) height, GuiConstants.color(color));
    }

    protected void rect(Color color) {
        rect(x, y, width, height, color);
    }

    /**
     * A tinted square icon, the gui equivalent of the old {@code GuiTexture} quads.
     */
    protected void icon(Identifier texture, double x, double y, double size, Color color) {
        Render2D.image(texture, (float) x, (float) y, (float) size, (float) size, 0, GuiConstants.color(color));
    }

    /**
     * A tinted icon rotated around its own center.
     */
    protected void icon(Identifier texture, double x, double y, double width, double height, double rotation, Color color) {
        Render2D.image(new BuiltImage(texture, (float) x, (float) y, (float) width, (float) height, 0, GuiConstants.color(color))
            .withRotation((float) rotation, (float) (x + width / 2), (float) (y + height / 2)));
    }

    protected void text(String text, double x, double y, Color color, boolean title) {
        GuiConstants.text(text, x, y, color, title);
    }

    protected void text(String text, double x, double y, Color color) {
        text(text, x, y, color, false);
    }

    /**
     * Clips everything drawn until the matching {@link #popScissor(DrawContext)} to the given rectangle.
     */
    protected void pushScissor(DrawContext context, double x, double y, double width, double height) {
        Render2D.pushScissor(context, (float) x, (float) y, (float) width, (float) height);
    }

    protected void popScissor(DrawContext context) {
        Render2D.popScissor(context);
    }

    // Events

    public boolean mouseClicked(Click click, boolean doubled) {
        return onMouseClicked(click, doubled);
    }

    public boolean onMouseClicked(Click click, boolean doubled) {
        return false;
    }

    public boolean mouseReleased(Click click) {
        return onMouseReleased(click);
    }

    public boolean onMouseReleased(Click click) {
        return false;
    }

    public void mouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        mouseOver = isOver(mouseX, mouseY);
        onMouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
    }

    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {}

    public boolean mouseScrolled(double amount) {
        return onMouseScrolled(amount);
    }

    public boolean onMouseScrolled(double amount) {
        return false;
    }

    public boolean keyPressed(KeyInput input) {
        return onKeyPressed(input);
    }

    public boolean onKeyPressed(KeyInput input) {
        return false;
    }

    public boolean keyRepeated(KeyInput input) {
        return onKeyRepeated(input);
    }

    public boolean onKeyRepeated(KeyInput input) {
        return false;
    }

    public boolean charTyped(CharInput input) {
        return onCharTyped(input);
    }

    public boolean onCharTyped(CharInput input) {
        return false;
    }

    // Other

    public void invalidate() {
        WWidget root = getRoot();
        if (root != null) {
            root.invalidate();
        }
    }

    protected WWidget getRoot() {
        return parent != null ? parent.getRoot() : (this instanceof WRoot ? this : null);
    }

    public WView getView() {
        return this instanceof WView ? (WView) this : (parent != null ? parent.getView() : null);
    }

    public boolean isOver(double x, double y) {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        if (this.focused != focused) {
            this.focused = focused;
        }
    }

}
