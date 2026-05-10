/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerModelPart;

import java.util.Set;

public class UndressCommand extends Command {
    
    private static final Set<PlayerModelPart> PARTS = Set.of(
        PlayerModelPart.JACKET,
        PlayerModelPart.LEFT_PANTS_LEG,
        PlayerModelPart.RIGHT_PANTS_LEG,
        PlayerModelPart.LEFT_SLEEVE,
        PlayerModelPart.RIGHT_SLEEVE
    );
    
    public UndressCommand() {
        super("Undress", "Undresses you (fun test command).");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            boolean flag = mc.options.isPlayerModelPartEnabled(PlayerModelPart.JACKET);
            PARTS.forEach(p -> mc.options.setPlayerModelPart(p, !flag));
            mc.options.sendClientSettings();
            return SINGLE_SUCCESS;
        });
    }
    
}