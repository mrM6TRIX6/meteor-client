/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.item.ItemStack;

public class DropItemEvent extends Cancellable {
    
    private static final DropItemEvent INSTANCE = new DropItemEvent();
    
    public ItemStack itemStack;
    
    public static DropItemEvent get(ItemStack itemStack) {
        INSTANCE.setCancelled(false);
        INSTANCE.itemStack = itemStack;
        return INSTANCE;
    }
    
}
