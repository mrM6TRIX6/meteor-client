/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.PreInit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class GuiThemes {
    
    private static final File FOLDER = new File(MeteorClient.FOLDER, "gui");
    private static final File THEMES_FOLDER = new File(FOLDER, "themes");
    private static final File FILE = new File(FOLDER, "gui.json");
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();
    
    private static final List<GuiTheme> themes = new ArrayList<>();
    private static GuiTheme theme;
    
    private GuiThemes() {}
    
    @PreInit
    public static void init() {
        add(new MeteorGuiTheme());
    }
    
    @SuppressWarnings("unused")
    @PostInit
    public static void postInit() {
        if (FILE.exists()) {
            try (Reader reader = Files.newBufferedReader(FILE.toPath())) {
                JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                
                if (jsonObject != null && jsonObject.has("currentTheme")) {
                    select(jsonObject.get("currentTheme").getAsString());
                } else {
                    MeteorClient.LOG.warn("Theme config file is missing 'currentTheme' field, using default");
                    select("Meteor");
                }
            } catch (IOException e) {
                MeteorClient.LOG.error("Failed to load theme configuration", e);
                select("Meteor");
            } catch (JsonSyntaxException e) {
                MeteorClient.LOG.error("Theme config file contains invalid JSON", e);
                select("Meteor");
            } catch (Exception e) {
                MeteorClient.LOG.error("Unexpected error while loading theme configuration", e);
                select("Meteor");
            }
        } else {
            select("Meteor");
        }
        
        if (theme == null) {
            MeteorClient.LOG.warn("Theme was not initialized, forcing default theme");
            select("Meteor");
        }
    }
    
    public static void add(GuiTheme theme) {
        for (ListIterator<GuiTheme> it = themes.listIterator(); it.hasNext(); ) {
            if (it.next().name.equals(theme.name)) {
                // Replace the old one with same name
                it.set(theme);
                
                MeteorClient.LOG.error("Theme with the name '{}' has already been added.", theme.name);
                return;
            }
        }
        
        themes.add(theme);
    }
    
    public static void select(String name) {
        // Find theme with the provided name
        GuiTheme selectedTheme = null;
        
        for (GuiTheme t : themes) {
            if (t.name.equals(name)) {
                selectedTheme = t;
                break;
            }
        }
        
        if (selectedTheme != null) {
            // Save current theme before switching
            saveTheme();
            
            // Select new theme
            GuiThemes.theme = selectedTheme;
            
            // Load new theme configuration
            try {
                File file = new File(THEMES_FOLDER, get().name + ".json");
                
                if (file.exists()) {
                    try (Reader reader = Files.newBufferedReader(file.toPath())) {
                        JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                        
                        if (jsonObject != null) {
                            get().fromJson(jsonObject);
                        } else {
                            MeteorClient.LOG.warn("Theme config file is empty or invalid: {}", file.getName());
                        }
                    }
                } else {
                    MeteorClient.LOG.info("Theme config file not found, using default settings: {}", file.getName());
                }
            } catch (IOException e) {
                MeteorClient.LOG.error("Failed to load theme configuration for: {}", get().name, e);
            } catch (JsonSyntaxException e) {
                MeteorClient.LOG.error("Theme config file contains invalid JSON: {}", get().name, e);
            } catch (Exception e) {
                MeteorClient.LOG.error("Unexpected error while loading theme: {}", get().name, e);
            }
            
            // Save global gui settings with the new theme
            saveGlobal();
        } else {
            MeteorClient.LOG.warn("Theme not found: {}", name);
        }
    }
    
    public static GuiTheme get() {
        return theme;
    }
    
    public static String[] getNames() {
        String[] names = new String[themes.size()];
        
        for (int i = 0; i < themes.size(); i++) {
            names[i] = themes.get(i).name;
        }
        
        return names;
    }
    
    // Saving
    
    private static void saveTheme() {
        if (get() != null) {
            try {
                JsonObject jsonObject = get().toJson();
                if (jsonObject == null) {
                    MeteorClient.LOG.warn("Failed to serialize theme to JSON: {}", get().name);
                    return;
                }
                
                THEMES_FOLDER.mkdirs();
                
                File themeFile = new File(THEMES_FOLDER, get().name + ".json");
                
                try (Writer writer = new FileWriter(themeFile)) {
                    GSON.toJson(jsonObject, writer);
                }
                
                MeteorClient.LOG.debug("Successfully saved theme: {}", get().name);
            } catch (IOException e) {
                MeteorClient.LOG.error("Failed to save theme configuration: {}", get().name, e);
            } catch (Exception e) {
                MeteorClient.LOG.error("Unexpected error while saving theme: {}", get().name, e);
            }
        }
    }
    
    private static void saveGlobal() {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("currentTheme", get().name);
            
            FOLDER.mkdirs();
            
            File tempFile = new File(FILE.getParentFile(), FILE.getName() + ".tmp");
            
            try (Writer writer = new FileWriter(tempFile)) {
                GSON.toJson(jsonObject, writer);
            }
            
            if (FILE.exists()) {
                Files.move(tempFile.toPath(), FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(tempFile.toPath(), FILE.toPath());
            }
            
            MeteorClient.LOG.debug("Successfully saved global theme settings");
        } catch (IOException e) {
            MeteorClient.LOG.error("Failed to save global theme settings", e);
            
            try {
                File tempFile = new File(FILE.getParentFile(), FILE.getName() + ".tmp");
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            } catch (Exception ex) {}
        } catch (Exception e) {
            MeteorClient.LOG.error("Unexpected error while saving global theme settings", e);
        }
    }
    
    public static void save() {
        saveTheme();
        saveGlobal();
    }
    
}
