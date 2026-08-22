/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.impl;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.engine.Fonts;
import meteordevelopment.meteorclient.renderer.engine.text.FontFamily;
import meteordevelopment.meteorclient.renderer.engine.text.FontInfo;
import meteordevelopment.meteorclient.settings.impl.FontFaceSetting;
import org.apache.commons.lang3.Strings;

import java.util.List;

public class FontFaceSettingScreen extends WindowScreen {
    
    private final FontFaceSetting setting;
    
    private WTable table;
    
    private WTextBox filter;
    private String filterText = "";
    
    public FontFaceSettingScreen(FontFaceSetting setting) {
        super("Select MsdfFont");
        
        this.setting = setting;
    }
    
    @Override
    public void initWidgets() {
        filter = add(new WTextBox("")).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();
            
            table.clear();
            initTable();
        };
        
        window.view.hasScrollBar = false;
        
        enterAction = () -> {
            List<Cell<?>> row = table.getRow(0);
            if (row == null) {
                return;
            }
            
            WWidget widget = row.get(2).widget();
            if (widget instanceof WButton button) {
                button.action.run();
            }
        };
        
        WView view = add(new WView()).expandX().widget();
        
        // Prevents double scrolling for view-in-view scenario
        view.maxHeight = window.view.maxHeight - 128;
        
        view.scrollOnlyWhenMouseOver = false;
        table = view.add(new WTable()).expandX().widget();
        
        initTable();
    }
    
    private void initTable() {
        for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
            String name = fontFamily.getName();
            
            WLabel item = new WLabel(name);
            if (!filterText.isEmpty() && !Strings.CI.contains(name, filterText)) {
                continue;
            }
            table.add(item);
            
            WDropdown<FontInfo.Type> dropdown = table.add(WDropdown.of(FontInfo.Type.REGULAR)).right().widget();
            
            WButton select = table.add(new WButton("Select")).expandCellX().right().widget();
            select.action = () -> {
                setting.set(fontFamily.get(dropdown.get()));
                close();
            };
            
            table.row();
        }
    }
    
}
