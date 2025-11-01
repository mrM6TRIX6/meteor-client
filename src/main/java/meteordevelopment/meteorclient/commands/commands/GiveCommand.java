/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.world.RegistryUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class GiveCommand extends Command {
    
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(Text.literal("You must be in creative mode to use this."));
    
    public GiveCommand() {
        super("give", "Gives you any item.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("item", ItemStackArgumentType.itemStack(RegistryUtils.REGISTRY_ACCESS))
            .executes(context -> {
                if (!mc.player.getAbilities().creativeMode) {
                    throw NOT_IN_CREATIVE.create();
                }
                
                ItemStack itemStack = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false);
                giveItem(itemStack);
                
                return SINGLE_SUCCESS;
            })
            .then(argument("number", IntegerArgumentType.integer(1, 99))
                .executes(context -> {
                    if (!mc.player.getAbilities().creativeMode) {
                        throw NOT_IN_CREATIVE.create();
                    }
                    
                    ItemStack itemStack = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(IntegerArgumentType.getInteger(context, "number"), true);
                    giveItem(itemStack);
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
    }
    
    private void giveItem(ItemStack itemStack) {
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setStack(selectedSlot, itemStack);
        mc.interactionManager.clickCreativeStack(itemStack, 36 + selectedSlot);
    }
    
}
