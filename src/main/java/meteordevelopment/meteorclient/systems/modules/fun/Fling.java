/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Fling extends Module {
    
    private static final int MAX_BLOCKS = 33;
    private static final double MAX_PACKET_DISTANCE = 10.0;
    private static final int TELEPORT_DELAY = 25;
    private static final int REEL_DELAY = 250;
    
    private volatile boolean busy;
    private Thread flingThread;
    
    public Fling() {
        super(Category.FUN, "Fling", "Fling someone in the air.");
    }
    
    @Override
    public void onDeactivate() {
        busy = false;
        
        if (flingThread != null) {
            flingThread.interrupt();
            flingThread = null;
        }
    }
    
    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (busy || !Utils.canUpdate()) {
            return;
        }
        if (!(event.packet instanceof PlayerInteractItemC2SPacket)) {
            return;
        }
        
        if (!canFling()) {
            return;
        }
        
        event.cancel();
        
        Vec3d startPos = mc.player.getEntityPos();
        Vec3d targetPos = mc.player.fishHook.getHookedEntity().getEntityPos();
        Vec3d hookPos = mc.player.fishHook.getEntityPos();
        Vec3d flingPos = getHighestFreePos(targetPos, hookPos);
        
        startFlingSequence(startPos, targetPos, flingPos);
    }
    
    private boolean canFling() {
        if (mc.player.hasVehicle()) {
            info("Sir, please dismount from the vehicle");
            return false;
        }
        
        if (mc.player.getMainHandStack().getItem() != Items.FISHING_ROD) {
            return false;
        }
        
        if (mc.player.fishHook == null || mc.player.fishHook.isRemoved()) {
            return false;
        }
        
        return mc.player.fishHook.getHookedEntity() != null
            && !mc.player.fishHook.getHookedEntity().isRemoved();
    }
    
    private void startFlingSequence(Vec3d startPos, Vec3d targetPos, Vec3d flingPos) {
        busy = true;
        
        flingThread = new Thread(() -> {
            try {
                teleportSync(targetPos);
                sleep(TELEPORT_DELAY);
                
                teleportSync(flingPos);
                sleep(REEL_DELAY);
                
                executeSync(this::sendUseItem);
                sleep(TELEPORT_DELAY);
                
                teleportSync(targetPos);
                sleep(TELEPORT_DELAY);
                
                teleportSync(startPos);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                busy = false;
            }
        }, "FishingRodFling");
        
        flingThread.start();
    }
    
    private void teleportSync(Vec3d pos) throws InterruptedException {
        executeSync(() -> teleport(pos));
    }
    
    private void executeSync(Runnable task) throws InterruptedException {
        if (Utils.canUpdate()) {
            mc.execute(task);
        }
    }
    
    private void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }
    
    private Vec3d getHighestFreePos(Vec3d entityPos, Vec3d hookPos) {
        double dx = entityPos.x - hookPos.x;
        double dz = entityPos.z - hookPos.z;
        
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double maxHeight = Math.sqrt(
            Math.max(0.0, MAX_BLOCKS * MAX_BLOCKS - horizontalDistance * horizontalDistance)
        );
        
        int minY = Math.max(
            MathHelper.floor(entityPos.y),
            mc.world.getBottomY()
        );
        
        int maxY = Math.min(
            MathHelper.floor(entityPos.y + maxHeight),
            mc.world.getTopYInclusive() - 2
        );
        
        for (int y = maxY; y >= minY; y--) {
            Vec3d pos = new Vec3d(entityPos.x, y, entityPos.z);
            
            if (pos.distanceTo(hookPos) > MAX_BLOCKS) {
                continue;
            }
            
            Box box = mc.player.getBoundingBox().offset(pos.subtract(mc.player.getEntityPos()));
            
            if (mc.world.isSpaceEmpty(mc.player, box)) {
                return pos;
            }
        }
        
        return entityPos;
    }
    
    private void sendUseItem() {
        if (!Utils.canUpdate()) {
            return;
        }
        
        mc.interactionManager.sendSequencedPacket(
            mc.world,
            sequence -> new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                sequence,
                mc.player.getYaw(),
                mc.player.getPitch()
            )
        );
    }
    
    private void teleport(Vec3d destination) {
        if (!Utils.canUpdate()) {
            return;
        }
        
        mc.player.setVelocity(Vec3d.ZERO);
        
        Vec3d currentPos = mc.player.getEntityPos();
        double distance = currentPos.distanceTo(destination);
        
        if (distance > MAX_PACKET_DISTANCE) {
            int steps = (int) Math.floor(distance / MAX_PACKET_DISTANCE);
            
            for (int i = 1; i <= steps; i++) {
                if (!Utils.canUpdate()) {
                    return;
                }
                
                double progress = Math.min(
                    1.0,
                    (i * MAX_PACKET_DISTANCE) / distance
                );
                
                sendPositionPacket(currentPos.lerp(destination, progress));
            }
        }
        
        sendPositionPacket(destination);
        mc.player.setPosition(destination);
    }
    
    private void sendPositionPacket(Vec3d pos) {
        mc.getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.PositionAndOnGround(
                pos.x,
                pos.y,
                pos.z,
                true,
                mc.player.horizontalCollision
            )
        );
    }
    
}