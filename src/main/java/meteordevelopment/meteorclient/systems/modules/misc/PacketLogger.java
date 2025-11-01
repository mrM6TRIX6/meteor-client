/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.EnumSetting;
import meteordevelopment.meteorclient.settings.impl.PacketListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PacketLogger extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<PacketStage> packetStage = sgGeneral.add(new EnumSetting.Builder<PacketStage>()
        .name("packet-stage")
        .description("The stage at which the packet is being processed.")
        .defaultValue(PacketStage.Raw)
        .build()
    );
    
    private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("S2C-packets")
        .description("Server-to-client packets to log.")
        .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
        .build()
    );
    
    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("C2S-packets")
        .description("Client-to-server packets to log.")
        .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
        .build()
    );
    
    public PacketLogger() {
        super(Categories.Misc, "packet-logger", "Logging network packets in a chat.");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacketReceiveHighest(PacketEvent.Receive event) {
        if (packetStage.get() != PacketStage.Raw || !s2cPackets.get().contains(event.packet.getClass())) {
            return;
        }
        logPacket(event.packet);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacketSendHighest(PacketEvent.Send event) {
        if (packetStage.get() != PacketStage.Raw || !c2sPackets.get().contains(event.packet.getClass())) {
            return;
        }
        logPacket(event.packet);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private void onPacketReceiveLowest(PacketEvent.Receive event) {
        if (packetStage.get() != PacketStage.Processed || !s2cPackets.get().contains(event.packet.getClass())) {
            return;
        }
        logPacket(event.packet);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private void onPacketSendLowest(PacketEvent.Send event) {
        if (packetStage.get() != PacketStage.Processed || !c2sPackets.get().contains(event.packet.getClass())) {
            return;
        }
        logPacket(event.packet);
    }
    
    private void logPacket(Packet<?> packet) {
        StringBuilder message = new StringBuilder();
        
        message.append("[")
            .append(packet.getPacketType())
            .append("] ")
            .append(getClassNameWithNested(packet.getClass()));
        
        try {
            Field[] fields = packet.getClass().getDeclaredFields();
            List<String> fieldEntries = new ArrayList<>();
            
            for (Field field : fields) {
                if (field.getName().equals("CODEC")) {
                    continue;
                }
                
                field.setAccessible(true);
                Object value = field.get(packet);
                String valueStr = formatValue(value);
                
                fieldEntries.add(field.getName() + "=" + valueStr);
            }
            
            if (!fieldEntries.isEmpty()) {
                message.append(" {")
                    .append(String.join(", ", fieldEntries))
                    .append("}");
            }
            
        } catch (IllegalAccessException e) {
            message.append(" [Failed to access fields!]");
        }
        info(message.toString());
    }
    
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Text text) {
            return "\"" + text.getString() + "\"";
        }
        if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        }
        
        return value.toString();
    }
    
    public static String getClassNameWithNested(Class<?> clazz) {
        String fullName = clazz.getName();
        
        int lastPackageDotIndex = fullName.lastIndexOf('.');
        String classNameWithNested;
        
        if (lastPackageDotIndex != -1) {
            classNameWithNested = fullName.substring(lastPackageDotIndex + 1);
        } else {
            classNameWithNested = fullName;
        }
        
        return classNameWithNested.replace('$', '.');
    }
    
    public enum PacketStage {
        Raw,
        Processed
    }
    
}
