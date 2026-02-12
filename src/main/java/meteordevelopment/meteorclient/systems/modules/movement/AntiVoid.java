/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import meteordevelopment.orbit.EventHandler;

public class AntiVoid extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("mode")
        .description("The method to prevent you from falling into the void.")
        .defaultValue(Mode.JUMP)
        .onChanged(a -> onActivate())
        .build()
    );
    
    private boolean wasFlightEnabled, hasRun;
    
    public AntiVoid() {
        super(Categories.MOVEMENT, "AntiVoid", "Attempts to prevent you from falling into the void.");
    }
    
    @Override
    public void onActivate() {
        if (mode.get() == Mode.FLIGHT) {
            wasFlightEnabled = Modules.get().isActive(Flight.class);
        }
    }
    
    @Override
    public void onDeactivate() {
        if (!wasFlightEnabled && mode.get() == Mode.FLIGHT && Utils.canUpdate()) {
            Modules.get().get(Flight.class).disable();
        }
    }
    
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        int minY = mc.world.getBottomY();
        
        if (mc.player.getY() > minY || mc.player.getY() < minY - 15) {
            if (hasRun && mode.get() == Mode.FLIGHT) {
                Modules.get().get(Flight.class).disable();
                hasRun = false;
            }
            return;
        }
        
        switch (mode.get()) {
            case FLIGHT -> {
                Modules.get().get(Flight.class).enable();
                hasRun = true;
            }
            case JUMP -> mc.player.jump();
        }
    }
    
    private enum Mode implements IDisplayName {
        
        FLIGHT("Flight"),
        JUMP("Jump");
        
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
