/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.render.PlayerHeadTexture;
import meteordevelopment.meteorclient.utils.render.PlayerHeadUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AccountCache implements ISerializable<AccountCache> {
    
    public String username = "";
    public String uuid = "";
    private PlayerHeadTexture headTexture;
    
    public PlayerHeadTexture getHeadTexture() {
        return headTexture != null ? headTexture : PlayerHeadUtils.STEVE_HEAD;
    }
    
    public void loadHead() {
        if (uuid == null || uuid.isBlank()) {
            return;
        }
        
        mc.execute(() -> headTexture = PlayerHeadUtils.fetchHead(UndashedUuid.fromStringLenient(uuid)));
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("username", username);
        jsonObject.addProperty("uuid", uuid);
        
        return jsonObject;
    }
    
    @Override
    public AccountCache fromJson(JsonObject jsonObject) {
        if (jsonObject.get("username").getAsString().isEmpty()) {
            throw new JsonSyntaxException("Invalid account cache data");
        }
        
        username = jsonObject.get("username").getAsString();
        uuid = jsonObject.get("uuid").getAsString();
        loadHead();
        
        return this;
    }
    
}
