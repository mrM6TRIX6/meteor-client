/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.impl;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.clientsettings.ClientSettings;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import net.minecraft.client.gui.screen.Screen;

public class ClientSettingsTab extends Tab {
    
    public ClientSettingsTab() {
        super("Client Settings");
    }
    
    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new ClientSettingsScreen(theme, this);
    }
    
    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ClientSettingsScreen;
    }
    
    public static class ClientSettingsScreen extends WindowTabScreen {
        
        private final Settings settings;
        
        public ClientSettingsScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
            
            settings = ClientSettings.get().settings;
            settings.onActivated();
        }
        
        @Override
        public void initWidgets() {
            add(theme.settings(settings)).expandX();
        }
        
        @Override
        public void tick() {
            super.tick();
            
            settings.tick(window, theme);
        }
        
        @Override
        public boolean toClipboard() {
            return JsonUtils.toClipboard(ClientSettings.get());
        }
        
        @Override
        public boolean fromClipboard() {
            return JsonUtils.fromClipboard(ClientSettings.get());
        }
        
    }
    
}
