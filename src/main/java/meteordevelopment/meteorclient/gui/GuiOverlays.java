/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Widgets which need to draw on top of the whole widget tree (dropdowns, completion popups) queue themselves here
 * while they render, {@link WidgetScreen} runs the queue once the tree is done.
 */
public class GuiOverlays {

    private GuiOverlays() {}

    private static final List<Runnable> TASKS = new ArrayList<>();

    public static void add(Runnable task) {
        TASKS.add(task);
    }

    public static void render() {
        // Overlays can queue further overlays, so don't use an iterator.
        for (int i = 0; i < TASKS.size(); i++) {
            TASKS.get(i).run();
        }

        TASKS.clear();
    }

    public static void clear() {
        TASKS.clear();
    }

}
