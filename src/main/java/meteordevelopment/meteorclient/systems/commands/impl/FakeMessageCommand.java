/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.TextArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class FakeMessageCommand extends Command {
    
    public FakeMessageCommand() {
        super("FakeMessage", "Sends a fake message in your chat.", "fm");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("message", TextArgumentType.text(REGISTRY_ACCESS))
            .executes(context -> {
                Text message = TextArgumentType.get(context, "message");
                mc.execute(() -> ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(message, 0));
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
