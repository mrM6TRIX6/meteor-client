/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.renderer.color.Color;
import meteordevelopment.meteorclient.utils.name.Namer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WMultiChoice<T> extends WWidget {
    
    public double spacing = 4;
    
    public double buttonPadding = 4;
    
    public Runnable action;
    
    protected final List<T> choices;
    protected final SequencedSet<T> selected = new LinkedHashSet<>();
    
    protected final Namer<T> namer;
    
    protected final double maxWidth;
    
    protected final double[] choiceX, choiceY, choiceWidth, choiceHeight;
    
    protected final double[] animProgress;
    
    protected int hovered = -1;
    protected int pressed = -1;
    
    private final Color textColor = new Color();
    private final Color backgroundColor = new Color();
    
    public WMultiChoice(SequencedSet<T> choices, SequencedSet<T> selected, double maxWidth) {
        this(choices, selected, maxWidth, Namer.auto());
    }

    public WMultiChoice(SequencedSet<T> choices, SequencedSet<T> selected, double maxWidth, Namer<T> namer) {
        this.choices = List.copyOf(choices);
        this.maxWidth = maxWidth;
        this.namer = namer;
        
        int size = this.choices.size();
        choiceX = new double[size];
        choiceY = new double[size];
        choiceWidth = new double[size];
        choiceHeight = new double[size];
        animProgress = new double[size];
        
        set(selected);
        
        // Start at the final state instead of fading every selected choice in when the gui opens.
        for (int i = 0; i < size; i++) {
            animProgress[i] = this.selected.contains(this.choices.get(i)) ? 1 : 0;
        }
    }
    
    public SequencedSet<T> get() {
        // A copy, so the setting this widget feeds doesn't end up aliasing our working set.
        return new LinkedHashSet<>(selected);
    }
    
    public void set(SequencedSet<T> value) {
        selected.clear();
        selected.addAll(value);
    }
    
    @Override
    protected void onCalculateSize() {
        double pad = pad();
        double spacing = GuiConstants.scale(this.spacing);
        double buttonPad = GuiConstants.scale(buttonPadding);
        double textHeight = GuiConstants.textHeight();
        double maxWidth = GuiConstants.scale(this.maxWidth);
        
        double rowWidth = 0;
        double rowY = 0;
        double widestRow = 0;
        
        for (int i = 0; i < choices.size(); i++) {
            double textWidth = GuiConstants.textWidth(namer.display(choices.get(i)));
            double totalWidth = buttonPad + textWidth + buttonPad;
            double totalHeight = buttonPad + textHeight + buttonPad;
            
            // Never wrap the first choice of a row, one wider than maxWidth still has to go somewhere.
            if (rowWidth > 0 && rowWidth + spacing + totalWidth > maxWidth) {
                widestRow = Math.max(widestRow, rowWidth);
                
                rowWidth = 0;
                rowY += totalHeight + spacing;
            }
            
            if (rowWidth > 0) {
                rowWidth += spacing;
            }
            
            this.choiceX[i] = pad + rowWidth;
            this.choiceY[i] = pad + rowY;
            this.choiceWidth[i] = totalWidth;
            this.choiceHeight[i] = totalHeight;
            
            rowWidth += totalWidth;
        }
        
        widestRow = Math.max(widestRow, rowWidth);
        
        double totalHeight = pad + rowY + (choices.isEmpty() ? 0 : choiceHeight[choices.size() - 1]) + pad;
        
        width = pad + widestRow + pad;
        height = totalHeight;
    }
    
    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        renderBackground(false, mouseOver);
        
        double textHeight = GuiConstants.textHeight();
        double buttonPad = GuiConstants.scale(buttonPadding);
        
        for (int i = 0; i < choices.size(); i++) {
            T choice = choices.get(i);
            
            animProgress[i] += (selected.contains(choice) ? 1 : -1) * delta * 14;
            animProgress[i] = MathHelper.clamp(animProgress[i], 0, 1);
            
            double choiceX = x + this.choiceX[i];
            double choiceY = y + this.choiceY[i];
            double choiceWidth = this.choiceWidth[i];
            double choiceHeight = this.choiceHeight[i];
            
            // Background
            Color bgColor = getBackgroundColor(i);
            rect(choiceX, choiceY, choiceWidth, choiceHeight, bgColor);
            
            // Border (subtle highlight for hovered)
            if (i == hovered) {
                rect(choiceX, choiceY, choiceWidth, choiceHeight, GuiConstants.TEXT_HIGHLIGHT);
            }
            
            // Text
            double textX = choiceX + buttonPad;
            double textY = choiceY + buttonPad;
            text(namer.display(choice), textX, textY, fade(animProgress[i]));
        }
    }
    
    private Color getBackgroundColor(int index) {
        boolean isSelected = selected.contains(choices.get(index));
        boolean isHovered = index == hovered;
        
        if (isSelected) {
            if (isHovered) {
                return GuiConstants.ACCENT;
            }
            return GuiConstants.ACCENT;
        } else {
            if (isHovered) {
                return GuiConstants.BACKGROUND.get(false, true);
            }
            return GuiConstants.BACKGROUND.get();
        }
    }
    
    private Color fade(double progress) {
        Color from = GuiConstants.TEXT_SECONDARY;
        Color to = GuiConstants.TEXT;
        
        return textColor.set(
            (int) MathHelper.lerp(progress, from.r, to.r),
            (int) MathHelper.lerp(progress, from.g, to.g),
            (int) MathHelper.lerp(progress, from.b, to.b),
            (int) MathHelper.lerp(progress, from.a, to.a)
        );
    }
    
    // Events
    
    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        hovered = -1;
        
        if (!mouseOver) {
            return;
        }
        
        for (int i = 0; i < choices.size(); i++) {
            double choiceX = x + this.choiceX[i];
            double choiceY = y + this.choiceY[i];
            
            if (mouseX >= choiceX && mouseX <= choiceX + choiceWidth[i] &&
                mouseY >= choiceY && mouseY <= choiceY + choiceHeight[i]) {
                hovered = i;
                break;
            }
        }
    }
    
    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        if (hovered != -1 && (click.button() == GLFW_MOUSE_BUTTON_LEFT || click.button() == GLFW_MOUSE_BUTTON_RIGHT)) {
            pressed = hovered;
        }
        
        return pressed != -1;
    }
    
    @Override
    public boolean onMouseReleased(Click click) {
        if (pressed == -1) {
            return false;
        }
        
        if (pressed == hovered) {
            T choice = choices.get(pressed);
            
            if (!selected.remove(choice)) {
                selected.add(choice);
            }
            
            if (action != null) {
                action.run();
            }
        }
        
        pressed = -1;
        
        return false;
    }
    
}