/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.EnumSetting;
import meteordevelopment.meteorclient.settings.impl.StringListSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;

import java.util.List;

public class CommandAura extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("message")
        .description("The specified message sent to the server.")
        .defaultValue("/msg %target% hi from %me%")
        .build()
    );
    
    private final Setting<Target> targetMode = sgGeneral.add(new EnumSetting.Builder<Target>()
        .name("target")
        .description("Only targets selected target.")
        .defaultValue(Target.EVERYONE)
        .build()
    );
    
    private final Setting<Boolean> toggleOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-death")
        .description("Disables when you die.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Boolean> toggleOnLog = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-log")
        .description("Disables when you disconnect from a server.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Boolean> isLogs = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-logs")
        .description("Show logs in chat.")
        .defaultValue(false)
        .build()
    );
    
    public CommandAura() {
        super(Categories.Misc, "command-aura", "Sends a message when players come in render distance.");
    }
    
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (toggleOnLog.get()) {
            toggle();
        }
    }
    
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof DeathMessageS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.playerId());
            if (entity == mc.player && toggleOnDeath.get()) {
                toggle();
                info("Toggled off because you died.");
            }
        }
    }
    
    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof PlayerEntity) || event.entity.getUuid().equals(mc.player.getUuid())) {
            return;
        }

        String targetName = event.entity.getName().getString();
        
        switch (targetMode.get()) {
            
            case EVERYONE -> {
                for (String msg : messages.get()) {
                    ChatUtils.sendPlayerMsg(msg.replaceAll("%target%", targetName).replaceAll("%me%", EntityUtils.getName(mc.player)));
                }
                if (isLogs.get()) {
                    info("Used command on (highlight)%s(default).", targetName);
                }
            }
            
            case ONLY_FRIENDS -> {
                if (!Friends.get().isFriend((PlayerEntity) event.entity)) {
                    return;
                }
                for (String msg : messages.get()) {
                    ChatUtils.sendPlayerMsg(msg.replaceAll("%target%", targetName).replaceAll("%me%", EntityUtils.getName(mc.player)));
                }
                if (isLogs.get()) {
                    info("Used command on (highlight)%s(default).", targetName);
                }
            }
            
            case IGNORE_FRIENDS -> {
                if (Friends.get().isFriend((PlayerEntity) event.entity)) {
                    return;
                }
                for (String msg : messages.get()) {
                    ChatUtils.sendPlayerMsg(msg.replaceAll("%target%", targetName).replaceAll("%me%", EntityUtils.getName(mc.player)));
                }
                if (isLogs.get()) {
                    info("Used command on (highlight)%s(default).", targetName);
                }
            }
        }
    }
    
    private enum Target {
        
        EVERYONE,
        ONLY_FRIENDS,
        IGNORE_FRIENDS
        
    }
    
}
