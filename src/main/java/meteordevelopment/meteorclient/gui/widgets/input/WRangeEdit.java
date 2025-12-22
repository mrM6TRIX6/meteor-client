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
    
    private WTextBox fromTextBox;
    private WTextBox toTextBox;
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
            Math.max(min, Math.min(range.from, max)),
            Math.max(min, Math.min(range.to, max))
        );
    }
    
    @Override
    public void init() {
        fromTextBox = add(theme.textBox(Integer.toString(value.from), this::filter)).minWidth(75).widget();
        add(theme.label("-")).centerY();
        toTextBox = add(theme.textBox(Integer.toString(value.to), this::filter)).minWidth(75).widget();
        
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
        fromTextBox.actionOnUnfocused = () -> {
            Range lastValue = value;
            
            if (fromTextBox.get().isEmpty()) {
                fromTextBox.set(Integer.toString(value.from));
                return;
            }
            
            try {
                int newFrom = Integer.parseInt(fromTextBox.get());
                newFrom = Math.max(min, Math.min(newFrom, max));
                
                if (newFrom > value.to) {
                    fromTextBox.set(Integer.toString(value.from));
                    return;
                }
                
                value = Range.of(newFrom, value.to);
                updateSliderFromValue();
            } catch (NumberFormatException ignored) {
                fromTextBox.set(Integer.toString(value.from));
            }
            
            if (!value.equals(lastValue) && action != null) {
                action.run();
            }
        };
        
        toTextBox.actionOnUnfocused = () -> {
            Range lastValue = value;
            
            if (toTextBox.get().isEmpty()) {
                toTextBox.set(Integer.toString(value.to));
                return;
            }
            
            try {
                int newTo = Integer.parseInt(toTextBox.get());
                newTo = Math.max(min, Math.min(newTo, max));
                
                if (newTo < value.from) {
                    toTextBox.set(Integer.toString(value.to));
                    return;
                }
                
                value = Range.of(value.from, newTo);
                updateSliderFromValue();
            } catch (NumberFormatException ignored) {
                toTextBox.set(Integer.toString(value.to));
            }
            
            if (!value.equals(lastValue) && action != null) {
                action.run();
            }
        };
        
        fromTextBox.action = null;
        toTextBox.action = null;
    }
    
    private void setupSliderActions() {
        rangeSlider.action = () -> {
            Range newValue = rangeSlider.get();
            if (!newValue.equals(value)) {
                value = newValue;
                fromTextBox.set(Integer.toString(value.from));
                toTextBox.set(Integer.toString(value.to));
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
        fromTextBox.set(Integer.toString(this.value.from));
        toTextBox.set(Integer.toString(this.value.to));
        updateSliderFromValue();
    }
    
}
