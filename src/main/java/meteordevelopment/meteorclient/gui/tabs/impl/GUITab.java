/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import net.minecraft.client.gui.screen.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class GUITab extends Tab {
    
    public GUITab() {
        super("GUI");
    }
    
    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new GuiScreen(theme, this);
    }
    
    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof GuiScreen;
    }
    
    private static class GuiScreen extends WindowTabScreen {
        
        public GuiScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
            
            theme.settings.onActivated();
        }
        
        @Override
        public void initWidgets() {
            WTable table = add(theme.table()).expandX().widget();
            
            table.add(theme.label("Theme:"));
            // WDropdown<String> themeW = table.add(theme.dropdown(GuiThemes.getNames(), GuiThemes.get().name)).widget();
//            themeW.action = () -> {
//                GuiThemes.select(themeW.get());
//
//                mc.setScreen(null);
//                tab.openScreen(GuiThemes.get());
//            };
            
            WButton reset = add(theme.button("Reset GUI Layout")).widget();
            reset.action = theme::clearWindowConfigs;
            
            add(theme.settings(theme.settings)).expandX();
        }
        
        @Override
        public boolean toClipboard() {
            return JsonUtils.toClipboard(theme.name + " GUI Theme", theme.toJson());
        }
        
        @Override
        public boolean fromClipboard() {
            JsonObject clipboard = JsonUtils.fromClipboard(theme.toJson());
            
            if (clipboard != null) {
                theme.fromJson(clipboard);
                return true;
            }
            
            return false;
        }
        
    }
    
}
