/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.GuiOverlays;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WRoot;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.name.Namer;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.List;

public class WDropdown<T> extends WPressable {

    public Runnable action;

    protected List<T> choices;
    protected T choice;

    /**
     * Supplies the label of every choice, so the dropdown works for any type instead of only for ones that
     * implement a naming interface.
     */
    protected final Namer<T> namer;

    protected double maxValueWidth;

    protected WDropdownRoot root;
    protected boolean expanded;
    protected double animProgress;

    public WDropdown(List<T> choices, T choice) {
        this(choices, choice, Namer.auto());
    }

    public WDropdown(List<T> choices, T choice, Namer<T> namer) {
        this.choices = choices;
        this.namer = namer;

        set(choice);
    }

    /**
     * Every constant of the enum as choices.
     */
    public static <E extends Enum<E>> WDropdown<E> of(E choice) {
        return new WDropdown<>(Arrays.asList(choice.getDeclaringClass().getEnumConstants()), choice);
    }

    @Override
    public void init() {
        root = new WDropdownRoot();
        root.spacing = 0;

        for (int i = 0; i < choices.size(); i++) {
            WDropdownValue widget = new WDropdownValue();
            widget.value = choices.get(i);

            Cell<?> cell = root.add(widget).padHorizontal(2).expandWidgetX();
            if (i >= choices.size() - 1) {
                cell.padBottom(2);
            }
        }
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        maxValueWidth = 0;
        for (T value : choices) {
            double valueWidth = GuiConstants.textWidth(namer.display(value));
            maxValueWidth = Math.max(maxValueWidth, valueWidth);
        }

        root.calculateSize();

        width = pad + maxValueWidth + pad + GuiConstants.textHeight() + pad;
        height = pad + GuiConstants.textHeight() + pad;

        root.width = width;
    }

    @Override
    protected void onCalculateWidgetPositions() {
        super.onCalculateWidgetPositions();

        root.x = x;
        root.y = y + height;

        root.calculateWidgetPositions();
    }

    @Override
    protected void onPressed(int button) {
        expanded = !expanded;
        root.setFocused(expanded);
        setFocused(expanded);
    }

    public T get() {
        return choice;
    }

    public void set(T value) {
        this.choice = value;
    }

    @Override
    public void move(double deltaX, double deltaY) {
        super.move(deltaX, deltaY);

        root.move(deltaX, deltaY);
    }

    @Override
    public boolean render(DrawContext context, double mouseX, double mouseY, double delta) {
        boolean render = super.render(context, mouseX, mouseY, delta);

        animProgress += (expanded ? 1 : -1) * delta * 14;
        animProgress = MathHelper.clamp(animProgress, 0, 1);

        WView view = getView();
        boolean rootInView = view == null || view.isWidgetInView(root);

        if (!render && animProgress > 0 && rootInView) {
            // The expanded list has to be drawn on top of everything else in the tree.
            GuiOverlays.add(() -> {
                pushScissor(context, x, y + height, width, root.height * animProgress);
                root.render(context, mouseX, mouseY, delta);
                popScissor(context);
            });
        }

        if (expanded && root.mouseOver) {
            GuiConstants.disableHoverColor = true;
        }

        return render;
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        double pad = pad();
        double s = GuiConstants.textHeight();

        renderBackground(pressed, mouseOver);

        String text = namer.display(get());
        text(text, x + pad + maxValueWidth / 2 - GuiConstants.textWidth(text) / 2, y + pad, GuiConstants.TEXT);

        icon(GuiConstants.TRIANGLE, x + pad + maxValueWidth + pad, y + pad, s, GuiConstants.TEXT);
    }

    // Events

    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        boolean used = false;

        if (!mouseOver && !root.mouseOver) {
            expanded = false;
        }

        if (super.onMouseClicked(click, doubled)) {
            used = true;
        }

        if (expanded && root.mouseClicked(click, doubled)) {
            used = true;
        }

        return used;
    }

    @Override
    public boolean onMouseReleased(Click click) {
        if (super.onMouseReleased(click)) {
            return true;
        }

        return expanded && root.mouseReleased(click);
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        super.onMouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);

        if (expanded) {
            root.mouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
        }
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        if (super.onMouseScrolled(amount)) {
            return true;
        }

        if (expanded) {
            return root.mouseScrolled(amount);
        }

        return false;
    }

    @Override
    public boolean onKeyPressed(KeyInput input) {
        if (super.onKeyPressed(input)) {
            return true;
        }

        return expanded && root.keyPressed(input);
    }

    @Override
    public boolean onKeyRepeated(KeyInput input) {
        if (super.onKeyRepeated(input)) {
            return true;
        }

        return expanded && root.keyRepeated(input);
    }

    @Override
    public boolean onCharTyped(CharInput input) {
        if (super.onCharTyped(input)) {
            return true;
        }

        return expanded && root.charTyped(input);
    }

    // Widgets

    protected static class WDropdownRoot extends WVerticalList implements WRoot {

        @Override
        public void invalidate() {}

        @Override
        protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
            double s = GuiConstants.scale(2);
            int c = GuiConstants.color(GuiConstants.OUTLINE.get());

            Render2D.rect((float) x, (float) (y + height - s), (float) width, (float) s, c);
            Render2D.rect((float) x, (float) y, (float) s, (float) (height - s), c);
            Render2D.rect((float) (x + width - s), (float) y, (float) s, (float) (height - s), c);
        }

    }

    protected class WDropdownValue extends WPressable {

        protected T value;

        @Override
        protected void onPressed(int button) {
            boolean isNew = !WDropdown.this.choice.equals(value);

            WDropdown.this.choice = value;
            expanded = false;

            if (isNew && WDropdown.this.action != null) {
                WDropdown.this.action.run();
            }
        }

        @Override
        protected void onCalculateSize() {
            double pad = pad();

            width = pad + GuiConstants.textWidth(namer.display(value)) + pad;
            height = pad + GuiConstants.textHeight() + pad;
        }

        @Override
        protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
            // Brighter than a normal background because the list sits on top of one.
            Render2D.rect((float) x, (float) y, (float) width, (float) height, GuiConstants.brighter(GuiConstants.BACKGROUND.get(pressed, mouseOver, true)));

            String text = namer.display(value);
            text(text, x + width / 2 - GuiConstants.textWidth(text) / 2, y + pad(), GuiConstants.TEXT);
        }

    }

}
