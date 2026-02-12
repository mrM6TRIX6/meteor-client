/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;

public class AutoClicker extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> inScreens = sgGeneral.add(new BoolSetting.Builder()
        .name("while-in-screens")
        .description("Whether to click while a screen is open.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Mode> leftClickMode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("mode-left")
        .description("The method of clicking for left clicks.")
        .defaultValue(Mode.PRESS)
        .build()
    );
    
    private final Setting<Integer> leftClickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-left")
        .description("The amount of delay between left clicks in ticks.")
        .defaultValue(2)
        .min(0)
        .sliderMax(60)
        .visible(() -> leftClickMode.get() == Mode.PRESS)
        .build()
    );
    
    private final Setting<Mode> rightClickMode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("mode-right")
        .description("The method of clicking for right clicks.")
        .defaultValue(Mode.PRESS)
        .build()
    );
    
    private final Setting<Integer> rightClickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-right")
        .description("The amount of delay between right clicks in ticks.")
        .defaultValue(2)
        .min(0)
        .sliderMax(60)
        .visible(() -> rightClickMode.get() == Mode.PRESS)
        .build()
    );
    
    private int rightClickTimer, leftClickTimer;
    
    public AutoClicker() {
        super(Categories.PLAYER, "AutoClicker", "Automatically clicks.");
    }
    
    @Override
    public void onActivate() {
        rightClickTimer = 0;
        leftClickTimer = 0;
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
    }
    
    @Override
    public void onDeactivate() {
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!inScreens.get() && mc.currentScreen != null) {
            return;
        }
        
        switch (leftClickMode.get()) {
            case DISABLED -> {}
            case HOLD -> mc.options.attackKey.setPressed(true);
            case PRESS -> {
                leftClickTimer++;
                if (leftClickTimer > leftClickDelay.get()) {
                    PlayerUtils.leftClick();
                    leftClickTimer = 0;
                }
            }
        }
        
        switch (rightClickMode.get()) {
            case DISABLED -> {}
            case HOLD -> mc.options.useKey.setPressed(true);
            case PRESS -> {
                rightClickTimer++;
                if (rightClickTimer > rightClickDelay.get()) {
                    PlayerUtils.rightClick();
                    rightClickTimer = 0;
                }
            }
        }
    }
    
    private enum Mode implements IDisplayName {
        
        DISABLED("Disabled"),
        HOLD("Hold"),
        PRESS("Press");
        
        private final String displayName;
        
        Mode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
}
