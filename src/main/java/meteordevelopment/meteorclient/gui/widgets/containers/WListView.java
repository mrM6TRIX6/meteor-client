/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.containers;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.renderer.RenderUtils;

public class WListView extends WView {
    
    public double heightFactor = 0.45;
    public double minVisibleHeight = 96;

    public WListView() {}

    public WListView(double heightFactor) {
        this.heightFactor = heightFactor;
    }

    @Override
    public void init() {
        // WView defaults to the whole window height, which is the one thing this view must not do
    }

    @Override
    protected void onCalculateSize() {
        maxHeight = Math.max(GuiConstants.scale(minVisibleHeight), RenderUtils.getWindowHeight() * heightFactor);

        super.onCalculateSize();
    }

}
