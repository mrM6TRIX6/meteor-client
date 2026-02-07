/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.impl;

import meteordevelopment.meteorclient.config.ConfigManager;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ConfigsTab extends Tab {
    
    public ConfigsTab() {
        super("Configs");
    }
    
    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new ConfigsScreen(theme, this);
    }
    
    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ConfigsScreen;
    }
    
    private static class ConfigsScreen extends WindowTabScreen {
        
        public ConfigsScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }
        
        @Override
        public void initWidgets() {
            WTable table = add(theme.table()).expandX().minWidth(400).widget();
            initTable(table);
            
            add(theme.horizontalSeparator()).expandX();
            
            WHorizontalList list = add(theme.horizontalList()).expandX().widget();
            
            // Create
            WButton createBtn = list.add(theme.button("Create")).expandX().widget();
            createBtn.action = () -> mc.setScreen(new EditConfigScreen(theme, null, this::reload));
            
            // Clear
            WButton clearBtn = list.add(theme.button("Clear")).expandX().widget();
            clearBtn.action = () -> {
                ConfigManager.getAll().clear();
                reload();
            };
        }
        
        private void initTable(WTable table) {
            table.clear();
            if (ConfigManager.isEmpty()) {
                return;
            }
            
            for (Config config : ConfigManager.getAll()) {
                table.add(theme.label(config.name.get())).expandCellX();
                
                WButton save = table.add(theme.button("Save")).widget();
                save.action = config::save;
                
                WButton load = table.add(theme.button("Load")).widget();
                load.action = config::load;
                
                WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
                edit.action = () -> mc.setScreen(new EditConfigScreen(theme, config, this::reload));
                
                WMinus remove = table.add(theme.minus()).widget();
                remove.action = () -> {
                    ConfigManager.remove(config);
                    reload();
                };
                
                table.row();
            }
        }
        
    }
    
    private static class EditConfigScreen extends WindowScreen {
        
        private WContainer settingsContainer;
        private final Config config;
        private final boolean isNew;
        private final Runnable action;
        
        public EditConfigScreen(GuiTheme theme, Config config, Runnable action) {
            super(theme, config == null ? "New Config" : "Edit Config");
            
            this.isNew = config == null;
            this.config = isNew ? new Config() : config;
            this.action = action;
        }
        
        @Override
        public void initWidgets() {
            settingsContainer = add(theme.verticalList()).expandX().minWidth(400).widget();
            settingsContainer.add(theme.settings(config.settings)).expandX();
            
            add(theme.horizontalSeparator()).expandX();
            
            WButton save = add(theme.button(isNew ? "Create" : "Save")).expandX().widget();
            save.action = () -> {
                if (config.name.get().isEmpty()) {
                    return;
                }
                
                if (isNew) {
                    for (Config p : ConfigManager.getAll()) {
                        if (config.equals(p)) {
                            return;
                        }
                    }
                }
                
                List<String> valid = new ArrayList<>();
                for (String address : config.loadOnJoin.get()) {
                    if (Utils.resolveAddress(address)) {
                        valid.add(address);
                    }
                }
                
                config.loadOnJoin.set(valid);
                
                if (isNew) {
                    ConfigManager.add(config);
                } else {
                    config.save();
                }
                
                close();
            };
            
            enterAction = save.action;
        }
        
        @Override
        public void tick() {
            super.tick();
            config.settings.tick(settingsContainer, theme);
        }
        
        @Override
        protected void onClosed() {
            if (action != null) {
                action.run();
            }
        }
        
    }
    
}
