/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.impl;

import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import net.minecraft.client.gui.screen.Screen;

public class ModulesTab extends Tab {

    public ModulesTab() {
        super("Modules");
    }

    @Override
    public TabScreen createScreen() {
        return new ModulesScreen();
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ModulesScreen;
    }

}
