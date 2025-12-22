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

public class GamemodeCommand extends Command {
    
    public GamemodeCommand() {
        super("GameMode", "Changes your gamemode client-side.", "GM");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("game_mode", GameModeArgumentType.create())
            .executes(context -> {
                GameMode gameMode = GameModeArgumentType.get(context, "game_mode");
                mc.interactionManager.setGameMode(gameMode);
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
