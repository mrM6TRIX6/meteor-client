/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.impl;

import meteordevelopment.meteorclient.gui.screens.settings.DynamicRegistryListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.name.Names;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.Set;

public class EnchantmentListSettingScreen extends DynamicRegistryListSettingScreen<Enchantment> {
    
    public EnchantmentListSettingScreen(Setting<Set<RegistryKey<Enchantment>>> setting) {
        super("Select Enchantments", setting, setting.get(), RegistryKeys.ENCHANTMENT);
    }
    
    @Override
    protected WWidget getValueWidget(RegistryKey<Enchantment> value) {
        return new WLabel(Names.get(value));
    }
    
    protected String[] getValueNames(RegistryKey<Enchantment> value) {
        return new String[] { Names.get(value), value.getValue().toString() };
    }
    
}
