/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.profiles;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinEvent;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.orbit.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Profiles extends System<Profiles> implements Iterable<Profile> {
    
    public static final File FOLDER = new File(MeteorClient.FOLDER, "profiles");
    
    private List<Profile> profiles = new ArrayList<>();
    
    public Profiles() {
        super("profiles");
    }
    
    public static Profiles get() {
        return Systems.get(Profiles.class);
    }
    
    public Profile get(String name) {
        for (Profile profile : this) {
            if (profile.name.get().equalsIgnoreCase(name)) {
                return profile;
            }
        }
        return null;
    }
    
    public void add(Profile profile) {
        if (!profiles.contains(profile)) {
            profiles.add(profile);
        }
        profile.save();
        save();
    }
    
    public void remove(Profile profile) {
        if (profiles.remove(profile)) {
            profile.delete();
        }
        save();
    }
    
    public int getCount() {
        return profiles.size();
    }
    
    public void clear() {
        profiles.clear();
        save();
    }
    
    public List<Profile> getAll() {
        return profiles;
    }
    
    @Override
    public File getFile() {
        return new File(FOLDER, "profiles.json");
    }
    
    @EventHandler
    private void onGameJoin(GameJoinEvent event) {
        for (Profile profile : this) {
            if (profile.loadOnJoin.get().contains(Utils.getWorldName())) {
                profile.load();
            }
        }
    }
    
    public boolean isEmpty() {
        return profiles.isEmpty();
    }
    
    @Override
    public @NotNull Iterator<Profile> iterator() {
        return profiles.iterator();
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("profiles", JsonUtils.listToJson(profiles));
        return jsonObject;
    }
    
    @Override
    public Profiles fromJson(JsonObject jsonObject) {
        profiles = JsonUtils.listFromJson(jsonObject.get("profiles").getAsJsonArray(), Profile::new);
        return this;
    }
    
}
