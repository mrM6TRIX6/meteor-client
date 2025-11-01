/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.utils.misc.Range;

public class WRangeEdit extends WHorizontalList {
    
    private Range value;
    
    public final int min, max;
    private final int sliderMin, sliderMax;
    public boolean noSlider;
    
    public Runnable action;
    public Runnable actionOnRelease;
    
    private WTextBox minTextBox;
    private WTextBox maxTextBox;
    private WRangeSlider rangeSlider;
    
    public WRangeEdit(Range value, int min, int max, int sliderMin, int sliderMax, boolean noSlider) {
        this.value = value != null ? validateRange(value, min, max) : Range.of(min, max);
        this.min = min;
        this.max = max;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
        this.noSlider = noSlider;
    }
    
    private Range validateRange(Range range, int min, int max) {
        return Range.of(
            Math.max(min, Math.min(range.min, max)),
            Math.max(min, Math.min(range.max, max))
        );
    }
    
    @Override
    public void init() {
        minTextBox = add(theme.textBox(Integer.toString(value.min), this::filter)).minWidth(75).widget();
        add(theme.label("-")).centerY();
        maxTextBox = add(theme.textBox(Integer.toString(value.max), this::filter)).minWidth(75).widget();
        
        if (!noSlider) {
            rangeSlider = add(theme.rangeSlider(value, sliderMin, sliderMax)).minWidth(200).centerY().expandX().widget();
            setupSliderActions();
        }
        setupTextBoxActions();
    }
    
    private boolean filter(String text, char c) {
        boolean good;
        boolean validate = true;
        
        if (c == '-' && text.isEmpty()) {
            good = true;
            validate = false;
        } else {
            good = Character.isDigit(c);
        }
        
        if (good && validate) {
            try {
                Integer.parseInt(text + c);
            } catch (NumberFormatException ignored) {
                good = false;
            }
        }
        return good;
    }
    
    private void setupTextBoxActions() {
        minTextBox.actionOnUnfocused = () -> {
            Range lastValue = value;
            
            if (minTextBox.get().isEmpty()) {
                minTextBox.set(Integer.toString(value.min));
                return;
            }
            
            try {
                int newMin = Integer.parseInt(minTextBox.get());
                newMin = Math.max(min, Math.min(newMin, max));
                
                if (newMin > value.max) {
                    minTextBox.set(Integer.toString(value.min));
                    return;
                }
                
                value = Range.of(newMin, value.max);
                updateSliderFromValue();
            } catch (NumberFormatException ignored) {
                minTextBox.set(Integer.toString(value.min));
            }
            
            if (!value.equals(lastValue) && action != null) {
                action.run();
            }
        };
        
        maxTextBox.actionOnUnfocused = () -> {
            Range lastValue = value;
            
            if (maxTextBox.get().isEmpty()) {
                maxTextBox.set(Integer.toString(value.max));
                return;
            }
            
            try {
                int newMax = Integer.parseInt(maxTextBox.get());
                newMax = Math.max(min, Math.min(newMax, max));
                
                if (newMax < value.min) {
                    maxTextBox.set(Integer.toString(value.max));
                    return;
                }
                
                value = Range.of(value.min, newMax);
                updateSliderFromValue();
            } catch (NumberFormatException ignored) {
                maxTextBox.set(Integer.toString(value.max));
            }
            
            if (!value.equals(lastValue) && action != null) {
                action.run();
            }
        };
        
        minTextBox.action = null;
        maxTextBox.action = null;
    }
    
    private void setupSliderActions() {
        rangeSlider.action = () -> {
            Range newValue = rangeSlider.get();
            if (!newValue.equals(value)) {
                value = newValue;
                minTextBox.set(Integer.toString(value.min));
                maxTextBox.set(Integer.toString(value.max));
                if (action != null) {
                    action.run();
                }
            }
        };
        
        rangeSlider.actionOnRelease = () -> {
            if (actionOnRelease != null) {
                actionOnRelease.run();
            }
        };
    }
    
    private void updateSliderFromValue() {
        if (rangeSlider != null) {
            rangeSlider.set(value);
        }
    }
    
    public Range get() {
        return value;
    }
    
    public void set(Range value) {
        if (value == null) {
            return;
        }
        
        this.value = validateRange(value, min, max);
        minTextBox.set(Integer.toString(this.value.min));
        maxTextBox.set(Integer.toString(this.value.max));
        updateSliderFromValue();
    }
    
}
