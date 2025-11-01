/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class ClearInventoryCommand extends Command {
    
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(Text.literal("You must be in creative mode to use this."));
    
    public ClearInventoryCommand() {
        super("clear-inventory", "Clears inventory in creative mode.", "clear", "ci");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (!mc.player.getAbilities().creativeMode) {
                throw NOT_IN_CREATIVE.create();
            }
            
            for (int i = 0; i < mc.player.playerScreenHandler.getStacks().size(); ++i) {
                mc.player.playerScreenHandler.getSlot(i).setStackNoCallbacks(ItemStack.EMPTY);
                mc.interactionManager.clickCreativeStack(ItemStack.EMPTY, i);
            }
            
            return SINGLE_SUCCESS;
        });
    }
    
}
