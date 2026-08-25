/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.containers;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.renderer.RenderUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class WView extends WVerticalList {

    public double maxHeight = Double.MAX_VALUE;
    public boolean scrollOnlyWhenMouseOver = true;
    public boolean hasScrollBar = true;

    protected boolean canScroll;
    private double actualHeight;

    private double scroll;
    private double targetScroll;
    private boolean moveAfterPositionWidgets;

    protected boolean handleMouseOver;
    protected boolean handlePressed;

    @Override
    public void init() {
        maxHeight = RenderUtils.getWindowHeight() - GuiConstants.scale(128);
    }

    @Override
    protected void onCalculateSize() {
        boolean couldScroll = canScroll;
        canScroll = false;
        widthRemove = 0;

        super.onCalculateSize();

        if (height > maxHeight) {
            actualHeight = height;
            height = maxHeight;
            canScroll = true;

            if (hasScrollBar) {
                widthRemove = handleWidth() * 2;
                width += widthRemove;
            }

            if (couldScroll) {
                moveAfterPositionWidgets = true;
            }
        } else {
            actualHeight = height;
            scroll = 0;
            targetScroll = 0;
        }
    }

    @Override
    protected void onCalculateWidgetPositions() {
        super.onCalculateWidgetPositions();

        if (moveAfterPositionWidgets) {
            scroll = MathHelper.clamp(scroll, 0, actualHeight - height);
            targetScroll = scroll;

            moveCells(0, -scroll);

            moveAfterPositionWidgets = false;
        }
    }

    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        if (handleMouseOver && click.button() == GLFW_MOUSE_BUTTON_LEFT && !doubled) {
            handlePressed = true;
            return true;
        }

        return false;
    }

    @Override
    public boolean onMouseReleased(Click click) {
        if (handlePressed) {
            handlePressed = false;
        }

        return false;
    }

    /**
     * A view nested in another one only gets events while the cursor is inside its parent, so a drag started on the
     * handle would never be released once the cursor leaves it. Counting as focused while dragging keeps the moves and
     * the release coming, the same way {@link meteordevelopment.meteorclient.gui.widgets.input.WSlider} focuses itself.
     *
     * <p>{@link WContainer#isFocused()} reports whether any child is focused and ignores the container's own flag,
     * hence the override instead of a {@link #setFocused(boolean)} call.
     */
    @Override
    public boolean isFocused() {
        return handlePressed || super.isFocused();
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        handleMouseOver = false;

        if (canScroll && hasScrollBar) {
            double x = handleX();
            double y = handleY();

            if (mouseX >= x && mouseX <= x + handleWidth() && mouseY >= y && mouseY <= y + handleHeight()) {
                handleMouseOver = true;
            }
        }

        if (handlePressed) {
            double preScroll = scroll;
            double mouseDelta = mouseY - lastMouseY;

            // The handle is drawn at (height - handleHeight()) * (scroll / (actualHeight - height)) and handleHeight()
            // is height * height / actualHeight, so inverting that mapping to make the handle follow the cursor exactly
            // leaves nothing but the ratio of the content to the visible part
            scroll += Math.round(mouseDelta * (actualHeight / height));
            scroll = MathHelper.clamp(scroll, 0, actualHeight - height);

            targetScroll = scroll;

            double delta = scroll - preScroll;
            if (delta != 0) {
                moveCells(0, -delta);
            }
        }
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        if (scrollOnlyWhenMouseOver && !mouseOver) {
            return false;
        }

        double preTargetScroll = targetScroll;

        targetScroll -= Math.round(GuiConstants.scale(amount * 40));
        targetScroll = MathHelper.clamp(targetScroll, 0, actualHeight - height);

        // A nested view that has nothing left to scroll lets the one around it take over instead of swallowing the
        // scroll, so the cursor sitting on a short list doesn't lock the window in place
        return targetScroll != preTargetScroll;
    }

    @Override
    public boolean render(DrawContext context, double mouseX, double mouseY, double delta) {
        updateScroll(delta);

        if (canScroll) {
            pushScissor(context, x, y, width, height);
        }
        boolean render = super.render(context, mouseX, mouseY, delta);
        if (canScroll) {
            popScissor(context);
        }

        return render;
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        if (canScroll && hasScrollBar) {
            rect(handleX(), handleY(), handleWidth(), handleHeight(), GuiConstants.SCROLLBAR.get(handlePressed, handleMouseOver));
        }
    }

    private void updateScroll(double delta) {
        double preScroll = scroll;
        double max = actualHeight - height;

        if (Math.abs(targetScroll - scroll) < 1) {
            scroll = targetScroll;
        } else if (targetScroll > scroll) {
            scroll += Math.round(GuiConstants.scale(delta * 300 + delta * 100 * (Math.abs(targetScroll - scroll) / 10)));
            if (scroll > targetScroll) {
                scroll = targetScroll;
            }
        } else if (targetScroll < scroll) {
            scroll -= Math.round(GuiConstants.scale(delta * 300 + delta * 100 * (Math.abs(targetScroll - scroll) / 10)));
            if (scroll < targetScroll) {
                scroll = targetScroll;
            }
        }

        scroll = MathHelper.clamp(scroll, 0, max);

        double change = scroll - preScroll;
        if (change != 0) {
            moveCells(0, -change);
        }
    }

    @Override
    protected boolean propagateEvents(WWidget widget) {
        return (mouseOver && isWidgetInView(widget)) || widget.isFocused();
    }

    protected double handleWidth() {
        return GuiConstants.scale(6);
    }

    protected double handleHeight() {
        return height / actualHeight * height;
    }

    protected double handleX() {
        return x + width - handleWidth();
    }

    protected double handleY() {
        return y + (height - handleHeight()) * (scroll / (actualHeight - height));
    }

    public boolean isWidgetInView(WWidget widget) {
        return widget.y < y + height && widget.y + widget.height > y;
    }

}
