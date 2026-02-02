/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

public class EXPThrower extends Module {
    
    public EXPThrower() {
        super(Categories.PLAYER, "ExpThrower", "Automatically throws XP bottles from your hotbar.");
    }
    
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        FindItemResult exp = InventoryUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
        if (!exp.found()) {
            return;
        }
        
        Rotations.rotate(mc.player.getYaw(), 90, () -> {
            if (exp.getHand() != null) {
                mc.interactionManager.interactItem(mc.player, exp.getHand());
            } else {
                InventoryUtils.swap(exp.slot(), true);
                mc.interactionManager.interactItem(mc.player, exp.getHand());
                InventoryUtils.swapBack();
            }
        });
    }
    
}
