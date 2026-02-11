/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.spider.modes;

import meteordevelopment.meteorclient.events.entity.player.PlayerJumpEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.settings.impl.ModeEnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.spider.Spider;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;

public class Polar extends ModeEnumChoiceSetting.ModeImpl<Spider> {
    
    public Polar() {
        super(() -> Modules.get().get(Spider.class));
    }
    
    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
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
        double highJump = getParent().jumpHeight();
        
        if (mc.player.horizontalCollision && highJump > 0.42F) {
            event.motion = (float) highJump;
        }
    }
    
}
