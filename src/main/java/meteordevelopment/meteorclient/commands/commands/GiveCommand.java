/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
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
                giveItem(itemStack, findEmptySlot());
                
                return SINGLE_SUCCESS;
            })
            .then(argument("number", IntegerArgumentType.integer(1, 99))
                .executes(context -> {
                    if (!mc.player.getAbilities().creativeMode) {
                        throw NOT_IN_CREATIVE.create();
                    }
                    
                    ItemStack itemStack = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(IntegerArgumentType.getInteger(context, "number"), true);
                    giveItem(itemStack, findEmptySlot());
                    
                    return SINGLE_SUCCESS;
                })
                .then(argument("slot", IntegerArgumentType.integer(0, 40))
                    .executes(context -> {
                        if (!mc.player.getAbilities().creativeMode) {
                            throw NOT_IN_CREATIVE.create();
                        }
                        
                        ItemStack itemStack = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(IntegerArgumentType.getInteger(context, "number"), true);
                        int slot = IntegerArgumentType.getInteger(context, "slot");
                        giveItem(itemStack, slot);
                        
                        return SINGLE_SUCCESS;
                    })
                )
            )
        );
    }
    
    private int findEmptySlot() {
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        
        if (mc.player.getInventory().getSelectedStack().isEmpty()) {
            return selectedSlot;
        }
        
        FindItemResult fir = InventoryUtils.find(ItemStack::isEmpty, 0, 35);
        return fir.found() ? fir.slot() : selectedSlot;
    }
    
    private void giveItem(ItemStack itemStack, int slot) {
        mc.player.getInventory().setStack(slot, itemStack);
        mc.interactionManager.clickCreativeStack(itemStack, SlotUtils.creativeInventory(slot));
    }
    
}
