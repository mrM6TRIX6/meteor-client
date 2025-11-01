
/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

public class CheckCMDCommand extends Command {
    
    private static final SimpleCommandExceptionType ALWAYS_CHECKING = new SimpleCommandExceptionType(Text.of("Already executing Command Check!"));
    
    private int checking = 0;
    
    public CheckCMDCommand() {
        super("checkcmd", "Checks if command blocks are active.");
    }
    
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (checking > 0) {
                throw ALWAYS_CHECKING.create();
            }
            
            MeteorClient.EVENT_BUS.subscribe(this);
            checking = 200;
            mc.player.networkHandler.sendPacket(new UpdateCommandBlockC2SPacket(mc.player.getBlockPos(), "", CommandBlockBlockEntity.Type.AUTO, false, false, false));
            info("Checking..");
            
            return SINGLE_SUCCESS;
        });
    }
    
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof GameMessageS2CPacket)) {
            return;
        }
        
        Text message = ((GameMessageS2CPacket) event.packet).content();
        if (message.getContent() instanceof TranslatableTextContent) {
            String key = ((TranslatableTextContent) message.getContent()).getKey();
            if (key.equals("advMode.notEnabled")) {
                info("Command blocks are deactivated.");
                event.cancel();
                this.checking = 0;
            } else if (key.equals("advMode.notAllowed") || key.equals("advMode.setCommand.success")) {
                info("Command blocks are activated.");
                event.cancel();
                this.checking = 0;
            }
        }
    }
    
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (this.checking == 1) {
            error("Server didnt send a response!");
        }
        if (this.checking < 1) {
            this.checking = 0;
            MeteorClient.EVENT_BUS.unsubscribe(this);
            return;
        }
        this.checking--;
    }
    
}
