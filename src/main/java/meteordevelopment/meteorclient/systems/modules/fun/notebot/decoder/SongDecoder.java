/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun.notebot.decoder;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.fun.notebot.Notebot;
import meteordevelopment.meteorclient.systems.modules.fun.notebot.song.Song;

import java.io.File;

public abstract class SongDecoder {
    
    protected Notebot notebot = Modules.get().get(Notebot.class);
    
    /**
     * Parse file to a {@link Song} object
     *
     * @param file Song file
     *
     * @return A {@link Song} object
     */
    public abstract Song parse(File file) throws Exception;
    
}
