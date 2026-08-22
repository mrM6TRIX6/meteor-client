/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;

public class MemoryCommand extends Command {
    
    public MemoryCommand() {
        super("Memory", "Call System.gc()", "gc");
    }
    
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            System.gc();
            return SINGLE_SUCCESS;
        });
    }
    
}
