/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.Commands;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class PrefixCommand extends Command {
    
    private final static SimpleCommandExceptionType EMPTY_PREFIX = new SimpleCommandExceptionType(Text.literal("Empty command prefix"));
    private final static SimpleCommandExceptionType CONFLICT_PREFIX = new SimpleCommandExceptionType(Text.literal("You have set your command prefix to '/', which is used by Minecraft"));
    private final static DynamicCommandExceptionType TOO_LONG_PREFIX = new DynamicCommandExceptionType(prefix -> Text.literal("Command prefix '%s' too long".formatted(prefix)));
    
    public PrefixCommand() {
        super("Prefix", "Changes command prefix.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("prefix", StringArgumentType.string())
            .executes(context -> {
                String prefix = StringArgumentType.getString(context, "prefix");
                
                if (prefix.isBlank()) {
                    throw EMPTY_PREFIX.create();
                } else if (prefix.startsWith("/")) {
                    throw CONFLICT_PREFIX.create();
                } else if (prefix.length() > 7) {
                    throw TOO_LONG_PREFIX.create(prefix);
                }
                
                Commands.get().setPrefix(prefix);
                info("Command prefix has been successfully set to '%s'".formatted(prefix));
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
