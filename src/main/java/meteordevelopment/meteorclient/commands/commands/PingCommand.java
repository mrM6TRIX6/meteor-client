/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;

public class PingCommand extends Command {
    
    public PingCommand() {
        super("ping", "Check your ping.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            int ping = EntityUtils.getPing(mc.player);
            Formatting color;
            
            if (ping <= 150) {
                color = Formatting.GREEN;
            } else if (ping <= 300) {
                color = Formatting.YELLOW;
            } else {
                color = Formatting.RED;
            }
            
            info("Ping: %s%d.", color, ping);
            
            return SINGLE_SUCCESS;
        });
    }
    
}
