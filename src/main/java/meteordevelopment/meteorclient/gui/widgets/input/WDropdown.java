/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WRoot;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public abstract class WDropdown<T extends IDisplayName> extends WPressable {
    
    public Runnable action;
    
    protected List<T> choices;
    protected T choice;
    
    protected double maxValueWidth;
    
    protected WDropdownRoot root;
    protected boolean expanded;
    protected double animProgress;
    
    public WDropdown(List<T> choices, T choice) {
        this.choices = choices;
        
        set(choice);
    }
    
    @Override
    public void init() {
        root = createRootWidget();
        root.theme = theme;
        root.spacing = 0;
        
        for (int i = 0; i < choices.size(); i++) {
            WDropdownValue widget = createValueWidget();
            widget.theme = theme;
            widget.value = choices.get(i);
            
            Cell<?> cell = root.add(widget).padHorizontal(2).expandWidgetX();
            if (i >= choices.size() - 1) {
                cell.padBottom(2);
            }
        }
    }
    
    protected abstract WDropdownRoot createRootWidget();
    
    protected abstract WDropdownValue createValueWidget();
    
    @Override
    protected void onCalculateSize() {
        double pad = pad();
        
        maxValueWidth = 0;
        for (T value : choices) {
            double valueWidth = theme.textWidth(value.toString());
            maxValueWidth = Math.max(maxValueWidth, valueWidth);
        }
        
        root.calculateSize();
        
        width = pad + maxValueWidth + pad + theme.textHeight() + pad;
        height = pad + theme.textHeight() + pad;
        
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
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        boolean render = super.render(renderer, mouseX, mouseY, delta);
        
        animProgress += (expanded ? 1 : -1) * delta * 14;
        animProgress = MathHelper.clamp(animProgress, 0, 1);
        
        WView view = getView();
        boolean rootInView = view == null || view.isWidgetInView(root);
        
        if (!render && animProgress > 0 && rootInView) {
            renderer.absolutePost(() -> {
                renderer.scissorStart(x, y + height, width, root.height * animProgress);
                root.render(renderer, mouseX, mouseY, delta);
                renderer.scissorEnd();
            });
        }
        
        if (expanded && root.mouseOver) {
            theme.disableHoverColor = true;
        }
        
        return render;
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
    
    protected abstract static class WDropdownRoot extends WVerticalList implements WRoot {
        
        @Override
        public void invalidate() {}
        
    }
    
    protected abstract class WDropdownValue extends WPressable {
        
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
        
    }
    
}
