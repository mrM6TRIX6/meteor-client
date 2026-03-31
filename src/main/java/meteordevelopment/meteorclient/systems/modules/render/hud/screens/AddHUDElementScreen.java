/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElementInfo;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDGroup;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AddHUDElementScreen extends WindowScreen {
    
    private final int x, y;
    private final WTextBox searchBar;
    
    private Object firstObject;
    
    public AddHUDElementScreen(GuiTheme theme, int x, int y) {
        super(theme, "Add Hud element");
        
        this.x = x;
        this.y = y;
        
        searchBar = theme.textBox("");
        searchBar.action = () -> {
            clear();
            initWidgets();
        };
        
        enterAction = () -> runObject(firstObject);
    }
    
    @Override
    public void initWidgets() {
        firstObject = null;
        
        // Search bar
        add(searchBar).expandX();
        searchBar.setFocused(true);
        
        // Group infos
        HUD hud = HUD.get();
        Map<HUDGroup, List<Item>> grouped = new HashMap<>();
        
        for (HUDElementInfo<?> info : hud.infos.values()) {
            if (info.hasPresets() && !searchBar.get().isEmpty()) {
                for (HUDElementInfo<?>.Preset preset : info.presets) {
                    String title = info.name + "  -  " + preset.name;
                    if (Utils.searchTextDefault(title, searchBar.get(), false)) {
                        grouped.computeIfAbsent(info.group, HUDGroup -> new ArrayList<>()).add(new Item(title, info.description, preset));
                    }
                }
            } else if (Utils.searchTextDefault(info.name, searchBar.get(), false)) {
                grouped.computeIfAbsent(info.group, HUDGroup -> new ArrayList<>()).add(new Item(info.name, info.description, info));
            }
        }
        
        // Create widgets
        for (HUDGroup group : grouped.keySet()) {
            WSection section = add(theme.section(group.title())).expandX().widget();
            
            for (Item item : grouped.get(group)) {
                WHorizontalList l = section.add(theme.horizontalList()).expandX().widget();
                
                WLabel title = l.add(theme.label(item.title)).widget();
                title.tooltip = item.description;
                
                if (item.object instanceof HUDElementInfo<?>.Preset preset) {
                    WPlus add = l.add(theme.plus()).expandCellX().right().widget();
                    add.action = () -> runObject(preset);
                    
                    if (firstObject == null) {
                        firstObject = preset;
                    }
                } else {
                    HUDElementInfo<?> info = (HUDElementInfo<?>) item.object;
                    
                    if (info.hasPresets()) {
                        WButton open = l.add(theme.button(" > ")).expandCellX().right().widget();
                        open.action = () -> runObject(info);
                    } else {
                        WPlus add = l.add(theme.plus()).expandCellX().right().widget();
                        add.action = () -> runObject(info);
                    }
                    
                    if (firstObject == null) {
                        firstObject = info;
                    }
                }
            }
        }
    }
    
    private void runObject(Object object) {
        if (object == null) {
            return;
        }
        if (object instanceof HUDElementInfo<?>.Preset preset) {
            HUD.get().add(preset, x, y);
            close();
        } else {
            HUDElementInfo<?> info = (HUDElementInfo<?>) object;
            
            if (info.hasPresets()) {
                HUDElementPresetsScreen screen = new HUDElementPresetsScreen(theme, info, x, y);
                screen.parent = parent;
                
                mc.setScreen(screen);
            } else {
                HUD.get().add(info, x, y);
                close();
            }
        }
    }
    
    @Override
    protected void onRenderBefore(DrawContext drawContext, float delta) {
        HUDEditorScreen.renderElements(drawContext);
    }
    
    private record Item(String title, String description, Object object) {}
    
}
