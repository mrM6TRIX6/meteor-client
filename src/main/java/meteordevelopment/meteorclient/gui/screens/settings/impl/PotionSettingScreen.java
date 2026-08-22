/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.impl;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WItemWithLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.impl.PotionChoiceSetting;
import meteordevelopment.meteorclient.utils.misc.MyPotion;

public class PotionSettingScreen extends WindowScreen {
    
    private final PotionChoiceSetting setting;
    
    public PotionSettingScreen(PotionChoiceSetting setting) {
        super("Select Potion");
        
        this.setting = setting;
    }
    
    @Override
    public void initWidgets() {
        WTable table = add(new WTable()).expandX().widget();
        
        for (MyPotion potion : MyPotion.values()) {
            table.add(new WItemWithLabel(potion.potion, potion.getDisplayName()));
            
            WButton select = table.add(new WButton("Select")).widget();
            select.action = () -> {
                setting.set(potion);
                close();
            };
            
            table.row();
        }
    }
    
}
