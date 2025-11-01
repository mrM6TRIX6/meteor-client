/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.SpectatorTeleport;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.SpectatorTeleportC2SPacket;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SptpCommand extends Command {
    
    SpectatorTeleport spectatorTeleport = Modules.get().get(SpectatorTeleport.class);
    
    public SptpCommand() {
        super("sptp", "Teleports you with spectator mode teleport packet.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create())
            .executes(context -> {
                PlayerListEntry lookUpTarget = PlayerListEntryArgumentType.get(context);
                UUID uuid = lookUpTarget.getProfile().getId();
                sendTeleportPacket(uuid);
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
    private void sendTeleportPacket(UUID uuid) {
        String beforeTp = spectatorTeleport.beforeTp.get();
        String afterTp = spectatorTeleport.afterTp.get();
        int delayTime = spectatorTeleport.delayTime.get();
        boolean isDetectSpec = spectatorTeleport.isDetectSpec.get();
        boolean enableBefore = spectatorTeleport.enableBefore.get();
        boolean enableAfter = spectatorTeleport.enableAfter.get();
        
        if (mc.player.isSpectator() && isDetectSpec) {
            mc.player.networkHandler.sendPacket(new SpectatorTeleportC2SPacket(uuid));
        } else {
            if (enableBefore) {
                ChatUtils.sendPlayerMsg(beforeTp);
            }
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.schedule(() -> {
                mc.player.networkHandler.sendPacket(new SpectatorTeleportC2SPacket(uuid));
                if (enableAfter) {
                    ChatUtils.sendPlayerMsg(afterTp);
                }
                executor.shutdown();
            }, delayTime, TimeUnit.MILLISECONDS);
        }
    }
    
}
