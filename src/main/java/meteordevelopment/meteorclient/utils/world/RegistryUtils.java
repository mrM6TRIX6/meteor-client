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
    
    /**
     * Static registry access for command argument types that rely on Minecraft registries.
     * <p>
     * Argument types requiring registry data (e.g., block, item, or entity types) use this
     * {@link CommandRegistryAccess} object. Since dynamic registries are server-specific,
     * this field must be updated with each server join to ensure proper registry access.
     * <p>
     * The command tree and {@link CommandDispatcher} require rebuilding when registries change because:
     * <ol>
     * <li>Registry-dependent argument types create and store registry wrapper objects during command tree building
     * <li>Registry entries use referential equality - wrapper objects become stale after server changes
     * <li>CommandDispatcher node merging cannot replace existing stale argument type objects
     * </ol>
     * <p>
     * Initialized with built-in registries and updated in {@link #onGameJoin(GameJoinEvent)} with the
     * current world's registry manager and enabled features.
     *
     * @see #onGameJoin(GameJoinEvent)
     */
    public static CommandRegistryAccess REGISTRY_ACCESS = CommandManager.createRegistryAccess(BuiltinRegistries.createWrapperLookup());
    
    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(RegistryUtils.class);
    }
    
    /**
     * Updates {@link #REGISTRY_ACCESS} with the current world's registry manager when joining a new world/server.
     * This ensures command argument types have access to the correct server-specific dynamic registries.
     */
    @EventHandler
    private static void onGameJoin(GameJoinEvent event) {
        REGISTRY_ACCESS = CommandRegistryAccess.of(mc.world.getRegistryManager(), mc.world.getEnabledFeatures());
    }
    
}
