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
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class BetterTab extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    public final Setting<Boolean> autoTabSize = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-tab-size")
        .description("Tab size will automatically adjust to the count of players.")
        .defaultValue(true)
        .build()
    );
    
    public final Setting<Integer> tabSize = sgGeneral.add(new IntSetting.Builder()
        .name("tab-size")
        .description("How many players in total to display in the tab.")
        .defaultValue(80)
        .min(1)
        .sliderRange(1, 1000)
        .visible(() -> !autoTabSize.get())
        .build()
    );
    
    public final Setting<Integer> columnHeight = sgGeneral.add(new IntSetting.Builder()
        .name("column-height")
        .description("How many players to display in one column.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 1000)
        .visible(() -> !autoTabSize.get())
        .build()
    );
    
    public final Setting<Boolean> highlightSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-self")
        .description("Highlights yourself in the tab.")
        .defaultValue(true)
        .build()
    );
    
    public final Setting<SettingColor> selfColor = sgGeneral.add(new ColorSetting.Builder()
        .name("self-color")
        .description("The color to highlight your name with.")
        .defaultValue(new SettingColor(50, 193, 50, 100))
        .visible(highlightSelf::get)
        .build()
    );
    
    public final Setting<Boolean> highlightFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-friends")
        .description("Highlights friends in the tab.")
        .defaultValue(true)
        .build()
    );
    
    public final Setting<SettingColor> friendsColor = sgGeneral.add(new ColorSetting.Builder()
        .name("friends-color")
        .description("The color to highlight friends with.")
        .defaultValue(new SettingColor(16, 89, 203, 100))
        .visible(highlightFriends::get)
        .build()
    );
    
    public final Setting<Boolean> pingNumbers = sgGeneral.add(new BoolSetting.Builder()
        .name("ping-numbers")
        .description("Shows ping as a number in the tab.")
        .defaultValue(true)
        .build()
    );
    
    public BetterTab() {
        super(Categories.Render, "better-tab", "Various improvements to the tab.");
    }
    
}
