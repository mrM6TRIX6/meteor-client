/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.impl;

import meteordevelopment.meteorclient.config.Config;
import meteordevelopment.meteorclient.config.ConfigManager;
import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

public class ConfigsTab extends Tab {
    
    public ConfigsTab() {
        super("Configs");
    }
    
    @Override
    public TabScreen createScreen() {
        return new ConfigsScreen(this);
    }
    
    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ConfigsScreen;
    }
    
    private static class ConfigsScreen extends WindowTabScreen {
        
        public ConfigsScreen(Tab tab) {
            super(tab);
        }
        
        @Override
        public void initWidgets() {
            WTable table = add(new WTable()).expandX().minWidth(400).widget();
            initTable(table);
            
            add(new WHorizontalSeparator()).expandX();
            
            WHorizontalList list = add(new WHorizontalList()).expandX().widget();
            
            // Create
            WButton createBtn = list.add(new WButton("Create")).expandX().widget();
            createBtn.action = () -> mc.setScreen(new EditConfigScreen(null, this::reload));
            
            // Clear
            WButton clearBtn = list.add(new WButton("Clear")).expandX().widget();
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
                table.add(new WLabel(config.name.get())).expandCellX();
                
                WButton save = table.add(new WButton("Save")).widget();
                save.action = config::save;
                
                WButton load = table.add(new WButton("Load")).widget();
                load.action = config::load;
                
                WButton edit = table.add(new WButton(GuiConstants.EDIT)).widget();
                edit.action = () -> mc.setScreen(new EditConfigScreen(config, this::reload));
                
                WMinus remove = table.add(new WMinus()).widget();
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
        
        public EditConfigScreen(Config config, Runnable action) {
            super(config == null ? "New Config" : "Edit Config");
            
            this.isNew = config == null;
            this.config = isNew ? new Config() : config;
            this.action = action;
        }
        
        @Override
        public void initWidgets() {
            settingsContainer = add(new WVerticalList()).expandX().minWidth(400).widget();
            settingsContainer.add(DefaultSettingsWidgetFactory.settings(config.settings)).expandX();
            
            add(new WHorizontalSeparator()).expandX();
            
            WButton save = add(new WButton(isNew ? "Create" : "Save")).expandX().widget();
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
            config.settings.tick(settingsContainer);
        }
        
        @Override
        protected void onClosed() {
            if (action != null) {
                action.run();
            }
        }
        
    }
    
}
