/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerJumpEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class Spider extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("mode")
        .description("Spider mode.")
        .defaultValue(Mode.VANILLA)
        .build()
    );
    
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("climb-speed")
        .description("The speed you go up blocks.")
        .defaultValue(0.2)
        .min(0.0)
        .visible(() -> mode.get() == Mode.VANILLA)
        .build()
    );
    
    private final Setting<Double> jumpHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("jump-height")
        .description("Jump height.")
        .defaultValue(0.55)
        .min(0.0)
        .sliderRange(0.42, 0.6)
        .visible(() -> mode.get() == Mode.POLAR)
        .build()
    );
    
    public Spider() {
        super(Category.MOVEMENT, "Spider", "Allows you to climb walls like a spider.");
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!mc.player.horizontalCollision || mode.get() != Mode.VANILLA) {
            return;
        }
        
        Vec3d velocity = mc.player.getVelocity();
        if (velocity.y >= 0.2) {
            return;
        }
        
        mc.player.setVelocity(velocity.x, speed.get(), velocity.z);
    }
    
    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (mode.get() != Mode.POLAR) {
            return;
        }
        
        if (event.pos.getY() >= mc.player.getY() || mc.options.sneakKey.isPressed() && mc.player.isOnGround()) {
            event.shape = BlockUtils.shrink(
                event.shape,
                0.0001,
                0.0001
            );
        }
    }
    
    @EventHandler
    private void onPlayerJump(PlayerJumpEvent event) {
        if (mode.get() != Mode.POLAR) {
            return;
        }
        
        double highJump = jumpHeight.get();
        
        if (mc.player.horizontalCollision && highJump > 0.42F) {
            event.motion = (float) highJump;
        }
    }
    
    private enum Mode implements IDisplayName {
        
        VANILLA("Vanilla"),
        POLAR("Polar");
        
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
