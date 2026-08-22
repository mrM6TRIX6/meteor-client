/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.containers;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class WSection extends WVerticalList {

    public Runnable action;

    protected String title;

    protected boolean expanded;
    protected double animProgress;

    private WHeader header;
    protected final WWidget headerWidget;

    private double actualWidth, actualHeight;
    private double forcedHeight = -1;
    private boolean firstTime = true;

    public WSection(String title) {
        this(title, true, null);
    }

    public WSection(String title, boolean expanded) {
        this(title, expanded, null);
    }

    public WSection(String title, boolean expanded, WWidget headerWidget) {
        this.title = title;
        this.expanded = expanded;
        this.headerWidget = headerWidget;

        animProgress = expanded ? 1 : 0;
    }

    @Override
    public void init() {
        header = new WHeader(title);

        super.add(header).expandX();
    }

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
        return super.add(widget).padHorizontal(6);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    protected void onCalculateSize() {
        if (forcedHeight == -1) {
            super.onCalculateSize();

            actualWidth = width;
            actualHeight = height;
        } else {
            width = actualWidth;
            height = forcedHeight;

            if (animProgress == 1) {
                forcedHeight = -1;
            }
        }

        if (firstTime) {
            firstTime = false;

            forcedHeight = (actualHeight - header.height) * animProgress + header.height;
            onCalculateSize();
        }
    }

    @Override
    public boolean render(DrawContext context, double mouseX, double mouseY, double delta) {
        if (!visible) {
            return true;
        }

        double preProgress = animProgress;

        animProgress += (expanded ? 1 : -1) * delta * 14;
        animProgress = MathHelper.clamp(animProgress, 0, 1);

        if (animProgress != preProgress) {
            forcedHeight = (actualHeight - header.height) * animProgress + header.height;
            invalidate();
        }

        boolean scissor = (animProgress != 0 && animProgress != 1) || (expanded && animProgress != 1);
        if (scissor) {
            pushScissor(context, x, y, width, ((height - header.height) * animProgress + header.height));
        }
        boolean toReturn = super.render(context, mouseX, mouseY, delta);
        if (scissor) {
            popScissor(context);
        }

        return toReturn;
    }

    @Override
    protected void renderWidget(WWidget widget, DrawContext context, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0 || widget instanceof WHeader) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    protected boolean propagateEvents(WWidget widget) {
        return expanded || widget instanceof WHeader;
    }

    protected class WHeader extends WHorizontalList {

        protected String title;

        private WTriangle triangle;

        public WHeader(String title) {
            this.title = title;
        }

        @Override
        public void init() {
            add(new WHorizontalSeparator(title)).expandX();

            if (headerWidget != null) {
                add(headerWidget);
            }

            triangle = new WHeaderTriangle();
            triangle.action = this::onClick;

            add(triangle);
        }

        @Override
        public boolean onMouseClicked(Click click, boolean doubled) {
            if (mouseOver && click.button() == GLFW_MOUSE_BUTTON_LEFT && !doubled) {
                onClick();
                return true;
            }

            return false;
        }

        protected void onClick() {
            setExpanded(!expanded);

            if (action != null) {
                action.run();
            }
        }

        @Override
        protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
            triangle.rotation = (1 - animProgress) * -90;
        }

    }

    /**
     * Same as a normal triangle, but always drawn in the text color instead of the background one.
     */
    protected static class WHeaderTriangle extends WTriangle {

        @Override
        protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
            icon(GuiConstants.TRIANGLE, x, y, width, height, rotation, GuiConstants.TEXT);
        }

    }

}
