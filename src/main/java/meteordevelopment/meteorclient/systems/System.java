/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.files.StreamUtils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public abstract class System<T> implements ISerializable<T> {
    
    private final String name;
    private File file;
    
    protected boolean isFirstInit;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();
    
    public System(String name) {
        this.name = name;
        
        if (name != null) {
            this.file = new File(MeteorClient.FOLDER, name + ".json");
            this.isFirstInit = !file.exists();
        }
    }
    
    public void init() {}
    
    public void save(File folder) {
        File file = getFile();
        if (file == null) {
            return;
        }
        
        JsonObject jsonObject = toJson();
        if (jsonObject == null) {
            return;
        }
        
        try {
            File tempFile = File.createTempFile(MeteorClient.MOD_ID, file.getName());
            
            try (FileWriter writer = new FileWriter(tempFile)) {
                GSON.toJson(jsonObject, writer);
            }
            
            if (folder != null) {
                file = new File(folder, file.getName());
            }
            
            file.getParentFile().mkdirs();
            
            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                StreamUtils.copy(tempFile, file);
            }
            tempFile.delete();
        } catch (IOException e) {
            MeteorClient.LOG.error("Error saving {}. Possibly corrupted?", this.name, e);
        }
    }
    
    public void save() {
        save(null);
    }
    
    public void load(File folder) {
        File file = getFile();
        if (file == null) {
            return;
        }
        
        try {
            if (folder != null) {
                file = new File(folder, file.getName());
            }
            
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                    if (jsonObject != null) {
                        fromJson(jsonObject);
                    }
                } catch (Exception e) {
                    String backupName = FilenameUtils.removeExtension(file.getName()) + "-" + ZonedDateTime.now().format(DATE_TIME_FORMATTER) + ".backup.json";
                    File backup = new File(file.getParentFile(), backupName);
                    
                    try {
                        Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException ex) {
                        StreamUtils.copy(file, backup);
                    }
                    
                    MeteorClient.LOG.error("Error loading {}. Possibly corrupted?", this.name, e);
                    MeteorClient.LOG.info("Saved settings backup to '{}'.", backup);
                }
            }
        } catch (IOException e) {
            MeteorClient.LOG.error("Error loading {}.", this.name, e);
        }
    }
    
    public void load() {
        load(null);
    }
    
    public File getFile() {
        return file;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public JsonObject toJson() {
        return null;
    }
    
    @Override
    public T fromJson(JsonObject jsonObject) {
        return null;
    }
    
}
