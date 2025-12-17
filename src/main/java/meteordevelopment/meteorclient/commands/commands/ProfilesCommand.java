/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ProfileArgumentType;
import meteordevelopment.meteorclient.systems.configs.Config;
import meteordevelopment.meteorclient.systems.configs.Configs;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

public class ProfilesCommand extends Command {
    
    public ProfilesCommand() {
        super("profiles", "Manages profiles.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("load")
            .then(argument("profile", ProfileArgumentType.create())
                .executes(context -> {
                    Config config = ProfileArgumentType.get(context);
                    if (config != null) {
                        config.load();
                        info("Loaded config (highlight)%s(default).", config.name.get());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("save")
            .then(argument("profile", ProfileArgumentType.create())
                .executes(context -> {
                    Config config = ProfileArgumentType.get(context);
                    if (config != null) {
                        config.save();
                        info("Saved config (highlight)%s(default).", config.name.get());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("remove")
            .then(argument("profile", ProfileArgumentType.create())
                .executes(context -> {
                    Config config = ProfileArgumentType.get(context);
                    if (config != null) {
                        Configs.get().remove(config);
                        info("Deleted config (highlight)%s(default).", config.name.get());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("clear")
            .executes(context -> {
                Configs.get().clear();
                info("Configs has been cleared.");
                
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("list")
            .executes(context -> {
                ChatUtils.info("--- Configs ((highlight)%s(default)) ---", Configs.get().getCount());
                Configs.get().forEach(profile -> ChatUtils.info("(highlight)%s".formatted(profile.name)));
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
