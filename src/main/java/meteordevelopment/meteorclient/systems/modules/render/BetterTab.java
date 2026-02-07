/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.ColorSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class BetterTab extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> autoTabSize = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-tab-size")
        .description("Tab size will automatically adjust to the count of players.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Integer> tabSize = sgGeneral.add(new IntSetting.Builder()
        .name("tab-size")
        .description("How many players in total to display in the tab.")
        .defaultValue(80)
        .min(1)
        .sliderRange(1, 1000)
        .visible(() -> !autoTabSize.get())
        .build()
    );
    
    private final Setting<Integer> columnHeight = sgGeneral.add(new IntSetting.Builder()
        .name("column-height")
        .description("How many players to display in one column.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 1000)
        .visible(() -> !autoTabSize.get())
        .build()
    );
    
    private final Setting<Boolean> highlightSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-self")
        .description("Highlights yourself in the tab.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<SettingColor> selfColor = sgGeneral.add(new ColorSetting.Builder()
        .name("self-color")
        .description("The color to highlight your name with.")
        .defaultValue(new SettingColor(50, 193, 50, 100))
        .visible(highlightSelf::get)
        .build()
    );
    
    private final Setting<Boolean> highlightFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-friends")
        .description("Highlights friends in the tab.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<SettingColor> friendsColor = sgGeneral.add(new ColorSetting.Builder()
        .name("friends-color")
        .description("The color to highlight friends with.")
        .defaultValue(new SettingColor(16, 89, 203, 100))
        .visible(highlightFriends::get)
        .build()
    );
    
    private final Setting<Boolean> pingNumbers = sgGeneral.add(new BoolSetting.Builder()
        .name("ping-numbers")
        .description("Shows ping as a number in the tab.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> offlineHeads = sgGeneral.add(new BoolSetting.Builder()
        .name("offline-heads")
        .description("Render player heads on offline servers.")
        .defaultValue(true)
        .build()
    );
    
    public BetterTab() {
        super(Categories.RENDER, "BetterTab", "Various improvements to the player list hud.");
    }
    
    public boolean autoTabSize() {
        return isActive() && autoTabSize.get();
    }
    
    public int tabSize() {
        return tabSize.get();
    }
    
    public int columnHeight() {
        return columnHeight.get();
    }
    
    public boolean highlightSelf() {
        return isActive() && highlightSelf.get();
    }
    
    public Color selfColor() {
        return selfColor.get();
    }
    
    public boolean highlightFriends() {
        return isActive() && highlightFriends.get();
    }
    
    public Color friendsColor() {
        return friendsColor.get();
    }
    
    public boolean pingNumbers() {
        return isActive() && pingNumbers.get();
    }
    
    public boolean offlineHeads() {
        return isActive() && offlineHeads.get();
    }
    
}
