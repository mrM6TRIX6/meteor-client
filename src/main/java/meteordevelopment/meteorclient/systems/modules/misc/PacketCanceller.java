/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

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

import java.util.Set;

public class PacketCanceller extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("C2S-packets")
        .title("C2S Packets")
        .description("Client-to-server packets to cancel.")
        .filter(p -> PacketUtils.getC2SPackets().contains(p))
        .build()
    );
    
    private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("S2C-packets")
        .title("S2C Packets")
        .description("Server-to-client packets to cancel.")
        .filter(p -> PacketUtils.getS2CPackets().contains(p))
        .build()
    );
    
    public PacketCanceller() {
        super(Categories.MISC, "PacketCanceller", "Allows you to cancel certain packets.");
        runInMainMenu = true;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onPacketSend(PacketEvent.Send event) {
        if (c2sPackets.get().contains(event.packet.getClass())) {
            event.cancel();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onPacketReceive(PacketEvent.Receive event) {
        if (s2cPackets.get().contains(event.packet.getClass())) {
            event.cancel();
        }
    }
    
}
