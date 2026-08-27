/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.renderer.RenderUtils;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Reach extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> blockReach = sgGeneral.add(new DoubleSetting.Builder()
        .name("ExtraBlockReach")
        .description("The distance to add to your block reach.")
        .sliderMax(1)
        .build()
    );
    
    private final Setting<Double> entityReach = sgGeneral.add(new DoubleSetting.Builder()
        .name("ExtraEntityReach")
        .description("The distance to add to your entity reach.")
        .sliderMax(1)
        .build()
    );
    
    public Reach() {
        super(Category.PLAYER, "Reach", "Gives you super long arms.");
    }
    
    @Override
    public WWidget getWidget() {
        return new WLabel("Note: on vanilla servers you may give yourself up to 4 blocks of additional reach for specific actions - " +
            "interacting with block entities (chests, furnaces, etc.) or with vehicles. This does not work on paper servers.", RenderUtils.getWindowWidth() / 3.0);
    }
    
    public double blockReach() {
        return isActive() ? blockReach.get() : 0;
    }
    
    public double entityReach() {
        return isActive() ? entityReach.get() : 0;
    }
    
}
