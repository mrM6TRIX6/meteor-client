/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import meteordevelopment.orbit.EventHandler;

public class Rotation extends Module {
    
    private final SettingGroup sgYaw = settings.createGroup("Yaw");
    private final SettingGroup sgPitch = settings.createGroup("Pitch");
    
    // Yaw
    
    private final Setting<LockMode> yawLockMode = sgYaw.add(new EnumChoiceSetting.Builder<LockMode>()
        .name("yaw-lock-mode")
        .description("The way in which your yaw is locked.")
        .defaultValue(LockMode.SIMPLE)
        .build()
    );
    
    private final Setting<Double> yawAngle = sgYaw.add(new DoubleSetting.Builder()
        .name("yaw-angle")
        .description("Yaw angle in degrees.")
        .defaultValue(0)
        .sliderMax(360)
        .max(360)
        .visible(() -> yawLockMode.get() == LockMode.SIMPLE)
        .build()
    );
    
    // Pitch
    
    private final Setting<LockMode> pitchLockMode = sgPitch.add(new EnumChoiceSetting.Builder<LockMode>()
        .name("pitch-lock-mode")
        .description("The way in which your pitch is locked.")
        .defaultValue(LockMode.SIMPLE)
        .build()
    );
    
    private final Setting<Double> pitchAngle = sgPitch.add(new DoubleSetting.Builder()
        .name("pitch-angle")
        .description("Pitch angle in degrees.")
        .defaultValue(0)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .visible(() -> pitchLockMode.get() == LockMode.SIMPLE)
        .build()
    );
    
    public Rotation() {
        super(Categories.PLAYER, "Rotation", "Changes/locks your yaw and pitch.");
    }
    
    @Override
    public void onActivate() {
        onTick(null);
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        switch (yawLockMode.get()) {
            case SIMPLE -> setYawAngle(yawAngle.get().floatValue());
            case SMART -> setYawAngle(getSmartYawDirection());
        }
        
        switch (pitchLockMode.get()) {
            case SIMPLE -> mc.player.setPitch(pitchAngle.get().floatValue());
            case SMART -> mc.player.setPitch(getSmartPitchDirection());
        }
    }
    
    private float getSmartYawDirection() {
        return Math.round((mc.player.getYaw() + 1f) / 45f) * 45f;
    }
    
    private float getSmartPitchDirection() {
        return Math.round((mc.player.getPitch() + 1f) / 30f) * 30f;
    }
    
    private void setYawAngle(float yawAngle) {
        mc.player.setYaw(yawAngle);
        mc.player.headYaw = yawAngle;
        mc.player.bodyYaw = yawAngle;
    }
    
    public enum LockMode implements IDisplayName {
        
        SMART("Smart"),
        SIMPLE("Simple"),
        NONE("None");
        
        private final String displayName;
        
        LockMode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
}
