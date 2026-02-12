/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.instrumentdetect;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import net.minecraft.block.NoteBlock;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public enum InstrumentDetectMode implements IDisplayName {
    
    BLOCK_STATE("Block State", ((noteBlock, blockPos) -> noteBlock.get(NoteBlock.INSTRUMENT))),
    BELOW_BLOCK("Below Block", ((noteBlock, blockPos) -> mc.world.getBlockState(blockPos.down()).getInstrument()));
    
    private final String displayName;
    private final InstrumentDetectFunction instrumentDetectFunction;
    
    InstrumentDetectMode(String displayName, InstrumentDetectFunction instrumentDetectFunction) {
        this.displayName = displayName;
        this.instrumentDetectFunction = instrumentDetectFunction;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    public InstrumentDetectFunction getInstrumentDetectFunction() {
        return instrumentDetectFunction;
    }
    
}