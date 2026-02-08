/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ITagged;
import meteordevelopment.orbit.EventHandler;

public class AutoJump extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("mode")
        .description("The method of jumping.")
        .defaultValue(Mode.JUMP)
        .build()
    );
    
    private final Setting<JumpWhen> jumpIf = sgGeneral.add(new EnumChoiceSetting.Builder<JumpWhen>()
        .name("jump-if")
        .description("Jump if.")
        .defaultValue(JumpWhen.ALWAYS)
        .build()
    );
    
    private final Setting<Double> velocityHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("velocity-height")
        .description("The distance that velocity mode moves you.")
        .defaultValue(0.25)
        .min(0)
        .sliderMax(2)
        .build()
    );
    
    public AutoJump() {
        super(Categories.MOVEMENT, "AutoJump", "Automatically jumps.");
    }
    
    private boolean jump() {
        return switch (jumpIf.get()) {
            case SPRINTING -> mc.player.isSprinting() && (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0);
            case WALKING -> mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0;
            case ALWAYS -> true;
        };
    }
    
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (!mc.player.isOnGround() || mc.player.isSneaking() || !jump()) {
            return;
        }
        
        if (mode.get() == Mode.JUMP) {
            mc.player.jump();
        } else {
            ((IVec3d) mc.player.getVelocity()).meteor$setY(velocityHeight.get());
        }
    }
    
    private enum JumpWhen implements ITagged {
        
        SPRINTING("Sprinting"),
        WALKING("Walking"),
        ALWAYS("Always");
        
        private final String tag;
        
        JumpWhen(String tag) {
            this.tag = tag;
        }
        
        @Override
        public String getTag() {
            return tag;
        }
        
    }
    
    private enum Mode implements ITagged {
        
        JUMP("Jump"),
        LOW_HOP("Low Hop");
        
        private final String tag;
        
        Mode(String tag) {
            this.tag = tag;
        }
        
        @Override
        public String getTag() {
            return tag;
        }
        
    }
    
}
