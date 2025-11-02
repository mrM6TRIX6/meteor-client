/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.config.Setting;
import meteordevelopment.meteorclient.config.SettingGroup;
import meteordevelopment.meteorclient.config.types.BoolSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;

public class AirWalk extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> allowJumping = sgGeneral.add(new BoolSetting.Builder()
        .name("Ease Jumping (Buggy)")
        .description("Makes getting up easier")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Boolean> allowSneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("Allow Sneaking")
        .description("Allows sneaking while in the air")
        .defaultValue(false)
        .build()
    );
    
    public AirWalk() {
        super(Categories.Movement, "air-walk", "Lets you walk on air");
    }
    
    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (mc.player == null) {
            return;
        }
        
        boolean sneaking = mc.options.sneakKey.isPressed();
        boolean jumping = mc.options.jumpKey.isPressed();
        
        if (sneaking && !allowSneaking.get()) {
            return;
        }
        if (jumping && allowJumping.get()) {
            mc.player.jump();
        }
        
        int playerX = mc.player.getBlockPos().getX();
        int playerY = mc.player.getBlockPos().getY();
        int playerZ = mc.player.getBlockPos().getZ();
        
        BlockPos pos = event.pos;
        BlockPos lock = new BlockPos(playerX, playerY - 1, playerZ);
        
        if (lock.equals(pos)) {
            event.shape = VoxelShapes.fullCube();
        }
    }
    
}
