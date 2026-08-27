/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import meteordevelopment.meteorclient.gui.tabs.impl.*;
import meteordevelopment.meteorclient.utils.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.reflect.PreInit;

import java.util.ArrayList;
import java.util.List;

public class Tabs {
    
    private static final Reference2ReferenceLinkedOpenHashMap<Class<? extends Tab>, Tab> tabInstances = new Reference2ReferenceLinkedOpenHashMap<>();
    
    private Tabs() {}
    
    @PreInit(dependencies = PathManagers.class)
    public static void init() {
        add(new ModulesTab());
        add(new ConfigsTab());
        add(new FriendsTab());
        add(new MacrosTab());
        
        if (PathManagers.get().getSettings().get().sizeGroups() > 0) {
            add(new PathManagerTab());
        }
    }
    
    public static void add(Tab tab) {
        tabInstances.put(tab.getClass(), tab);
    }
    
    public static Tab get(Class<? extends Tab> clazz) {
        return tabInstances.get(clazz);
    }
    
    public static List<Tab> getAll() {
        return new ArrayList<>(tabInstances.values());
    }
    
}
