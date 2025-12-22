/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ClientTextArgumentType;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.utils.world.RegistryUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class FakeMessageCommand extends Command {
    
    public FakeMessageCommand() {
        super("FakeMessage", "Sends a fake message in your chat.", "FM");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("message", ClientTextArgumentType.text(RegistryUtils.REGISTRY_ACCESS))
            .executes(context -> {
                Text message = ClientTextArgumentType.get(context, "message");
                ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(message, 0);
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
