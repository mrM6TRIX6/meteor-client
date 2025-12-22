/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.misc.text.MeteorClickEvent;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.text.HoverEvent;
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
                String playerName = player.getProfile().getName();
                String uuid = player.getProfile().getId().toString();
                
                Text message = createUUIDMessage(playerName, uuid);
                info(message);
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
    private Text createUUIDMessage(String playerName, String uuid) {
        Text uuidText = Text.literal(uuid)
            .styled(style -> style
                .withColor(Formatting.WHITE)
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy")))
                .withClickEvent(new MeteorClickEvent.CopyToClipboard(uuid))
            );
        
        return Text.literal(playerName + "'s UUID: ")
            .styled(style -> style.withColor(Formatting.GRAY))
            .append(uuidText)
            .append(Text.literal(".").styled(style -> style.withColor(Formatting.GRAY)));
    }
}