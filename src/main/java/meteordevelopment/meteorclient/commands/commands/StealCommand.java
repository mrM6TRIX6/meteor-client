/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.Text;

public class StealCommand extends Command {
    
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(Text.literal("You must be in creative mode to use this."));
    private final static DynamicCommandExceptionType EMPTY_SLOT = new DynamicCommandExceptionType(slot -> Text.literal("Player doesn't have an item in " + slot + " slot."));
    
    public StealCommand() {
        super("steal", "Steals an item from the player's equipment slot.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerArgumentType.create())
            .executes(context -> {
                steal(PlayerArgumentType.get(context), EquipmentSlot.MAINHAND);
                return SINGLE_SUCCESS;
            })
            .then(literal("mainhand")
                .executes(context -> {
                    steal(PlayerArgumentType.get(context), EquipmentSlot.MAINHAND);
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("offhand")
                .executes(context -> {
                    steal(PlayerArgumentType.get(context), EquipmentSlot.OFFHAND);
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("head")
                .executes(context -> {
                    steal(PlayerArgumentType.get(context), EquipmentSlot.HEAD);
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("chest")
                .executes(context -> {
                    steal(PlayerArgumentType.get(context), EquipmentSlot.CHEST);
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("legs")
                .executes(context -> {
                    steal(PlayerArgumentType.get(context), EquipmentSlot.LEGS);
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("feet")
                .executes(context -> {
                    steal(PlayerArgumentType.get(context), EquipmentSlot.FEET);
                    return SINGLE_SUCCESS;
                })
            )
        );
    }
    
    private void steal(PlayerEntity player, EquipmentSlot slot) throws CommandSyntaxException {
        if (!mc.player.getAbilities().creativeMode) {
            throw NOT_IN_CREATIVE.create();
        }
        
        ItemStack itemStack = player.getEquippedStack(slot).copy();
        if (itemStack.isEmpty()) {
            throw EMPTY_SLOT.create(slot.getName());
        }
        
        InventoryUtils.clickCreativeStack(itemStack, InventoryUtils.findEmptyGive());
    }
    
}
