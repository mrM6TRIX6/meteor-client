/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.instrumentdetect;

import net.minecraft.block.NoteBlock;
import net.minecraft.client.MinecraftClient;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public enum InstrumentDetectMode {
    
    BLOCK_STATE(((noteBlock, blockPos) -> noteBlock.get(NoteBlock.INSTRUMENT))),
    BELOW_BLOCK(((noteBlock, blockPos) -> mc.world.getBlockState(blockPos.down()).getInstrument()));
    
    private final InstrumentDetectFunction instrumentDetectFunction;
    
    InstrumentDetectMode(InstrumentDetectFunction instrumentDetectFunction) {
        this.instrumentDetectFunction = instrumentDetectFunction;
    }
    
    public InstrumentDetectFunction getInstrumentDetectFunction() {
        return instrumentDetectFunction;
    }
    
}
