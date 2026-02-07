/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.CommandManager;
import meteordevelopment.meteorclient.systems.clientsettings.ClientSettings;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HelpCommand extends Command {
    
    public HelpCommand() {
        super("Help", "List of all commands.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ChatUtils.info("--- Commands ((highlight)%d(default)) ---", CommandManager.getCount());
            
            MutableText commands = Text.literal("");
            CommandManager.getAll().forEach(command -> commands.append(getCommandText(command)));
            ChatUtils.sendMsg(commands);
            
            return SINGLE_SUCCESS;
        });
    }
    
    private MutableText getCommandText(Command command) {
        // Hover tooltip
        MutableText tooltip = Text.literal("");
        
        tooltip.append(Text.literal(command.getName()).formatted(Formatting.BLUE, Formatting.BOLD)).append("\n");
        
        MutableText aliases = Text.literal(command.toString());
        if (!command.getAliases().isEmpty()) {
            aliases.append(", ");
            for (String alias : command.getAliases()) {
                if (alias.isEmpty()) {
                    continue;
                }
                aliases.append(ClientSettings.get().prefix.get() + alias);
                if (!alias.equalsIgnoreCase(command.getAliases().getLast())) {
                    aliases.append(", ");
                }
            }
        }
        tooltip.append(aliases.formatted(Formatting.GRAY)).append("\n\n");
        
        tooltip.append(Text.literal(command.getDescription()).formatted(Formatting.WHITE));
        
        // Text
        MutableText text = Text.literal(command.getName());
        if (command != CommandManager.getAll().getLast()) {
            text.append(Text.literal(", ").formatted(Formatting.GRAY));
        }
        text.setStyle(text
            .getStyle()
            .withHoverEvent(new HoverEvent.ShowText(tooltip))
            .withClickEvent(new ClickEvent.SuggestCommand(command.toString()))
        );
        
        return text;
    }
    
}
