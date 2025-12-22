/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WRangeSlider;
import meteordevelopment.meteorclient.utils.misc.Range;

public class WMeteorRangeSlider extends WRangeSlider implements MeteorWidget {
    
    public WMeteorRangeSlider(Range value, int min, int max) {
        super(value, min, max);
    }
    
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();
        
        handleFromX = calculateFromHandleX();
        handleToX = calculateToHandleX();
        double trackWidth = calculateTrackWidth();
        double trackX = calculateTrackX();
        double trackY = calculateTrackY();
        
        renderTrack(renderer, theme, trackX, trackY, trackWidth);
        
        renderActiveRange(renderer, theme, trackX, trackY, handleFromX, handleToX);
        
        renderHandle(renderer, theme, handleFromX, draggingFrom || handleFromMouseOver, true);
        renderHandle(renderer, theme, handleToX, draggingTo || handleToMouseOver, false);
    }
    
    private void renderTrack(GuiRenderer renderer, MeteorGuiTheme theme, double trackX, double trackY, double trackWidth) {
        double trackHeight = theme.scale(3);
        renderer.quad(trackX, trackY, trackWidth, trackHeight, theme.sliderRight.get());
    }
    
    private void renderActiveRange(GuiRenderer renderer, MeteorGuiTheme theme, double trackX, double trackY, double minX, double maxX) {
        double trackHeight = theme.scale(3);
        renderer.quad(minX, trackY, maxX - minX, trackHeight, theme.sliderLeft.get());
    }
    
    private void renderHandle(GuiRenderer renderer, MeteorGuiTheme theme, double handleX, boolean active, boolean isMinHandle) {
        double s = handleSize();
        boolean dragging = isMinHandle ? draggingFrom : draggingTo;
        boolean mouseOver = isMinHandle ? handleFromMouseOver : handleToMouseOver;
        
        renderer.quad(handleX - s / 2, y, s, s, GuiRenderer.CIRCLE, theme.sliderHandle.get(dragging, mouseOver || active));
    }
    
}
