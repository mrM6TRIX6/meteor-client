/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

public class ResetCommand extends Command {
    
    public ResetCommand() {
        super("Reset", "Resets specified settings.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("settings")
            .then(argument("module", ModuleArgumentType.create())
                .executes(context -> {
                    Module module = context.getArgument("module", Module.class);
                    module.settings.forEach(group -> group.forEach(Setting::reset));
                    module.info("Reset all settings.");
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("all")
                .executes(context -> {
                    Modules.get().getAll().forEach(module -> module.settings.forEach(group -> group.forEach(Setting::reset)));
                    ChatUtils.infoPrefix("Modules", "Reset all module settings");
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("gui")
            .executes(context -> {
                GuiConstants.clearWindowConfigs();
                ChatUtils.info("Reset GUI positioning.");
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("bind")
            .then(argument("module", ModuleArgumentType.create())
                .executes(context -> {
                    Module module = context.getArgument("module", Module.class);
                    
                    module.keybind.reset();
                    module.info("Reset bind.");
                    
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("all")
                .executes(context -> {
                    Modules.get().getAll().forEach(module -> module.keybind.reset());
                    ChatUtils.infoPrefix("Modules", "Reset all binds.");
                    return SINGLE_SUCCESS;
                })
            )
        );
    }
    
}
