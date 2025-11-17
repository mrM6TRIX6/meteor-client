/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class EquipCommand extends Command {
    
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(Text.literal("You must be in creative mode to use this."));
    
    public EquipCommand() {
        super("equip", "Put any item in any armor slot.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("head")
            .executes(context -> {
                equip(EquipmentSlot.HEAD);
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("chest")
            .executes(context -> {
                equip(EquipmentSlot.CHEST);
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("legs")
            .executes(context -> {
                equip(EquipmentSlot.LEGS);
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("feet")
            .executes(context -> {
                equip(EquipmentSlot.FEET);
                return SINGLE_SUCCESS;
            })
        );
    }
    
    private void equip(EquipmentSlot slot) throws CommandSyntaxException {
        if (!mc.player.getAbilities().creativeMode) {
            throw NOT_IN_CREATIVE.create();
        }
        
        // I don't know why but at least with ClientPlayerInteractionManager#clickCreativeStack
        // they are placed in the reverse order
        // 5 = crafting slot + crafting result slot
        int armorSlotId = Math.abs(slot.getOffsetEntitySlotId(-3)) + 5;
        
        InventoryUtils.clickCreativeStack(mc.player.getMainHandStack(), armorSlotId, true);
        InventoryUtils.clickCreativeStack(mc.player.getEquippedStack(slot), mc.player.getInventory().getSelectedSlot());
    }
    
}
