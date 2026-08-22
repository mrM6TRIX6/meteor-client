/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.impl;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WItemWithLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.impl.ItemSetting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import org.apache.commons.lang3.Strings;

public class ItemSettingScreen extends WindowScreen {
    
    private final ItemSetting setting;
    
    private WTable table;
    
    private WTextBox filter;
    private String filterText = "";
    
    public ItemSettingScreen(ItemSetting setting) {
        super("Select item");
        
        this.setting = setting;
    }
    
    @Override
    public void initWidgets() {
        filter = add(new WTextBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();
            
            table.clear();
            initTable();
        };
        
        table = add(new WTable()).expandX().widget();
        initTable();
    }
    
    public void initTable() {
        for (Item item : Registries.ITEM) {
            if (setting.filter != null && !setting.filter.test(item)) {
                continue;
            }
            if (item == Items.AIR) {
                continue;
            }
            
            WItemWithLabel itemLabel = new WItemWithLabel(item.getDefaultStack(), Names.get(item));
            if (!filterText.isEmpty() && !Strings.CI.contains(itemLabel.getLabelText(), filterText)) {
                continue;
            }
            table.add(itemLabel);
            
            WButton select = table.add(new WButton("Select")).expandCellX().right().widget();
            select.action = () -> {
                setting.set(item);
                close();
            };
            
            table.row();
        }
    }
    
}
