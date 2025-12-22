/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ClientTextArgumentType;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.world.RegistryUtils;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class DisconnectCommand extends Command {
    
    private final static SimpleCommandExceptionType NOT_IN_MULTIPLAYER = new SimpleCommandExceptionType(Text.literal("You must be in multiplayer to use this."));
    
    public DisconnectCommand() {
        super("Disconnect", "Disconnect from the server.", "Kick", "Quit");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("disconnect")
            .executes(context -> {
                checkMultiplayer();
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.translatable("multiplayer.disconnect.generic")));
                return SINGLE_SUCCESS;
            })
            .then(argument("reason", ClientTextArgumentType.text(RegistryUtils.REGISTRY_ACCESS))
                .executes(context -> {
                    checkMultiplayer();
                    mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(ClientTextArgumentType.get(context, "reason")));
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("position")
            .executes(context -> {
                checkMultiplayer();
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, !mc.player.isOnGround(), mc.player.horizontalCollision));
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("self_hurt")
            .executes(context -> {
                checkMultiplayer();
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(mc.player, mc.player.isSneaking()));
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("chat")
            .executes(context -> {
                checkMultiplayer();
                ChatUtils.sendPlayerMsg("§0§1§");
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("crash")
            .executes(context -> {
                checkMultiplayer();
                GlfwUtil.makeJvmCrash();
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("hotbar")
            .executes(context -> {
                checkMultiplayer();
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(-1));
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("self_interact")
            .executes(context -> {
                checkMultiplayer();
                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(mc.player, mc.player.isSneaking(), Hand.MAIN_HAND));
                return SINGLE_SUCCESS;
            })
        );
    }
    
    private void checkMultiplayer() throws CommandSyntaxException {
        if (mc.isIntegratedServerRunning()) {
            throw NOT_IN_MULTIPLAYER.create();
        }
    }
    
}
