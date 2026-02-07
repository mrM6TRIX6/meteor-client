/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ConfigArgumentType;
import meteordevelopment.meteorclient.config.Config;
import meteordevelopment.meteorclient.config.ConfigManager;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

public class ConfigsCommand extends Command {
    
    public ConfigsCommand() {
        super("Configs", "Manages configs.", "cfg");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("load")
            .then(argument("config", ConfigArgumentType.create())
                .executes(context -> {
                    Config config = ConfigArgumentType.get(context, "config");
                    if (config != null) {
                        config.load();
                        info("Loaded config (highlight)%s(default).", config.name.get());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("save")
            .then(argument("config", ConfigArgumentType.create())
                .executes(context -> {
                    Config config = ConfigArgumentType.get(context, "config");
                    if (config != null) {
                        config.save();
                        info("Saved config (highlight)%s(default).", config.name.get());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("remove")
            .then(argument("config", ConfigArgumentType.create())
                .executes(context -> {
                    Config config = ConfigArgumentType.get(context, "config");
                    if (config != null) {
                        ConfigManager.get().remove(config);
                        info("Deleted config (highlight)%s(default).", config.name.get());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("clear")
            .executes(context -> {
                ConfigManager.get().clear();
                info("Configs has been cleared.");
                
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("list")
            .executes(context -> {
                ChatUtils.info("--- Configs ((highlight)%s(default)) ---", ConfigManager.get().getCount());
                ConfigManager.get().forEach(cfg -> ChatUtils.info("(highlight)%s".formatted(cfg.name)));
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
