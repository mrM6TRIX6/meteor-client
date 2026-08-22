/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.utils;

import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class SettingsWidgetFactory {

    private static final Map<Class<?>, Supplier<Factory>> customFactories = new HashMap<>();

    protected final Map<Class<?>, Factory> factories = new HashMap<>();

    /**
     * {@code SettingsWidgetFactory.registerCustomFactory(SomeSetting.class, () -> (table, setting) -> {//create
     * widget})}
     */
    public static void registerCustomFactory(Class<?> settingClass, Supplier<Factory> factorySupplier) {
        customFactories.put(settingClass, factorySupplier);
    }

    public static void unregisterCustomFactory(Class<?> settingClass) {
        customFactories.remove(settingClass);
    }

    public abstract WWidget create(Settings settings, String filter);

    protected Factory getFactory(Class<?> settingClass) {
        if (customFactories.containsKey(settingClass)) {
            return customFactories.get(settingClass).get();
        }
        return factories.get(settingClass);
    }

    @FunctionalInterface
    public interface Factory {

        void create(WTable table, Setting<?> setting);

    }

}
