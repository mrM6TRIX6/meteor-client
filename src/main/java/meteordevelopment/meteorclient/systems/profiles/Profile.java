/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.profiles;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class Profile implements ISerializable<Profile> {
    
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
    
    public Profile() {}
    
    public Profile(JsonElement jsonElement) {
        fromJson((JsonObject) jsonElement);
    }
    
    public void load() {
        File folder = getFile();
        
        if (hud.get()) {
            Hud.get().load(folder);
        }
        if (macros.get()) {
            Macros.get().load(folder);
        }
        if (modules.get()) {
            Modules.get().load(folder);
        }
    }
    
    public void save() {
        File folder = getFile();
        
        if (hud.get()) {
            Hud.get().save(folder);
        }
        if (macros.get()) {
            Macros.get().save(folder);
        }
        if (modules.get()) {
            Modules.get().save(folder);
        }
    }
    
    public void delete() {
        try {
            FileUtils.deleteDirectory(getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private File getFile() {
        return new File(Profiles.FOLDER, name.get());
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.add("settings", settings.toJson());
        
        return jsonObject;
    }
    
    @Override
    public Profile fromJson(JsonObject jsonObject) {
        if (jsonObject.has("settings")) {
            settings.fromJson(jsonObject.get("settings").getAsJsonObject());
        }
        
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Profile profile = (Profile) o;
        return Objects.equals(profile.name.get(), this.name.get());
    }
    
}
