/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.misc.Range;

public abstract class WRangeSlider extends WWidget {
    
    protected Range value;
    protected final int min, max;
    
    protected double handleMinX, handleMaxX;
    protected boolean draggingMin, draggingMax;
    protected double valueMinAtDragStart, valueMaxAtDragStart;
    
    protected double scrollHandleMinX, scrollHandleMinY, scrollHandleMinH;
    protected double scrollHandleMaxX, scrollHandleMaxY, scrollHandleMaxH;
    protected boolean scrollHandleMinMouseOver, scrollHandleMaxMouseOver;
    
    protected boolean handleMinMouseOver, handleMaxMouseOver;
    
    public Runnable action;
    public Runnable actionOnRelease;
    
    public WRangeSlider(Range value, int min, int max) {
        this.value = value != null ? validateRange(value, min, max) : Range.of(min, max);
        this.min = min;
        this.max = max;
    }
    
    private Range validateRange(Range range, int min, int max) {
        return Range.of(
            Math.max(min, Math.min(range.min, max)),
            Math.max(min, Math.min(range.max, max))
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
    public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
        if (used) {
            return false;
        }
        
        double s = handleSize();
        double s2 = s / 2;
        double trackWidth = width - s;
        double trackX = x + s2;
        
        boolean mouseOverX = mouseX >= trackX && mouseX <= trackX + trackWidth;
        boolean mouseOverY = mouseY >= y && mouseY <= y + height;
        mouseOver = mouseOverX && mouseOverY;
        
        if (mouseOver) {
            handleMinX = calculateMinHandleX();
            handleMaxX = calculateMaxHandleX();
            
            boolean isOverMin = isMouseOverHandle(mouseX, mouseY, handleMinX, y, s);
            boolean isOverMax = isMouseOverHandle(mouseX, mouseY, handleMaxX, y, s);
            
            if (isOverMin && isOverMax) {
                double mouseDeltaX = mouseX - (handleMinX + handleMaxX) / 2;
                
                if (mouseDeltaX > 0) {
                    isOverMin = false;
                    if (value.max >= max) {
                        isOverMin = true;
                        isOverMax = false;
                    }
                } else {
                    isOverMax = false;
                    if (value.min <= min) {
                        isOverMin = false;
                        isOverMax = true;
                    }
                }
            }
            
            if (isOverMin) {
                draggingMin = true;
                valueMinAtDragStart = value.min;
                return true;
            }
            
            if (isOverMax) {
                draggingMax = true;
                valueMaxAtDragStart = value.max;
                return true;
            }
            
            double percent = (mouseX - trackX) / trackWidth;
            int newValue = (int) Math.round(min + percent * (max - min));
            newValue = Math.max(min, Math.min(newValue, max));
            
            if (Math.abs(newValue - value.min) <= Math.abs(newValue - value.max)) {
                setValue(Range.of(Math.min(newValue, value.max), value.max));
                draggingMin = true;
                valueMinAtDragStart = value.min;
            } else {
                setValue(Range.of(value.min, Math.max(newValue, value.min)));
                draggingMax = true;
                valueMaxAtDragStart = value.max;
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
        
        handleMinX = calculateMinHandleX();
        handleMaxX = calculateMaxHandleX();
        
        boolean isOverMin = isMouseOverHandle(mouseX, mouseY, handleMinX, y, s);
        boolean isOverMax = isMouseOverHandle(mouseX, mouseY, handleMaxX, y, s);
        
        if (isOverMin && isOverMax) {
            double mouseDeltaX = mouseX - (handleMinX + handleMaxX) / 2;
            
            if (mouseDeltaX > 0) {
                isOverMin = false;
                if (value.max >= max) {
                    isOverMin = true;
                    isOverMax = false;
                }
            } else {
                isOverMax = false;
                if (value.min <= min) {
                    isOverMin = false;
                    isOverMax = true;
                }
            }
        }
        
        handleMinMouseOver = isOverMin;
        handleMaxMouseOver = isOverMax;
        
        scrollHandleMinX = handleMinX - s / 2;
        scrollHandleMinY = y;
        scrollHandleMinH = s;
        
        scrollHandleMaxX = handleMaxX - s / 2;
        scrollHandleMaxY = y;
        scrollHandleMaxH = s;
        
        scrollHandleMinMouseOver = isOverMin;
        scrollHandleMaxMouseOver = isOverMax;
        
        if (draggingMin || draggingMax) {
            double percent = Math.max(0, Math.min(1, (mouseX - trackX) / trackWidth));
            int newValue = (int) Math.round(min + percent * (max - min));
            newValue = Math.max(min, Math.min(newValue, max));
            
            if (draggingMin) {
                setValue(Range.of(Math.min(newValue, value.max), value.max));
            } else {
                setValue(Range.of(value.min, Math.max(newValue, value.min)));
            }
        }
    }
    
    @Override
    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (draggingMin || draggingMax) {
            if ((draggingMin && value.min != valueMinAtDragStart) ||
                (draggingMax && value.max != valueMaxAtDragStart)) {
                if (actionOnRelease != null) {
                    actionOnRelease.run();
                }
            }
            draggingMin = false;
            draggingMax = false;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean onMouseScrolled(double amount) {
        if (scrollHandleMinMouseOver) {
            int newMin = Math.max(min, Math.min(value.min + (int) amount, value.max));
            if (newMin != value.min) {
                setValue(Range.of(newMin, value.max));
                if (action != null) {
                    action.run();
                }
            }
            return true;
        }
        
        if (scrollHandleMaxMouseOver) {
            int newMax = Math.max(value.min, Math.min(value.max + (int) amount, max));
            if (newMax != value.max) {
                setValue(Range.of(value.min, newMax));
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
    
    protected double calculateMinHandleX() {
        double s = handleSize();
        double trackWidth = width - s;
        double trackX = x + s / 2;
        double minPercent = (double) (value.min - min) / (max - min);
        return trackX + Math.max(0, Math.min(minPercent, 1)) * trackWidth;
    }
    
    protected double calculateMaxHandleX() {
        double s = handleSize();
        double trackWidth = width - s;
        double trackX = x + s / 2;
        double maxPercent = (double) (value.max - min) / (max - min);
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
        
        return mouseX >= handleLeft &&
            mouseX <= handleRight &&
            mouseY >= handleY &&
            mouseY <= handleBottom;
    }
    
}
