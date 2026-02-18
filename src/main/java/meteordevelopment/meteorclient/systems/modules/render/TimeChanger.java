/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.time.LocalTime;

public class TimeChanger extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> systemSync = sgGeneral.add(new BoolSetting.Builder()
        .name("system-sync")
        .description("Synchronize world time with your real time.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Double> time = sgGeneral.add(new DoubleSetting.Builder()
        .name("time")
        .description("The specified time to be set.")
        .defaultValue(0)
        .sliderRange(-20000, 20000)
        .visible(() -> !systemSync.get())
        .build()
    );
    
    private long oldTime;
    
    public TimeChanger() {
        super(Categories.RENDER, "TimeChanger", "Makes you able to set a custom time.");
    }
    
    @Override
    public void onActivate() {
        oldTime = mc.world.getTime();
    }
    
    @Override
    public void onDeactivate() {
        mc.world.getLevelProperties().setTimeOfDay(oldTime);
    }
    
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof WorldTimeUpdateS2CPacket) {
            oldTime = ((WorldTimeUpdateS2CPacket) event.packet).timeOfDay();
            event.cancel();
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        long time;
        
        if (systemSync.get()) {
            LocalTime now = LocalTime.now();
            time = (now.getHour() * 1000 + now.getMinute() * (1000 / 60)) - 6000;
        } else {
            time = this.time.get().longValue();
        }
        
        mc.world.getLevelProperties().setTimeOfDay(time);
    }
    
}
