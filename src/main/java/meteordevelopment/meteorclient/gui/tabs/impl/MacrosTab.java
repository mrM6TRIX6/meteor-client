/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.impl;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.screens.EditSystemScreen;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.macros.Macro;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import net.minecraft.client.gui.screen.Screen;

public class MacrosTab extends Tab {
    
    public MacrosTab() {
        super("Macros");
    }
    
    @Override
    public TabScreen createScreen() {
        return new MacrosScreen(this);
    }
    
    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof MacrosScreen;
    }
    
    private static class MacrosScreen extends WindowTabScreen {
        
        public MacrosScreen(Tab tab) {
            super(tab);
        }
        
        @Override
        public void initWidgets() {
            WTable table = add(new WTable()).expandX().minWidth(400).widget();
            initTable(table);
            
            add(new WHorizontalSeparator()).expandX();
            
            WHorizontalList list = add(new WHorizontalList()).expandX().widget();
            
            // Create
            WButton create = list.add(new WButton("Create")).expandX().widget();
            create.action = () -> mc.setScreen(new EditMacroScreen(null, this::reload));
            
            // Clear
            WButton clearBtn = list.add(new WButton("Clear")).expandX().widget();
            clearBtn.action = () -> {
                Macros.get().clear();
                reload();
            };
        }
        
        private void initTable(WTable table) {
            table.clear();
            if (Macros.get().isEmpty()) {
                return;
            }
            
            for (Macro macro : Macros.get()) {
                table.add(new WLabel(macro.name.get() + " (" + macro.keybind.get() + ")"));
                
                WButton edit = table.add(new WButton(GuiConstants.EDIT)).expandCellX().right().widget();
                edit.action = () -> mc.setScreen(new EditMacroScreen(macro, this::reload));
                
                WMinus remove = table.add(new WMinus()).widget();
                remove.action = () -> {
                    Macros.get().remove(macro);
                    reload();
                };
                
                table.row();
            }
        }
        
        @Override
        public boolean toClipboard() {
            return JsonUtils.toClipboard(Macros.get());
        }
        
        @Override
        public boolean fromClipboard() {
            return JsonUtils.fromClipboard(Macros.get());
        }
        
    }
    
    private static class EditMacroScreen extends EditSystemScreen<Macro> {
        
        public EditMacroScreen(Macro value, Runnable reload) {
            super(value, reload);
        }
        
        @Override
        public Macro create() {
            return new Macro();
        }
        
        @Override
        public boolean save() {
            if (value.name.get().isBlank() || value.messages.get().isEmpty()) {
                return false;
            }
            
            if (isNew) {
                for (Macro m : Macros.get()) {
                    if (value.equals(m)) {
                        return false;
                    }
                }
            }
            
            if (isNew) {
                Macros.get().add(value);
            } else {
                Macros.get().save();
            }
            
            return true;
        }
        
        @Override
        public Settings getSettings() {
            return value.settings;
        }
        
    }
    
}
