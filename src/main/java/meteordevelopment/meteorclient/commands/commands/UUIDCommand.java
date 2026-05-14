/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.text.TextUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;

public class UUIDCommand extends Command {
    
    private static final SimpleCommandExceptionType NOT_TARGETING_ENTITY = new SimpleCommandExceptionType(Text.literal("You must be targeting an entity with your crosshair."));
    
    public UUIDCommand() {
        super("UUID", "Shows the UUID of the player.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (mc.crosshairTarget instanceof EntityHitResult result) {
                Entity entity = result.getEntity();
                String uuid = entity.getUuidAsString();
                
                info(
                    Text.literal(entity.getName().getString() + "'s UUID: ").formatted(Formatting.GRAY)
                        .append(TextUtils.copyable(uuid).formatted(Formatting.WHITE))
                        .append(Text.literal(".").formatted(Formatting.GRAY))
                );
                
                return SINGLE_SUCCESS;
            } else {
                throw NOT_TARGETING_ENTITY.create();
            }
        });
        
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