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
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.Packet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Set;

public class PacketDebugger extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Set<Class<? extends Packet<?>>>> nativeC2SPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("native-C2S-packets")
        .title("Native C2S Packets")
        .description("The original client-to-server packets have not been hooked and modified.")
        .filter(p -> PacketUtils.getC2SPackets().contains(p))
        .build()
    );
    
    private final Setting<Set<Class<? extends Packet<?>>>> nativeS2CPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("native-S2C-packets")
        .title("Native S2C Packets")
        .description("The original server-to-client packets have not been hooked and modified.")
        .filter(p -> PacketUtils.getS2CPackets().contains(p))
        .build()
    );
    
    private final Setting<Set<Class<? extends Packet<?>>>> finalC2SPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("final-C2S-packets")
        .title("Final C2S Packets")
        .description("The final server-to-client packets, which can be hooked and modified.")
        .filter(p -> PacketUtils.getC2SPackets().contains(p))
        .build()
    );
    
    private final Setting<Set<Class<? extends Packet<?>>>> finalS2CPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("final-S2C-packets")
        .title("Final S2C Packets")
        .description("The final server-to-client packets, which can be hooked and modified.")
        .filter(p -> PacketUtils.getS2CPackets().contains(p))
        .build()
    );
    
    public PacketDebugger() {
        super(Categories.MISC, "PacketDebugger", "Logging network packets.");
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
    
    @SuppressWarnings("unchecked")
    private void logPacket(Packet<?> packet) {
        String name = PacketUtils.getName((Class<? extends Packet<?>>) packet.getClass());
        // Outside a dev environment, field names are obfuscated and hard to retrieve.
        // Thus, the module runs in a limited mode here (skipping packet parameters).
        String params = FabricLoader.getInstance().isDevelopmentEnvironment()
            ? ReflectionToStringBuilder.toString(packet, ToStringStyle.NO_CLASS_NAME_STYLE)
            : StringUtils.EMPTY;
        
        if (Utils.canUpdate()) {
            info("[(highlight)%s(default)/(highlight)%s(default)] (highlight)%s(default)%s",
                packet.getPacketType().side(),
                packet.getPacketType().id(),
                name,
                params
            );
        } else {
            MeteorClient.LOG.info("[PacketDebugger] [{}/{}] {}",
                packet.getPacketType().side(),
                packet.getPacketType().id(),
                name + params
            );
        }
    }
    
}
