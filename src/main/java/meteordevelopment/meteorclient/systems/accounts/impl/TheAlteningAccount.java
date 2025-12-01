/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import de.florianmichael.waybackauthlib.InvalidCredentialsException;
import de.florianmichael.waybackauthlib.WaybackAuthLib;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.mixin.YggdrasilMinecraftSessionServiceAccessor;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;
import net.minecraft.client.session.Session;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TheAlteningAccount extends Account<TheAlteningAccount> implements TokenAccount {
    
    private static final Environment ENVIRONMENT = new Environment("http://sessionserver.thealtening.com", "http://authserver.thealtening.com", "The Altening");
    private static final YggdrasilAuthenticationService SERVICE = new YggdrasilAuthenticationService(((MinecraftClientAccessor) mc).meteor$getProxy(), ENVIRONMENT);
    private String token;
    private @Nullable WaybackAuthLib auth;
    
    public TheAlteningAccount(String token) {
        super(AccountType.THE_ALTENING, token);
        this.token = token;
    }
    
    @Override
    public boolean fetchInfo() {
        auth = getAuth();
        
        try {
            auth.logIn();
            
            cache.username = auth.getCurrentProfile().getName();
            cache.uuid = auth.getCurrentProfile().getId().toString();
            cache.loadHead();
            
            return true;
        } catch (InvalidCredentialsException e) {
            MeteorClient.LOG.error("Invalid TheAltening credentials.");
            return false;
        } catch (Exception e) {
            MeteorClient.LOG.error("Failed to fetch info for TheAltening account!");
            return false;
        }
    }
    
    @Override
    public boolean login() {
        if (auth == null) {
            return false;
        }
        applyLoginEnvironment(SERVICE, YggdrasilMinecraftSessionServiceAccessor.meteor$createYggdrasilMinecraftSessionService(SERVICE.getServicesKeySet(), SERVICE.getProxy(), ENVIRONMENT));
        
        try {
            setSession(new Session(auth.getCurrentProfile().getName(), auth.getCurrentProfile().getId(), auth.getAccessToken(), Optional.empty(), Optional.empty(), Session.AccountType.MOJANG));
            return true;
        } catch (Exception e) {
            MeteorClient.LOG.error("Failed to login with TheAltening.");
            return false;
        }
    }
    
    private WaybackAuthLib getAuth() {
        WaybackAuthLib auth = new WaybackAuthLib(ENVIRONMENT.servicesHost());
        
        auth.setUsername(name);
        auth.setPassword("Meteor on Crack!");
        
        return auth;
    }
    
    @Override
    public String getToken() {
        return token;
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("type", type.name());
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("token", token);
        jsonObject.add("cache", cache.toJson());
        
        return jsonObject;
    }
    
    @Override
    public TheAlteningAccount fromJson(JsonObject jsonObject) {
        if (jsonObject.get("name").getAsString().isEmpty() || jsonObject.get("cache").isJsonNull() || jsonObject.get("token").getAsString().isEmpty()) {
            throw new JsonSyntaxException("Invalid account data");
        }
        
        name = jsonObject.get("name").getAsString();
        token = jsonObject.get("token").getAsString();
        cache.fromJson(jsonObject.get("cache").getAsJsonObject());
        
        return this;
    }
    
}
