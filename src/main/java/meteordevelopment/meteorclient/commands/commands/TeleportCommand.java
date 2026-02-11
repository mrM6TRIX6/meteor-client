/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.commands.arguments.PositionArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class TeleportCommand extends Command {
    
    
    public TeleportCommand() {
        super("Teleport", "Sends a packet to the server with new position. Allows to teleport small distances.", "tp");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("pos", PositionArgumentType.pos())
            .executes(context -> {
                Vec3d pos = PositionArgumentType.getPos(context, "pos");
                
                mc.player.updatePosition(
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
                );
                
                return SINGLE_SUCCESS;
            })
            .then(argument("yaw", FloatArgumentType.floatArg())
                .then(argument("pitch", FloatArgumentType.floatArg())
                    .executes(context -> {
                        Vec3d pos = PositionArgumentType.getPos(context, "pos");
                        float yaw = FloatArgumentType.getFloat(context, "yaw");
                        float pitch = FloatArgumentType.getFloat(context, "pitch");
                        
                        mc.player.updatePositionAndAngles(
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            yaw,
                            pitch
                        );
                        
                        return SINGLE_SUCCESS;
                    })
                )
            )
        );
        
        builder.then(argument("player", PlayerArgumentType.create())
            .executes(context -> {
                PlayerEntity player = PlayerArgumentType.get(context, "player");
                
                mc.player.updatePositionAndAngles(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYaw(),
                    player.getPitch()
                );
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
