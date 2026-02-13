/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerTickPreEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.util.Random;

public class Freeze extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> disableOnFlag = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-flag")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> cancelC0B = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-C0B")
        .title("Cancel C0B")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> notification = sgGeneral.add(new BoolSetting.Builder()
        .name("notification")
        .defaultValue(false)
        .visible(disableOnFlag::get)
        .build()
    );
    
    private final Setting<Boolean> balance = sgGeneral.add(new BoolSetting.Builder()
        .name("balance-warp")
        .defaultValue(false)
        .build()
    );
    
    private final FloatOffsetGenerator yawOffset = new FloatOffsetGenerator();
    private final FloatOffsetGenerator pitchOffset = new FloatOffsetGenerator();
    
    private double missedOutTick;
    private boolean warpInProgress;
    
    public Freeze() {
        super(Categories.MOVEMENT, "Freeze", "Allows you to freeze yourself without the server knowing.");
    }
    
    @Override
    public void onActivate() {
        missedOutTick = 0;
    }
    
    @Override
    public void onDeactivate() {
        if (balance.get()) {
            warpInProgress = true;
            while (missedOutTick > 0) {
                // todo: does not run module tick if running at game tick layer
                mc.player.tick();
                missedOutTick--;
            }
            warpInProgress = false;
        }
        
        missedOutTick = 0;
    }
    
    @EventHandler
    private void onPlayerTickPre(PlayerTickPreEvent event) {
        if (warpInProgress) {
            return;
        }
        
        event.cancel();
        missedOutTick++;
    }
    
    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        float yawOffset = this.yawOffset.nextFloat();
        float pitchOffset = this.pitchOffset.nextFloat();
        
        switch (event.packet) {
            case CommonPongC2SPacket ignored -> {
                if (cancelC0B.get()) {
                    event.cancel();
                }
            }
            
            case PlayerInteractItemC2SPacket packet -> {
                event.cancel();
                
                event.sendPacketSilently(
                    new PlayerMoveC2SPacket.LookAndOnGround(
                        mc.player.getYaw() + yawOffset,
                        mc.player.getPitch() + pitchOffset,
                        mc.player.isOnGround(),
                        mc.player.horizontalCollision
                    )
                );
                
                event.sendPacketSilently(
                    new PlayerInteractItemC2SPacket(
                        packet.getHand(),
                        packet.getSequence(),
                        mc.player.getYaw() + yawOffset,
                        mc.player.getPitch() + pitchOffset
                    )
                );
            }
            
            case PlayerInteractEntityC2SPacket ignored -> {
                event.cancel();
                
                event.sendPacketSilently(
                    new PlayerMoveC2SPacket.LookAndOnGround(
                        mc.player.getYaw() + yawOffset,
                        mc.player.getPitch() + pitchOffset,
                        mc.player.isOnGround(),
                        mc.player.horizontalCollision
                    )
                );
            }
            
            case PlayerInteractBlockC2SPacket packet -> {
                event.cancel();
                
                event.sendPacketSilently(
                    new PlayerMoveC2SPacket.LookAndOnGround(
                        mc.player.getYaw() + yawOffset,
                        mc.player.getPitch() + pitchOffset,
                        mc.player.isOnGround(),
                        mc.player.horizontalCollision
                    )
                );
                
                event.sendPacketSilently(packet);
            }
            
            default -> {}
        }
    }
    
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            missedOutTick = 0;
            if (disableOnFlag.get()) {
                if (notification.get()) {
                    info("Module disabled due to flag.");
                }
                toggle();
            }
        }
    }
    
    /**
     * Bypasses Grim's duplicate rotation check
     */
    private static class FloatOffsetGenerator {
        
        private final Random random = new Random();
        
        private float prev;
        
        public float nextFloat() {
            float offset;
            do {
                offset = (float) (0.002 + random.nextDouble() * (0.01 - 0.002));
            } while (Math.abs(offset - prev) < 1.0E-6F);
            
            prev = offset;
            return offset;
        }
        
    }
    
}
