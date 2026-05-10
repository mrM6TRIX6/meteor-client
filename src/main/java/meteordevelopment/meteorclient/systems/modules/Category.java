/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public enum Category implements IDisplayName {
    
    COMBAT("Combat"),
    PLAYER("Player"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    WORLD("World"),
    MISC("Misc"),
    EXPLOIT("Exploit"),
    FUN("Fun");
    
    public final String name;
    public final ItemStack icon;
    
    Category(String name, ItemStack icon) {
        this.name = name;
        this.icon = icon == null ? Items.AIR.getDefaultStack() : icon;
    }
    
    Category(String name) {
        this(name, null);
    }
    
    @Override
    public String getDisplayName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
}
