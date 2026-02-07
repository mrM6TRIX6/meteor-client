package meteordevelopment.meteorclient.systems.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.StringListSetting;
import meteordevelopment.meteorclient.settings.impl.StringSetting;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Represents single config file (one JSON file) with optional sections modules/hud/macros
 */
public class Config implements ISerializable<Config> {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public final Settings settings = new Settings();
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSave = settings.createGroup("Save");
    
    public Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name of the profile.")
        .filter(Utils::nameFilter)
        .build()
    );
    
    public Setting<List<String>> loadOnJoin = sgGeneral.add(new StringListSetting.Builder()
        .name("load-on-join")
        .description("Which servers to set this profile as active when joining.")
        .filter(Utils::ipFilter)
        .build()
    );
    
    public Setting<Boolean> hud = sgSave.add(new BoolSetting.Builder()
        .name("hud")
        .description("Whether the profile should save hud.")
        .defaultValue(false)
        .build()
    );
    
    public Setting<Boolean> macros = sgSave.add(new BoolSetting.Builder()
        .name("macros")
        .description("Whether the profile should save macros.")
        .defaultValue(false)
        .build()
    );
    
    public Setting<Boolean> modules = sgSave.add(new BoolSetting.Builder()
        .name("modules")
        .description("Whether the profile should save modules.")
        .defaultValue(false)
        .build()
    );
    
    public Config() {}
    
    /**
     * Load this config into global systems. Only fields present in file are applied.
     */
    public void load() {
        File file = getFile();
        if (!file.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            
            if (root.has("settings")) {
                settings.fromJson(root.getAsJsonObject("settings"));
            }
            
            if (root.has("modules")) {
                Modules.get().fromJson(root.getAsJsonObject("modules"));
            }
            if (root.has("hud")) {
                Hud.get().fromJson(root.getAsJsonObject("hud"));
            }
            if (root.has("macros")) {
                Macros.get().fromJson(root.getAsJsonObject("macros"));
            }
        } catch (Exception e) {
            MeteorClient.LOG.error("Error loading config file {}", file, e);
        }
    }
    
    /**
     * Save selected components into single config file. Only components whose flags are true are written.
     */
    public void save() {
        File file = getFile();
        file.getParentFile().mkdirs();
        
        JsonObject root = new JsonObject();
        root.add("settings", settings.toJson());
        
        if (modules.get()) {
            root.add("modules", Modules.get().toJson());
        }
        if (hud.get()) {
            root.add("hud", Hud.get().toJson());
        }
        if (macros.get()) {
            root.add("macros", Macros.get().toJson());
        }
        
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            MeteorClient.LOG.error("Error saving config file {}", file, e);
        }
    }
    
    public void deleteFile() {
        try {
            Files.deleteIfExists(getFile().toPath());
        } catch (IOException e) {
            MeteorClient.LOG.error("Error deleting config file {}", getFile(), e);
        }
    }
    
    public File getFile() {
        return new File(Configs.FOLDER, name.get() + ".json");
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("file", name.get() + ".json");
        
        return jsonObject;
    }
    
    @Override
    public Config fromJson(JsonObject jsonObject) {
        if (jsonObject.has("settings")) {
            settings.fromJson(jsonObject.getAsJsonObject("settings"));
        }
        
        if (jsonObject.has("name")) {
            name.set(jsonObject.get("name").getAsString());
        }
        
        return this;
    }
    
}
