/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.PacketListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.Packet;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Set;

public class PacketDebugger extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Set<Class<? extends Packet<?>>>> nativeC2SPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("native-C2S-packets")
        .description("The original client-to-server packets have not been hooked and modified.")
        .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
        .build()
    );
    
    private final Setting<Set<Class<? extends Packet<?>>>> nativeS2CPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("native-S2C-packets")
        .description("The original server-to-client packets have not been hooked and modified.")
        .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
        .build()
    );
    
    private final Setting<Set<Class<? extends Packet<?>>>> finalC2SPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("final-C2S-packets")
        .description("The final server-to-client packets, which can be hooked and modified.")
        .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
        .build()
    );
    
    private final Setting<Set<Class<? extends Packet<?>>>> finalS2CPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("final-S2C-packets")
        .description("The final server-to-client packets, which can be hooked and modified.")
        .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
        .build()
    );
    
    public PacketDebugger() {
        super(Categories.Misc, "PacketDebugger", "Logging network packets.");
        runInMainMenu = true;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST + 2)
    private void onNativePacketSend(PacketEvent.Send event) {
        if (nativeC2SPackets.get().contains(event.packet.getClass())) {
            logPacket(event.packet);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST + 2)
    private void onNativePacketReceive(PacketEvent.Receive event) {
        if (nativeS2CPackets.get().contains(event.packet.getClass())) {
            logPacket(event.packet);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private void onFinalPacketSend(PacketEvent.Send event) {
        if (finalC2SPackets.get().contains(event.packet.getClass())) {
            logPacket(event.packet);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private void onFinalPacketReceive(PacketEvent.Receive event) {
        if (finalS2CPackets.get().contains(event.packet.getClass())) {
            logPacket(event.packet);
        }
    }
    
    private void logPacket(Packet<?> packet) {
        String packetString = packet.getClass().isRecord() ? packet.toString() : ToStringBuilder.reflectionToString(packet, ToStringStyle.SHORT_PREFIX_STYLE);
        
        int bracketIndex = packetString.indexOf('[');
        String name = packetString.substring(0, bracketIndex);
        String params = packetString.substring(bracketIndex);
        
        if (mc.world != null) {
            info("[(highlight)%s(default)/(highlight)%s(default)] (highlight)%s(default)%s", packet.getPacketType().side(), packet.getPacketType().id(), name, params);
        } else {
            MeteorClient.LOG.info("Packet Debugger | [{}/{}] {}", packet.getPacketType().side(), packet.getPacketType().id(), name + params);
        }
    }
    
}
