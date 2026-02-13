/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.network.FailedHttpResponse;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.render.PlayerHeadTexture;
import meteordevelopment.meteorclient.utils.render.PlayerHeadUtils;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Friend implements ISerializable<Friend>, Comparable<Friend> {
    
    public volatile String name;
    private volatile @Nullable UUID uuid;
    private volatile @Nullable PlayerHeadTexture headTexture;
    private volatile boolean updating;
    
    public Friend(String name, @Nullable UUID uuid) {
        this.name = name;
        this.uuid = uuid;
        this.headTexture = null;
    }
    
    public Friend(PlayerEntity player) {
        this(player.getName().getString(), player.getUuid());
    }
    
    public Friend(String name) {
        this(name, null);
    }
    
    public String getName() {
        return name;
    }
    
    public PlayerHeadTexture getHead() {
        return headTexture != null ? headTexture : PlayerHeadUtils.STEVE_HEAD;
    }
    
    public void updateInfo() {
        updating = true;
        HttpResponse<APIResponse> res = null;
        
        if (uuid != null) {
            res = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + UndashedUuid.toString(uuid))
                .exceptionHandler(e -> MeteorClient.LOG.error("Error while trying to connect session server for friend '{}'", name))
                .sendJsonResponse(APIResponse.class);
        }
        
        // Fallback to name-based lookup
        if (res == null || res.statusCode() != 200) {
            res = Http.get("https://api.mojang.com/users/profiles/minecraft/" + name)
                .exceptionHandler(e -> MeteorClient.LOG.error("Error while trying to update info for friend '{}'", name))
                .sendJsonResponse(APIResponse.class);
        }
        
        if (res != null && res.statusCode() == 200) {
            name = res.body().name;
            uuid = UndashedUuid.fromStringLenient(res.body().id);
            mc.execute(() -> headTexture = PlayerHeadUtils.fetchHead(uuid));
        } else if (!(res instanceof FailedHttpResponse)) { // cracked accounts shouldn't be assigned ids
            uuid = null;
        }
        
        updating = false;
    }
    
    public boolean headTextureNeedsUpdate() {
        return !this.updating && headTexture == null;
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("name", name);
        if (uuid != null) {
            jsonObject.addProperty("uuid", UndashedUuid.toString(uuid));
        }
        
        return jsonObject;
    }
    
    @Override
    public Friend fromJson(JsonObject jsonObject) {
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
        Friend friend = (Friend) o;
        return Objects.equals(name, friend.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public int compareTo(@NotNull Friend friend) {
        return name.compareToIgnoreCase(friend.name);
    }
    
    private static class APIResponse {
        
        String name, id;
        
    }
    
}
