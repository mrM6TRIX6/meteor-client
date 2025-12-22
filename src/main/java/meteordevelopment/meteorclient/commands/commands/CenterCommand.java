/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.command.CommandSource;

public class CenterCommand extends Command {
    
    public CenterCommand() {
        super("Center", "Centers the player on a block.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("middle")
            .executes(context -> {
                PlayerUtils.centerPlayer(true);
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("center")
            .executes(context -> {
                PlayerUtils.centerPlayer(false);
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
