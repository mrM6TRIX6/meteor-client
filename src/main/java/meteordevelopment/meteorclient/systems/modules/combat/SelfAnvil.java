/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.game.ScreenOpenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;

public class SelfAnvil extends Module {
    
    public SelfAnvil() {
        super(Categories.Combat, "SelfAnvil", "Automatically places an anvil on you to prevent other players from going into your hole.");
    }
    
    @EventHandler
    private void onOpenScreen(ScreenOpenEvent event) {
        if (event.screen instanceof AnvilScreen) {
            event.cancel();
        }
    }
    
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (BlockUtils.place(mc.player.getBlockPos().add(0, 2, 0), InventoryUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof AnvilBlock), 0)) {
            toggle();
        }
    }
    
}
