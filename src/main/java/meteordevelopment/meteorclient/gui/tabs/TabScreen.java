/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs;

import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;

public abstract class TabScreen extends WidgetScreen {
    
    public final Tab tab;
    
    public TabScreen(Tab tab) {
        super(tab.name);
        
        super.add(new WTopBar()).top().centerX();
        
        this.tab = tab;
    }
    
}
