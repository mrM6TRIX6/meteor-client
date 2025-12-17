package meteordevelopment.meteorclient.systems.configs;

import com.google.gson.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinEvent;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Configs extends System<Configs> implements Iterable<Config> {
    
    public static final File FOLDER = new File(MeteorClient.FOLDER, "configs");
    public static final File REGISTRY = new File(MeteorClient.FOLDER, "configs.json");
    
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    
    private final List<Config> configs = new ArrayList<>();
    private final AtomicBoolean ignoreFileEvents = new AtomicBoolean(false);
    private WatchService watchService;
    
    public Configs() {
        super("configs");
    }
    
    public static Configs get() {
        return Systems.get(Configs.class);
    }
    
    @Override
    public void init() {
        FOLDER.mkdirs();
        load(null);
        startWatcher();
    }
    
    @Override
    public void save(File folder) {
        try (FileWriter writer = new FileWriter(getFile())) {
            GSON.toJson(toJson(), writer);
        } catch (IOException e) {
            MeteorClient.LOG.error("Error saving configs registry", e);
        }
    }
    
    @Override
    public void load(File folder) {
        if (!getFile().exists()) {
            saveRegistry();
            return;
        }
        
        try (FileReader reader = new FileReader(getFile())) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            fromJson(root);
        } catch (Exception e) {
            MeteorClient.LOG.error("Error loading configs registry", e);
        }
        
        File[] files = FOLDER.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                if (file.getName().equalsIgnoreCase("configs.json")) {
                    continue;
                }
                String base = file.getName().substring(0, file.getName().length() - 5);
                if (get(base) == null) {
                    Config config = new Config();
                    config.name.set(base);
                    configs.add(config);
                }
            }
        }
        
        saveRegistry();
    }
    
    @Override
    public File getFile() {
        // System.getFile() expects this to be MeteorClient.FOLDER/<name>.json which fits configs.json in root
        return REGISTRY;
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        
        for (Config config : configs) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("file", config.name.get() + ".json");
            array.add(jsonObject);
        }
        
        root.add("configs", array);
        return root;
    }
    
    @Override
    public Configs fromJson(JsonObject jsonObject) {
        configs.clear();
        if (jsonObject == null || !jsonObject.has("configs")) {
            return this;
        }
        
        JsonArray array = jsonObject.getAsJsonArray("configs");
        
        for (JsonElement element : array) {
            JsonObject configObj = element.getAsJsonObject();
            String file = configObj.get("file").getAsString();
            String name = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
            Config config = new Config();
            config.name.set(name);
            configs.add(config);
        }
        
        return this;
    }
    
    public List<Config> getAll() {
        return configs;
    }
    
    public Config get(String name) {
        for (Config config : configs) {
            if (config.name.get().equalsIgnoreCase(name)) {
                return config;
            }
        }
        
        return null;
    }
    
    public int getCount() {
        return configs.size();
    }
    
    public void add(Config config) {
        if (!configs.contains(config)) {
            configs.add(config);
        }
        
        // Create backing file and save
        ignoreFileEvents.set(true);
        config.save();
        saveRegistry();
        ignoreFileEvents.set(false);
    }
    
    public void remove(Config config) {
        if (configs.remove(config)) {
            ignoreFileEvents.set(true);
            config.deleteFile();
            saveRegistry();
            ignoreFileEvents.set(false);
        }
    }
    
    public void clear() {
        List<Config> copy = new ArrayList<>(configs);
        for (Config config : copy) {
            remove(config);
        }
    }
    
    @Override
    public @NotNull Iterator<Config> iterator() {
        return configs.iterator();
    }
    
    public boolean isEmpty() {
        return configs.isEmpty();
    }
    
    private void saveRegistry() {
        ignoreFileEvents.set(true);
        
        try (FileWriter writer = new FileWriter(getFile())) {
            GSON.toJson(toJson(), writer);
        } catch (IOException e) {
            MeteorClient.LOG.error("Error saving configs registry", e);
        } finally {
            // Small delay to avoid watch events firing for our own write
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
            
            ignoreFileEvents.set(false);
        }
    }
    
    @EventHandler
    private void onGameJoin(GameJoinEvent event) {
        for (Config config : configs) {
            if (config.loadOnJoin.get().contains(Utils.getWorldName())) {
                config.load();
            }
        }
    }
    
    private void startWatcher() {
        Thread watcher = new Thread(new ConfigWatcher(), "ConfigWatcher");
        watcher.setDaemon(true);
        watcher.start();
    }
    
    private class ConfigWatcher implements Runnable {
        
        @Override
        public void run() {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                FOLDER.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                
                while (true) {
                    WatchKey key = watchService.take();
                    if (ignoreFileEvents.get()) {
                        key.reset();
                        continue;
                    }
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path rel = (Path) event.context();
                        String fileName = rel.getFileName().toString();
                        if (!fileName.toLowerCase().endsWith(".json")) {
                            continue;
                        }
                        if (fileName.equalsIgnoreCase("configs.json")) {
                            continue;
                        }
                        
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            onExternalCreate(fileName);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            onExternalDelete(fileName);
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            onExternalModify(fileName);
                        }
                    }
                    
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                MeteorClient.LOG.error("Config watcher failed", e);
            }
        }
        
        private void onExternalCreate(String fileName) {
            String base = fileName.substring(0, fileName.length() - 5);
            if (get(base) != null) {
                return;
            }
            
            Config config = new Config();
            config.name.set(base);
            configs.add(config);
            saveRegistry();
        }
        
        private void onExternalDelete(String fileName) {
            String base = fileName.substring(0, fileName.length() - 5);
            Config found = get(base);
            if (found != null) {
                configs.remove(found);
                saveRegistry();
            }
        }
        
        private void onExternalModify(String fileName) {
            // Modification could change settings inside a config. Optionally reload metadata if stored in registry.
            // Minimal behavior: do nothing (user can explicit reload). If you want auto-reload, implement parsing here.
        }
        
    }
    
}
