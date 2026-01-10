/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.misc.Range;
import net.minecraft.client.gui.Click;

public abstract class WRangeSlider extends WWidget {
    
    protected Range value;
    protected final int min, max;
    
    protected double handleFromX, handleToX;
    protected boolean draggingFrom, draggingTo;
    protected double valueFromAtDragStart, valueToAtDragStart;
    
    protected double scrollHandleFromX, scrollHandleFromY, scrollHandleFromH;
    protected double scrollHandleToX, scrollHandleToY, scrollHandleToH;
    protected boolean scrollHandleFromMouseOver, scrollHandleToMouseOver;
    
    protected boolean handleFromMouseOver, handleToMouseOver;
    
    public Runnable action;
    public Runnable actionOnRelease;
    
    public WRangeSlider(Range value, int min, int max) {
        this.value = value != null ? validateRange(value, min, max) : Range.of(min, max);
        this.min = min;
        this.max = max;
    }
    
    private Range validateRange(Range range, int min, int max) {
        return Range.of(
            Math.max(min, Math.min(range.from, max)),
            Math.max(min, Math.min(range.to, max))
        );
    }
    
    protected double handleSize() {
        return theme.textHeight();
    }
    
    @Override
    protected void onCalculateSize() {
        width = 200;
        height = handleSize();
    }
    
    @Override
    public boolean onMouseClicked(Click click, boolean used) {
        if (used) {
            return false;
        }
        
        double s = handleSize();
        double s2 = s / 2;
        double trackWidth = width - s;
        double trackX = x + s2;
        
        boolean mouseOverX = click.x() >= trackX && click.x() <= trackX + trackWidth;
        boolean mouseOverY = click.y() >= y && click.y() <= y + height;
        mouseOver = mouseOverX && mouseOverY;
        
        if (mouseOver) {
            handleFromX = calculateFromHandleX();
            handleToX = calculateToHandleX();
            
            boolean isOverMin = isMouseOverHandle(click.x(), click.y(), handleFromX, y, s);
            boolean isOverMax = isMouseOverHandle(click.x(), click.y(), handleToX, y, s);
            
            if (isOverMin && isOverMax) {
                double mouseDeltaX = click.x() - (handleFromX + handleToX) / 2;
                
                if (mouseDeltaX > 0) {
                    isOverMin = false;
                    if (value.to >= max) {
                        isOverMin = true;
                        isOverMax = false;
                    }
                } else {
                    isOverMax = false;
                    if (value.from <= min) {
                        isOverMin = false;
                        isOverMax = true;
                    }
                }
            }
            
            if (isOverMin) {
                draggingFrom = true;
                valueFromAtDragStart = value.from;
                return true;
            }
            
            if (isOverMax) {
                draggingTo = true;
                valueToAtDragStart = value.to;
                return true;
            }
            
            double percent = (click.x() - trackX) / trackWidth;
            int newValue = (int) Math.round(min + percent * (max - min));
            newValue = Math.max(min, Math.min(newValue, max));
            
            if (Math.abs(newValue - value.from) <= Math.abs(newValue - value.to)) {
                setValue(Range.of(Math.min(newValue, value.to), value.to));
                draggingFrom = true;
                valueFromAtDragStart = value.from;
            } else {
                setValue(Range.of(value.from, Math.max(newValue, value.from)));
                draggingTo = true;
                valueToAtDragStart = value.to;
            }
            return true;
        }
        return false;
    }
    
    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        double s = handleSize();
        double s2 = s / 2;
        double trackWidth = width - s;
        double trackX = x + s2;
        
        boolean mouseOverX = mouseX >= trackX && mouseX <= trackX + trackWidth;
        boolean mouseOverY = mouseY >= y && mouseY <= y + height;
        mouseOver = mouseOverX && mouseOverY;
        
        handleFromX = calculateFromHandleX();
        handleToX = calculateToHandleX();
        
        boolean isOverMin = isMouseOverHandle(mouseX, mouseY, handleFromX, y, s);
        boolean isOverMax = isMouseOverHandle(mouseX, mouseY, handleToX, y, s);
        
