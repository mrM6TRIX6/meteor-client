/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient;

import net.minecraft.client.MinecraftClient;

/**
 * Global minecraft client accessor
 */
public interface IMinecraft {
    
    MinecraftClient mc = MinecraftClient.getInstance();
    
}
