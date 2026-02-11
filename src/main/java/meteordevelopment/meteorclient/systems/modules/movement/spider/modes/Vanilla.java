/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.spider.modes;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.impl.ModeEnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.spider.Spider;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class Vanilla extends ModeEnumChoiceSetting.ModeImpl<Spider> {
    
    public Vanilla() {
        super(() -> Modules.get().get(Spider.class));
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!mc.player.horizontalCollision) {
            return;
        }
        
        Vec3d velocity = mc.player.getVelocity();
        if (velocity.y >= 0.2) {
            return;
        }
        
        mc.player.setVelocity(velocity.x, getParent().speed(), velocity.z);
    }
    
}
