/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import net.minecraft.item.Items;

public class Categories {
    
    public static final Category COMBAT = new Category("Combat", Items.DIAMOND_SWORD.getDefaultStack());
    public static final Category PLAYER = new Category("Player", Items.CRAFTING_TABLE.getDefaultStack());
    public static final Category MOVEMENT = new Category("Movement", Items.IRON_BOOTS.getDefaultStack());
    public static final Category RENDER = new Category("Render", Items.BEACON.getDefaultStack());
    public static final Category WORLD = new Category("World", Items.GRASS_BLOCK.getDefaultStack());
    public static final Category MISC = new Category("Misc", Items.LAVA_BUCKET.getDefaultStack());
    public static final Category EXPLOIT = new Category("Exploit", Items.TNT.getDefaultStack());
    public static final Category FUN = new Category("Fun", Items.FIREWORK_ROCKET.getDefaultStack());
    
    private static boolean registering;
    
    public static void init() {
        registering = true;
        
        // Meteor
        Modules.registerCategory(COMBAT);
        Modules.registerCategory(PLAYER);
        Modules.registerCategory(MOVEMENT);
        Modules.registerCategory(RENDER);
        Modules.registerCategory(WORLD);
        Modules.registerCategory(MISC);
        Modules.registerCategory(EXPLOIT);
        Modules.registerCategory(FUN);
        
        // TODO: Delete it
        // Addons
        AddonManager.ADDONS.forEach(MeteorAddon::onRegisterCategories);
        
        registering = false;
    }
    
    public static boolean isRegistering() {
        return registering;
    }
    
}
