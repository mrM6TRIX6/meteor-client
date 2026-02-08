/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.instrumentdetect;

import meteordevelopment.meteorclient.utils.misc.ITagged;
import net.minecraft.block.NoteBlock;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public enum InstrumentDetectMode implements ITagged {
    
    BLOCK_STATE("Block State", ((noteBlock, blockPos) -> noteBlock.get(NoteBlock.INSTRUMENT))),
    BELOW_BLOCK("Below Block", ((noteBlock, blockPos) -> mc.world.getBlockState(blockPos.down()).getInstrument()));
    
    private final String tag;
    private final InstrumentDetectFunction instrumentDetectFunction;
    
    InstrumentDetectMode(String tag, InstrumentDetectFunction instrumentDetectFunction) {
        this.tag = tag;
        this.instrumentDetectFunction = instrumentDetectFunction;
    }
    
    @Override
    public String getTag() {
        return tag;
    }
    
    public InstrumentDetectFunction getInstrumentDetectFunction() {
        return instrumentDetectFunction;
    }
    
}