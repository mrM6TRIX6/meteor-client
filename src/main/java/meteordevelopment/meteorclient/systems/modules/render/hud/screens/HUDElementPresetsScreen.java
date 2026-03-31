/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElementInfo;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;

public class HUDElementPresetsScreen extends WindowScreen {
    
    private final HUDElementInfo<?> info;
    private final int x, y;
    
    private final WTextBox searchBar;
    
    @Nullable
    private HUDElementInfo<?>.Preset firstPreset;
    
    public HUDElementPresetsScreen(GuiTheme theme, HUDElementInfo<?> info, int x, int y) {
        super(theme, "Select preset for " + info.name);
        
        this.info = info;
        this.x = x + 9;
        this.y = y;
        
        searchBar = theme.textBox("");
        searchBar.action = () -> {
            clear();
            initWidgets();
        };
        
        enterAction = () -> {
            if (firstPreset == null) {
                return;
            }
            HUD.get().add(firstPreset, x, y);
            close();
        };
    }
    
    @Override
    public void initWidgets() {
        firstPreset = null;
        
        // Search bar
        add(searchBar).expandX();
        searchBar.setFocused(true);
        
        // Presets
        for (HUDElementInfo<?>.Preset preset : info.presets) {
            if (!Utils.searchTextDefault(preset.name, searchBar.get(), false)) {
                continue;
            }
            
            WHorizontalList l = add(theme.horizontalList()).expandX().widget();
            
            l.add(theme.label(preset.name));
            
            WPlus add = l.add(theme.plus()).expandCellX().right().widget();
            add.action = () -> {
                HUD.get().add(preset, x, y);
                close();
            };
            
            if (firstPreset == null) {
                firstPreset = preset;
            }
        }
    }
    
    @Override
    protected void onRenderBefore(DrawContext drawContext, float delta) {
        HUDEditorScreen.renderElements(drawContext);
    }
    
}
