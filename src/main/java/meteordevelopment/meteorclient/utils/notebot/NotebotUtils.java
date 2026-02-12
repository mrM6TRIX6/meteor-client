/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import meteordevelopment.meteorclient.utils.notebot.instrumentdetect.InstrumentDetectFunction;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class NotebotUtils {
    
    public static Note getNoteFromNoteBlock(BlockState noteBlock, BlockPos blockPos, NotebotMode mode, InstrumentDetectFunction instrumentDetectFunction) {
        NoteBlockInstrument instrument = null;
        int level = noteBlock.get(NoteBlock.NOTE);
        if (mode == NotebotMode.EXACT_INSTRUMENTS) {
            instrument = instrumentDetectFunction.detectInstrument(noteBlock, blockPos);
        }
        
        return new Note(instrument, level);
    }
    
    public enum NotebotMode implements IDisplayName {
        
        ANY_INSTRUMENT("Any Instrument"),
        EXACT_INSTRUMENTS("Exact Instruments");
        
        private final String displayName;
        
        NotebotMode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
    public enum OptionalInstrument implements IDisplayName {
        
        NONE("None", null),
        HARP("Harp", NoteBlockInstrument.HARP),
        BASEDRUM("Basedrum", NoteBlockInstrument.BASEDRUM),
        SNARE("Snare", NoteBlockInstrument.SNARE),
        HAT("Hat", NoteBlockInstrument.HAT),
        BASS("Bass", NoteBlockInstrument.BASS),
        FLUTE("Flute", NoteBlockInstrument.FLUTE),
        BELL("Bell", NoteBlockInstrument.BELL),
        GUITAR("Guitar", NoteBlockInstrument.GUITAR),
        CHIME("Chime", NoteBlockInstrument.CHIME),
        XYLOPHONE("Xylophone", NoteBlockInstrument.XYLOPHONE),
        IRON_XYLOPHONE("Iron Xylophone", NoteBlockInstrument.IRON_XYLOPHONE),
        COW_BELL("Cow Bell", NoteBlockInstrument.COW_BELL),
        DIDGERIDOO("Didgeridoo", NoteBlockInstrument.DIDGERIDOO),
        BIT("Bit", NoteBlockInstrument.BIT),
        BANJO("Banjo", NoteBlockInstrument.BANJO),
        PLING("Pling", NoteBlockInstrument.PLING);
        
        public static final Map<NoteBlockInstrument, OptionalInstrument> BY_MINECRAFT_INSTRUMENT = new HashMap<>();
        
        static {
            for (OptionalInstrument optionalInstrument : values()) {
                BY_MINECRAFT_INSTRUMENT.put(optionalInstrument.minecraftInstrument, optionalInstrument);
            }
        }
        
        private final String displayName;
        private final NoteBlockInstrument minecraftInstrument;
        
        OptionalInstrument(String displayName, @Nullable NoteBlockInstrument minecraftInstrument) {
            this.displayName = displayName;
            this.minecraftInstrument = minecraftInstrument;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
        public NoteBlockInstrument toMinecraftInstrument() {
            return minecraftInstrument;
        }
        
        public static OptionalInstrument fromMinecraftInstrument(NoteBlockInstrument instrument) {
            if (instrument != null) {
                return BY_MINECRAFT_INSTRUMENT.get(instrument);
            } else {
                return null;
            }
        }
        
    }
    
}
