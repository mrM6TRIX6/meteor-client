/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;

public class ToggleCommand extends Command {
    
    public ToggleCommand() {
        super("Toggle", "Toggles a module.", "t");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("all")
            .then(literal("on")
                .executes(context -> {
                    new ArrayList<>(Modules.get().getAll()).forEach(Module::enable);
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("off")
                .executes(context -> {
                    new ArrayList<>(Modules.get().getActive()).forEach(Module::toggle);
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(argument("module", ModuleArgumentType.create())
            .executes(context -> {
                Module m = ModuleArgumentType.get(context, "module");
                m.toggle();
                m.sendToggledMsg();
                return SINGLE_SUCCESS;
            })
            .then(literal("on")
                .executes(context -> {
                    Module m = ModuleArgumentType.get(context, "module");
                    m.enable();
                    return SINGLE_SUCCESS;
                }))
            .then(literal("off")
                .executes(context -> {
                    Module m = ModuleArgumentType.get(context, "module");
                    m.disable();
                    return SINGLE_SUCCESS;
                })
            )
        );
    }
    
}
