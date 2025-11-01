/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;

public abstract class TabScreen extends WidgetScreen {
    
    public final Tab tab;
    
    public TabScreen(GuiTheme theme, Tab tab) {
        super(theme, tab.name);
        
        super.add(theme.topBar()).top().centerX();
        
        this.tab = tab;
    }
    
}
