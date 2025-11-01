/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ProfileArgumentType;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
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
                    Profile profile = ProfileArgumentType.get(context);
                    if (profile != null) {
                        profile.load();
                        info("Loaded profile (highlight)%s(default).", profile.name.get());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("save")
            .then(argument("profile", ProfileArgumentType.create())
                .executes(context -> {
                    Profile profile = ProfileArgumentType.get(context);
                    if (profile != null) {
                        profile.save();
                        info("Saved profile (highlight)%s(default).", profile.name.get());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("remove")
            .then(argument("profile", ProfileArgumentType.create())
                .executes(context -> {
                    Profile profile = ProfileArgumentType.get(context);
                    if (profile != null) {
                        Profiles.get().remove(profile);
                        info("Deleted profile (highlight)%s(default).", profile.name.get());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("clear")
            .executes(context -> {
                Profiles.get().clear();
                info("Profiles has been cleared.");
                
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("list")
            .executes(context -> {
                ChatUtils.info("--- Profiles ((highlight)%s(default)) ---", Profiles.get().getCount());
                Profiles.get().forEach(profile -> ChatUtils.info("(highlight)%s".formatted(profile.name)));
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
}
