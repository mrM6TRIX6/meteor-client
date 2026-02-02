/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.EnumSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Arm;

public class HandDerp extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("When the hand switches.")
        .defaultValue(Mode.DELAY)
        .build()
    );
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .visible(() -> mode.get() == Mode.DELAY)
        .build()
    );
    
    private final Setting<Boolean> hideFirstPerson = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-first-person")
        .description("Ignore arm switching changes for first person.")
        .defaultValue(false)
        .build()
    );
    
    private Arm originalHand;
    private Arm currentHand;
    
    private int timer;
    
    public HandDerp() {
        super(Categories.FUN, "HandDerp", "Switches your main hand.");
    }
    
    @Override
    public void onActivate() {
        timer = 0;
        originalHand = mc.player.getMainArm();
        currentHand = originalHand;
    }
    
    @Override
    public void onDeactivate() {
        if (currentHand != originalHand) {
            switchHand();
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (++timer > delay.get() && mode.get() == Mode.DELAY) {
            switchHand();
            timer = 0;
        }
    }
    
    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof HandSwingC2SPacket && mode.get() == Mode.SWING) {
            switchHand();
        }
    }
    
    private void switchHand() {
        currentHand = currentHand.getOpposite();
        mc.player.setMainArm(currentHand);
    }
    
    public boolean hideFirstPerson() {
        return isActive() && hideFirstPerson.get();
    }
    
    public Arm getOriginalHand() {
        return originalHand;
    }
    
    private enum Mode {
        
        DELAY,
        SWING
        
    }
    
}
