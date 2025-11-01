/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.world;

import net.minecraft.world.chunk.WorldChunk;

/**
 * @author Crosby
 */
public record ChunkDataEvent(WorldChunk chunk) {}
