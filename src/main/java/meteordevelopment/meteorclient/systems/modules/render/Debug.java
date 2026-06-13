/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Debug extends Module {
    
    public Debug() {
        super(Category.RENDER, "Debug", "Various tools for developers.");
    }
    
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton reloadBtn = theme.button("Reload shaders");
        reloadBtn.action = MeteorRenderPipelines::reload;
        
        return reloadBtn;
    }
    
}