        if (isOverMin && isOverMax) {
            double mouseDeltaX = mouseX - (handleFromX + handleToX) / 2;
            
            if (mouseDeltaX > 0) {
                isOverMin = false;
                if (value.to >= max) {
                    isOverMin = true;
                    isOverMax = false;
                }
            } else {
                isOverMax = false;
                if (value.from <= min) {
                    isOverMin = false;
                    isOverMax = true;
                }
            }
        }
        
        handleFromMouseOver = isOverMin;
        handleToMouseOver = isOverMax;
        
        scrollHandleFromX = handleFromX - s / 2;
        scrollHandleFromY = y;
        scrollHandleFromH = s;
        
        scrollHandleToX = handleToX - s / 2;
        scrollHandleToY = y;
        scrollHandleToH = s;
        
        scrollHandleFromMouseOver = isOverMin;
        scrollHandleToMouseOver = isOverMax;
        
        if (draggingFrom || draggingTo) {
            double percent = Math.max(0, Math.min(1, (mouseX - trackX) / trackWidth));
            int newValue = (int) Math.round(min + percent * (max - min));
            newValue = Math.max(min, Math.min(newValue, max));
            
            if (draggingFrom) {
                setValue(Range.of(Math.min(newValue, value.to), value.to));
            } else {
                setValue(Range.of(value.from, Math.max(newValue, value.from)));
            }
        }
    }
    
    @Override
    public boolean onMouseReleased(Click click) {
        if (draggingFrom || draggingTo) {
            if ((draggingFrom && value.from != valueFromAtDragStart) ||
                (draggingTo && value.to != valueToAtDragStart)) {
                if (actionOnRelease != null) {
                    actionOnRelease.run();
                }
            }
            draggingFrom = false;
            draggingTo = false;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean onMouseScrolled(double amount) {
        if (scrollHandleFromMouseOver) {
            int newFrom = Math.max(min, Math.min(value.from + (int) amount, value.to));
            if (newFrom != value.from) {
                setValue(Range.of(newFrom, value.to));
                if (action != null) {
                    action.run();
                }
            }
            return true;
        }
        
        if (scrollHandleToMouseOver) {
            int newTo = Math.max(value.from, Math.min(value.to + (int) amount, max));
            if (newTo != value.to) {
                setValue(Range.of(value.from, newTo));
                if (action != null) {
                    action.run();
                }
            }
            return true;
        }
        return false;
    }
    
    protected void setValue(Range newValue) {
        // Ensure values are within valid range
        Range validatedValue = validateRange(newValue, min, max);
        if (!validatedValue.equals(value)) {
            value = validatedValue;
            if (action != null) {
                action.run();
            }
        }
    }
    
    protected double calculateFromHandleX() {
        double s = handleSize();
        double trackWidth = width - s;
        double trackX = x + s / 2;
        double minPercent = (double) (value.from - min) / (max - min);
        return trackX + Math.max(0, Math.min(minPercent, 1)) * trackWidth;
    }
    
    protected double calculateToHandleX() {
        double s = handleSize();
        double trackWidth = width - s;
        double trackX = x + s / 2;
        double maxPercent = (double) (value.to - min) / (max - min);
        return trackX + Math.max(0, Math.min(maxPercent, 1)) * trackWidth;
    }
    
    protected double calculateTrackWidth() {
        return Math.max(0, width - handleSize());
    }
    
    protected double calculateTrackX() {
        return x + handleSize() / 2;
    }
    
    protected double calculateTrackY() {
        return y + handleSize() / 2 - 2;
    }
    
    public Range get() {
        return value;
    }
    
    public void set(Range value) {
        this.value = validateRange(value, min, max);
    }
    
    private boolean isMouseOverHandle(double mouseX, double mouseY, double handleX, double handleY, double handleSize) {
        double handleLeft = handleX - handleSize / 2;
        double handleRight = handleX + handleSize / 2;
        double handleBottom = handleY + handleSize;
        
        return mouseX >= handleLeft
            && mouseX <= handleRight
            && mouseY >= handleY
            && mouseY <= handleBottom;
    }
    
}
