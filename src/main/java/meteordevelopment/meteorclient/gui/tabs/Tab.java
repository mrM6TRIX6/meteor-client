/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs;

import net.minecraft.client.gui.screen.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class Tab {
    
    public final String name;
    
    public Tab(String name) {
        this.name = name;
    }
    
    public void openScreen() {
        mc.setScreen(this.createScreen());
    }
    
    public abstract TabScreen createScreen();
    
    public abstract boolean isScreen(Screen screen);
    
}
