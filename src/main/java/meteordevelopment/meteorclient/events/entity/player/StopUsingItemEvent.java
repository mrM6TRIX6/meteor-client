/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

import net.minecraft.item.ItemStack;

public class StopUsingItemEvent {
    
    private static final StopUsingItemEvent INSTANCE = new StopUsingItemEvent();
    
    public ItemStack itemStack;
    
    public static StopUsingItemEvent get(ItemStack itemStack) {
        INSTANCE.itemStack = itemStack;
        return INSTANCE;
    }
    
}
