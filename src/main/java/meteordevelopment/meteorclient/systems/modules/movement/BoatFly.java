/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.BoatMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.EnumSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BoatFly extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Boat movement mode.")
        .defaultValue(Mode.VANILLA)
        .onChanged(v -> {
            if (v != Mode.POLAR && mc.player != null && mc.player.getVehicle() instanceof BoatEntity boat) {
                boat.noClip = false;
                boat.setNoGravity(false);
            }
        })
        .build()
    );
    
    private final Setting<Double> horizontalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .build()
    );
    
    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical speed in blocks per second.")
        .defaultValue(6)
        .min(0)
        .sliderMax(20)
        .build()
    );
    
    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("How fast you fall in blocks per second.")
        .defaultValue(0.1)
        .min(0)
        .visible(() -> mode.get() == Mode.VANILLA)
        .build()
    );
    
    private final Setting<Boolean> cancelServerPackets = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-server-packets")
        .description("Cancels incoming boat move packets.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.VANILLA)
        .build()
    );
    
    public BoatFly() {
        super(Categories.MOVEMENT, "BoatFly", "Transforms your boat into a plane.");
    }
    
    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.POLAR && mc.player != null && mc.player.getVehicle() instanceof BoatEntity boat) {
            boat.noClip = false;
            boat.setNoGravity(false);
        }
    }
    
    @EventHandler
    private void onBoatMove(BoatMoveEvent event) {
        if (event.boat.getControllingPassenger() != mc.player) {
            return;
        }
        
        event.boat.setYaw(mc.player.getYaw());
        
        switch (mode.get()) {
            case VANILLA -> {
                // Horizontal movement
                Vec3d vel = PlayerUtils.getHorizontalVelocity(horizontalSpeed.get());
                double velX = vel.getX();
                double velY = 0;
                double velZ = vel.getZ();
                
                // Vertical movement
                if (mc.options.jumpKey.isPressed()) {
                    velY += verticalSpeed.get() / 20;
                }
                if (Input.isPressed(mc.options.sprintKey)) {
                    velY -= verticalSpeed.get() / 20;
                } else {
                    velY -= fallSpeed.get() / 20;
                }
                
                // Apply motion
                event.boat.setVelocity(velX, velY, velZ);
            }
            
            case POLAR -> {
                // NoClip flags
                event.boat.noClip = true;
                event.boat.setNoGravity(true);
                
                boolean insideBlock = isInsideSolidBlock();
                double speed = insideBlock ? 0.25 : horizontalSpeed.get();
                
                double motionX = 0;
                double motionY = 0;
                double motionZ = 0;
                
                float yaw = event.boat.getYaw();
                double yawRad = Math.toRadians(yaw);
                
                double sinYaw = Math.sin(yawRad);
                double cosYaw = Math.cos(yawRad);
                
                // Forward
                if (mc.options.forwardKey.isPressed()) {
                    motionX -= sinYaw * speed;
                    motionZ += cosYaw * speed;
                }
                
                // Backward
                if (mc.options.backKey.isPressed()) {
                    motionX += sinYaw * speed;
                    motionZ -= cosYaw * speed;
                }
                
                // Left
                if (mc.options.leftKey.isPressed()) {
                    motionX += cosYaw * speed;
                    motionZ += sinYaw * speed;
                }
                
                // Right
                if (mc.options.rightKey.isPressed()) {
                    motionX -= cosYaw * speed;
                    motionZ -= sinYaw * speed;
                }
                
                // Up
                if (mc.options.jumpKey.isPressed())
                    motionY += verticalSpeed.get();
                
                // Down
                if (mc.options.sprintKey.isPressed())
                    motionY -= verticalSpeed.get();
                
                // Apply motion
                event.boat.setVelocity(motionX, motionY, motionZ);
                
            }
        }
    }
    
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof VehicleMoveS2CPacket && mode.get() == Mode.VANILLA && cancelServerPackets.get()) {
            event.cancel();
        }
    }
    
    private boolean isInsideSolidBlock() {
        Box box = mc.player.getBoundingBox().expand(0.001);
        
        int minX = MathHelper.floor(box.minX);
        int minY = MathHelper.floor(box.minY);
        int minZ = MathHelper.floor(box.minZ);
        
        int maxX = MathHelper.floor(box.maxX);
        int maxY = MathHelper.floor(box.maxY);
        int maxZ = MathHelper.floor(box.maxZ);
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = BlockPos.ofFloored(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    
                    if (state.isSolid()) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private enum Mode {
        
        VANILLA,
        POLAR
        
    }
    
}
