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
import meteordevelopment.meteorclient.settings.impl.EnumSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.concurrent.atomic.AtomicReference;

public class Spider extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
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
        super(Categories.MOVEMENT, "Spider", "Allows you to climb walls like a spider.");
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
            event.shape = shrink(
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
    
    public static VoxelShape shrink(VoxelShape shape, double x, double y, double z) {
        if (shape.isEmpty()) {
            return VoxelShapes.empty();
        }
        
        if (shape.equals(VoxelShapes.fullCube())) {
            return VoxelShapes.cuboid(
                x,
                y,
                z,
                1.0 - x,
                1.0 - y,
                1.0 - z
            );
        }
        
        AtomicReference<VoxelShape> resultRef = new AtomicReference<>(VoxelShapes.empty());
        
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double width = maxX - minX;
            double height = maxY - minY;
            double depth = maxZ - minZ;
            
            boolean canShrinkX = x == 0.0 || width > x * 2;
            boolean canShrinkY = y == 0.0 || height > y * 2;
            boolean canShrinkZ = z == 0.0 || depth > z * 2;
            
            if (canShrinkX && canShrinkY && canShrinkZ) {
                double shrinkX = x > 0 ? x : 0.0;
                double shrinkY = y > 0 ? y : 0.0;
                double shrinkZ = z > 0 ? z : 0.0;
                
                VoxelShape shrunkBox = VoxelShapes.cuboid(
                    minX + shrinkX,
                    minY + shrinkY,
                    minZ + shrinkZ,
                    maxX - shrinkX,
                    maxY - shrinkY,
                    maxZ - shrinkZ
                );
                
                resultRef.set(VoxelShapes.union(resultRef.get(), shrunkBox));
            }
        });
        
        return resultRef.get();
    }
    
    public static VoxelShape shrink(VoxelShape shape, double x, double z) {
        return shrink(shape, x, 0.0, z);
    }
    
    public static VoxelShape shrink(VoxelShape shape, double amount) {
        return shrink(shape, amount, amount, amount);
    }
    
    private enum Mode {
        
        VANILLA,
        POLAR
        
    }
    
}
