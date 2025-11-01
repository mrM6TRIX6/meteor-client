/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

public class GameJoinEvent {
    
    private static final GameJoinEvent INSTANCE = new GameJoinEvent();
    
    public static GameJoinEvent get() {
        return INSTANCE;
    }
    
}
