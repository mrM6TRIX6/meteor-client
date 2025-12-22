/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Twerk extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Integer> sneakTime = sgGeneral.add(new IntSetting.Builder()
        .name("sneak-time")
        .description("How many ticks to stay sneaked.")
        .defaultValue(2)
        .min(0)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("standing-time")
        .description("How many ticks to not stay sneaked.")
        .defaultValue(2)
        .min(0)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );
    
    private int timer;
    private boolean sneaking;
    
    public Twerk() {
        super(Categories.Fun, "Twerk", "Automatically sneaks and stands.");
    }
    
    @Override
    public void onActivate() {
        timer = 0;
        sneaking = false;
    }
    
    @Override
    public void onDeactivate() {
        mc.options.sneakKey.setPressed(false);
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || !isActive()) {
            return;
        }
        
        if (timer-- > 0) {
            return;
        }
        
        sneaking = !sneaking;
        timer = sneaking ? sneakTime.get() : delay.get();
        mc.options.sneakKey.setPressed(sneaking);
    }
    
}
