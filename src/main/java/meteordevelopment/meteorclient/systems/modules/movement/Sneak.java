/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public class Sneak extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("mode")
        .description("Which method to sneak.")
        .defaultValue(Mode.VANILLA)
        .build()
    );
    
    public Sneak() {
        super(Categories.MOVEMENT, "Sneak", "Sneaks for you.");
    }
    
    public boolean doPacket() {
        return isActive() && !mc.player.getAbilities().flying && mode.get() == Mode.PACKET;
    }
    
    public boolean doVanilla() {
        return isActive() && !mc.player.getAbilities().flying && mode.get() == Mode.VANILLA;
    }
    
    private enum Mode implements IDisplayName {
        
        PACKET("Packet"),
        VANILLA("Vanilla");
        
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
