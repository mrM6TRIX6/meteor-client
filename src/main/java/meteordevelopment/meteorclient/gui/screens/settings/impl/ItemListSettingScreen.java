/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.impl;

import meteordevelopment.meteorclient.gui.screens.settings.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WItemWithLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.impl.ItemListSetting;
import meteordevelopment.meteorclient.utils.name.Names;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.function.Predicate;

public class ItemListSettingScreen extends CollectionListSettingScreen<Item> {
    
    public ItemListSettingScreen(ItemListSetting setting) {
        super("Select Items", setting, setting.get(), Registries.ITEM);
    }
    
    @Override
    protected boolean includeValue(Item value) {
        Predicate<Item> filter = ((ItemListSetting) setting).filter;
        if (filter != null && !filter.test(value)) {
            return false;
        }
        
        return value != Items.AIR;
    }
    
    @Override
    protected WWidget getValueWidget(Item value) {
        return new WItemWithLabel(value.getDefaultStack());
    }
    
    protected String[] getValueNames(Item value) {
        return new String[] { Names.get(value), Registries.ITEM.getId(value).toString() };
    }
    
}
