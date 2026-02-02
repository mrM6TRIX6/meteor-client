/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class Blink extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> renderOriginal = sgGeneral.add(new BoolSetting.Builder()
        .name("render-original")
        .description("Renders your player model at the original position.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("pulse-delay")
        .description("After the duration in ticks has elapsed, send all packets and start blinking again.")
        .defaultValue(15)
        .min(0)
        .sliderMax(100)
        .build()
    );
    
    private final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private final Vector3d start = new Vector3d();
    
    private FakePlayerEntity model;
    private int timer = 0;
    private boolean sendingPackets;
    
    public Blink() {
        super(Categories.MOVEMENT, "Blink", "Allows you to essentially teleport while suspending motion updates.");
    }
    
    @Override
    public void onActivate() {
        timer = 0;
        createModel();
        Utils.set(start, mc.player.getEntityPos());
    }
    
    @Override
    public void onDeactivate() {
        if (model != null) {
            model.despawn();
            model = null;
        }
        dumpPackets(false);
        mc.player.setPos(start.x, start.y, start.z);
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (++timer > delay.get()) {
            dumpPackets(true);
            createModel();
            Utils.set(start, mc.player.getEntityPos());
            
            timer = 0;
        }
    }
    
    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (sendingPackets || !(event.packet instanceof PlayerMoveC2SPacket packet)) {
            return;
        }
        event.cancel();
        
        PlayerMoveC2SPacket prevPacket = packets.isEmpty() ? null : packets.getLast();
        
        if (prevPacket != null
            && packet.isOnGround() == prevPacket.isOnGround()
            && packet.getYaw(-1) == prevPacket.getYaw(-1)
            && packet.getPitch(-1) == prevPacket.getPitch(-1)
            && packet.getX(-1) == prevPacket.getX(-1)
            && packet.getY(-1) == prevPacket.getY(-1)
            && packet.getZ(-1) == prevPacket.getZ(-1)
        ) {
            return;
        }
        
        synchronized (packets) {
            packets.add(packet);
        }
    }
    
    @Override
    public String getInfoString() {
        return String.format("%.1f", timer / 20f);
    }
    
    private void dumpPackets(boolean send) {
        synchronized (packets) {
            if (send) {
                sendingPackets = true;
                packets.forEach(mc.player.networkHandler::sendPacket);
                sendingPackets = false;
            }
            packets.clear();
        }
    }
    
    private void createModel() {
        if (model != null) {
            model.despawn();
            model = null;
        }
        
        if (renderOriginal.get()) {
            model = new FakePlayerEntity(mc.player, mc.player.getGameProfile().name(), 20, true);
            model.doNotPush = true;
            model.hideWhenInsideCamera = true;
            model.noHit = true;
            model.spawn();
        }
    }
    
}
