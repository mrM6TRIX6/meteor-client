/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;

public class PermissionsCommand extends Command {
    
    public PermissionsCommand() {
        super("Permissions", "Shows the player permissions.", "Perms");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info(
                String.format("Your permission level: %s.",
                    Utils.formatPerms(mc.player.getPermissions())
                )
            );
            
            return SINGLE_SUCCESS;
        });
        
        builder.then(argument("player", PlayerArgumentType.create())
            .executes(context -> {
                PlayerEntity player = PlayerArgumentType.get(context, "player");
                
                info(
                    String.format("%s's permission level: %s.",
                        player.getGameProfile().name(),
                        Utils.formatPerms(player.getPermissions())
                    )
                );
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
}