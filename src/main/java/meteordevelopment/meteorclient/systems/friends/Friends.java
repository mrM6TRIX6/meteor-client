/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import joptsimple.internal.Strings;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Friends extends System<Friends> implements Iterable<Friend> {
    
    private final List<Friend> friends = new ArrayList<>();
    
    public Friends() {
        super("friends");
    }
    
    public static Friends get() {
        return Systems.get(Friends.class);
    }
    
    public boolean add(Friend friend) {
        if (friend.name.isEmpty() || friend.name.contains(" ")) {
            return false;
        }
        
        if (!friends.contains(friend)) {
            friends.add(friend);
            save();
            
            return true;
        }
        
        return false;
    }
    
    public boolean remove(Friend friend) {
        if (friends.remove(friend)) {
            save();
            return true;
        }
        
        return false;
    }
    
    public Friend get(String name) {
        for (Friend friend : friends) {
            if (friend.name.equalsIgnoreCase(name)) {
                return friend;
            }
        }
        
        return null;
    }
    
    public Friend get(PlayerEntity player) {
        return get(player.getName().getString());
    }
    
    public Friend get(PlayerListEntry player) {
        return get(player.getProfile().name());
    }
    
    public boolean isFriend(String name) {
        return get(name) != null;
    }
    
    public boolean isFriend(PlayerEntity player) {
        return player != null && get(player) != null;
    }
    
    public boolean isFriend(PlayerListEntry player) {
        return get(player) != null;
    }
    
    public boolean shouldAttack(PlayerEntity player) {
        return !isFriend(player);
    }
    
    public void clear() {
        friends.clear();
        save();
    }
    
    public int getCount() {
        return friends.size();
    }
    
    public boolean isEmpty() {
        return friends.isEmpty();
    }
    
    @Override
    public @NotNull Iterator<Friend> iterator() {
        return friends.iterator();
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.add("friends", JsonUtils.listToJson(friends));
        
        return jsonObject;
    }
    
    @Override
    public Friends fromJson(JsonObject jsonObject) {
        friends.clear();
        
        for (JsonElement element : jsonObject.get("friends").getAsJsonArray()) {
            JsonObject friendJson = (JsonObject) element;
            if (!friendJson.has("name")) {
                continue;
            }
            
            String name = friendJson.get("name").getAsString();
            if (get(name) != null) {
                continue;
            }
            
            String uuid = Strings.EMPTY;
            if (friendJson.has("uuid")) {
                uuid = friendJson.get("uuid").getAsString();
            }
            
            Friend friend = !uuid.isBlank()
                ? new Friend(name, UndashedUuid.fromStringLenient(uuid))
                : new Friend(name);
            
            friends.add(friend);
        }
        
        Collections.sort(friends);
        
        MeteorExecutor.execute(() -> friends.forEach(Friend::updateInfo));
        
        return this;
    }
    
}
