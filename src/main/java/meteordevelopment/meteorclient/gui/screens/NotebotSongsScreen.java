/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.fun.notebot.Notebot;
import meteordevelopment.meteorclient.systems.modules.fun.notebot.decoder.SongDecoders;
import meteordevelopment.meteorclient.utils.Utils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotebotSongsScreen extends WindowScreen {
    
    private static final Notebot notebot = Modules.get().get(Notebot.class);
    
    private WTextBox filter;
    private String filterText = "";
    
    private WTable table;
    
    public NotebotSongsScreen() {
        super("Notebot Songs");
    }
    
    @Override
    public void initWidgets() {
        // Random Song
        WButton randomSong = add(new WButton("Random Song")).minWidth(400).expandX().widget();
        randomSong.action = notebot::playRandomSong;
        
        // Filter
        filter = add(new WTextBox("", "Search for the songs...")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();
            
            table.clear();
            initSongsTable();
        };
        
        table = add(new WTable()).widget();
        
        initSongsTable();
    }
    
    private void initSongsTable() {
        AtomicBoolean noSongsFound = new AtomicBoolean(true);
        try {
            Files.list(MeteorClient.FOLDER.toPath().resolve("notebot")).forEach(path -> {
                if (SongDecoders.hasDecoder(path)) {
                    String name = path.getFileName().toString();
                    
                    if (Utils.searchTextDefault(name, filterText, false)) {
                        addPath(path);
                        noSongsFound.set(false);
                    }
                }
            });
        } catch (IOException e) {
            table.add(new WLabel("Missing meteor-client/notebot folder.")).expandCellX();
            table.row();
        }
        
        if (noSongsFound.get()) {
            table.add(new WLabel("No songs found.")).expandCellX().center();
        }
    }
    
    private void addPath(Path path) {
        table.add(new WHorizontalSeparator()).expandX().minWidth(400);
        table.row();
        
        table.add(new WLabel(FilenameUtils.getBaseName(path.getFileName().toString()))).expandCellX();
        WButton load = table.add(new WButton("Load")).right().widget();
        load.action = () -> notebot.loadSong(path.toFile());
        WButton preview = table.add(new WButton("Preview")).right().widget();
        preview.action = () -> notebot.previewSong(path.toFile());
        
        table.row();
    }
    
}
