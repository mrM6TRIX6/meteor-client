/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts.types;

import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.client.session.Session;

import java.util.Optional;

public class SessionAccount extends Account<SessionAccount> implements TokenAccount {
    
    private String accessToken;
    
    public SessionAccount(String label) {
        super(AccountType.SESSION, label);
        accessToken = label;
    }
    
    @Override
    public boolean fetchInfo() {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        
        ProfileResponse profile;
        try {
            profile = Http.get("https://api.minecraftservices.com/minecraft/profile")
                .bearer(accessToken)
                .sendJson(ProfileResponse.class);
        } catch (IllegalArgumentException e) {
            MeteorClient.LOG.error("Invalid session account token", e);
            return false;
        }
        
        if (profile == null || profile.id == null || profile.name == null) {
            return false;
        }
        
        cache.username = profile.name;
        cache.uuid = profile.id;
        
        return true;
    }
    
    @Override
    public boolean login() {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        
        super.login();
        cache.loadHead();
        
        setSession(new Session(cache.username, UndashedUuid.fromStringLenient(cache.uuid), accessToken, Optional.empty(), Optional.empty()));
        return true;
    }
    
    @Override
    public String getToken() {
        return accessToken;
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = super.toJson();
        
        jsonObject.addProperty("token", accessToken);
        
        return jsonObject;
    }
    
    @Override
    public SessionAccount fromJson(JsonObject jsonObject) {
        super.fromJson(jsonObject);
        
        accessToken = jsonObject.get("token").getAsString();
        
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SessionAccount account2)) {
            return false;
        }
        return account2.name.equals(this.name);
    }
    
    private static class ProfileResponse {
        
        public String id;
        public String name;
        
    }
    
}