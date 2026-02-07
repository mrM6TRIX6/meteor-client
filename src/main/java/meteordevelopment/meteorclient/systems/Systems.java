/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.clientsettings.ClientSettings;
import meteordevelopment.meteorclient.systems.configs.Configs;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Systems {
    
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends System>, System<?>> systems = new Reference2ReferenceOpenHashMap<>();
    private static final List<Runnable> preLoadTasks = new ArrayList<>(1);
    
    public static void addPreLoadTask(Runnable task) {
        preLoadTasks.add(task);
    }
    
    public static void init() {
        // Has to be loaded first so the hidden modules list in Client Settings tab can load modules
        add(new Modules());
        
        ClientSettings clientSettings = new ClientSettings();
        System<?> clientSettingsSystem = add(clientSettings);
        clientSettingsSystem.init();
        clientSettingsSystem.load();
        
        // Registers the colors from client settings tab. This allows rainbow colours to work for friends.
        clientSettings.settings.registerColorSettings(null);
        
        add(new Macros());
        add(new Friends());
        add(new Accounts());
        add(new Configs());
        add(new Proxies());
        
        MeteorClient.EVENT_BUS.subscribe(Systems.class);
    }
    
    private static System<?> add(System<?> system) {
        systems.put(system.getClass(), system);
        MeteorClient.EVENT_BUS.subscribe(system);
        system.init();
        
        return system;
    }
    
    // save/load
    
    @EventHandler
    private static void onGameLeft(GameLeftEvent event) {
        save();
    }
    
    public static void save() {
        long start = java.lang.System.currentTimeMillis();
        MeteorClient.LOG.info("Saving");
        
        for (System<?> system : systems.values()) {
            system.save();
        }
        
        MeteorClient.LOG.info("Saved in {} milliseconds.", java.lang.System.currentTimeMillis() - start);
    }
    
    public static void load() {
        long start = java.lang.System.currentTimeMillis();
        MeteorClient.LOG.info("Loading");
        
        for (Runnable task : preLoadTasks) {
            task.run();
        }
        for (System<?> system : systems.values()) {
            system.load();
        }
        
        MeteorClient.LOG.info("Loaded in {} milliseconds", java.lang.System.currentTimeMillis() - start);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends System<?>> T get(Class<T> clazz) {
        return (T) systems.get(clazz);
    }
    
}
