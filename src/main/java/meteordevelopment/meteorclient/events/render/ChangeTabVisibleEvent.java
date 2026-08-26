/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

public class ChangeTabVisibleEvent {

    private static final ChangeTabVisibleEvent INSTANCE = new ChangeTabVisibleEvent();

    public boolean visible;

    public static ChangeTabVisibleEvent get(boolean visible) {
        INSTANCE.visible = visible;

        return INSTANCE;
    }

}
