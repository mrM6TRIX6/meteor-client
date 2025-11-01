/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import com.mojang.brigadier.CommandDispatcher;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.server.command.CommandManager;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class RegistryUtils {
    
    public static CommandRegistryAccess REGISTRY_ACCESS = CommandManager.createRegistryAccess(BuiltinRegistries.createWrapperLookup());
    
    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(RegistryUtils.class);
    }
    
    /**
     * Argument types that rely on Minecraft registries access those registries through a {@link CommandRegistryAccess}
     * object. Since dynamic registries are specific to each server, we need to make a new CommandRegistryAccess object
     * every time we join a server.
     * <p>
     * The command tree and by extension the {@link CommandDispatcher} also have to be rebuilt because:
     * <ol>
     * <li>Argument types that require registries use a registry wrapper object that is created and stored in the
     *     argument type objects when the command tree is built.
     * <li>Registry entries and keys are compared using referential equality. Even if the data encoded is the same,
     *     registry wrapper objects' dynamic data becomes stale after joining another server.
     * <li>The CommandDispatcher's node merging only adds missing children, it cannot replace stale argument type
     *     objects.
     * </ol>
     */
    @EventHandler
    private static void onGameJoin(GameJoinEvent event) {
        REGISTRY_ACCESS = CommandRegistryAccess.of(mc.world.getRegistryManager(), mc.world.getEnabledFeatures());
    }
    
}
