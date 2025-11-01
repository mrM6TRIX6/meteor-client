/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;

public class UUIDCommand extends Command {
    
    public UUIDCommand() {
        super("uuid", "Shows the UUID of the player.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create())
            .executes(context -> {
                PlayerListEntry player = PlayerListEntryArgumentType.get(context);
                info("%s's UUID: %s.", player.getProfile().getName(), player.getProfile().getId());
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
