/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.GameModeArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.world.GameMode;

public class GameModeCommand extends Command {
    
    public GameModeCommand() {
        super("GameMode", "Changes your game mode client-side.", "GM");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("gamemode", GameModeArgumentType.create())
            .executes(context -> {
                GameMode gameMode = GameModeArgumentType.get(context, "gamemode");
                mc.interactionManager.setGameMode(gameMode);
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
