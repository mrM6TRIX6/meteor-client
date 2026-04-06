/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.misc.text.TextUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class UUIDCommand extends Command {
    
    public UUIDCommand() {
        super("UUID", "Shows the UUID of the player.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create())
            .executes(context -> {
                PlayerListEntry player = PlayerListEntryArgumentType.get(context, "player");
                String playerName = player.getProfile().name();
                String uuid = player.getProfile().id().toString();
                
                info(
                    Text.literal(playerName + "'s UUID: ").formatted(Formatting.GRAY)
                        .append(TextUtils.copyable(uuid).formatted(Formatting.WHITE))
                        .append(Text.literal(".").formatted(Formatting.GRAY))
                );
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
}