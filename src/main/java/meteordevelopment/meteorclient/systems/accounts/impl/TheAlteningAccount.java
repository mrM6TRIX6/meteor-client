/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts.impl;

import com.google.gson.JsonObject;
import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import de.florianmichael.waybackauthlib.InvalidCredentialsException;
import de.florianmichael.waybackauthlib.WaybackAuthLib;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;
import net.minecraft.client.session.Session;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TheAlteningAccount extends Account<TheAlteningAccount> implements TokenAccount {
    
    private static final Environment ENVIRONMENT = new Environment(
        "http://sessionserver.thealtening.com",
        "http://authserver.thealtening.com",
        "https://api.mojang.com",
        "TheAltening"
    );
    private static final YggdrasilAuthenticationService SERVICE = new YggdrasilAuthenticationService(mc.getNetworkProxy(), ENVIRONMENT);
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
            
            cache.username = auth.getCurrentProfile().name();
            cache.uuid = auth.getCurrentProfile().id().toString();
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
        applyLoginEnvironment(SERVICE);
        
        try {
            setSession(
                new Session(
                    auth.getCurrentProfile().name(),
                    auth.getCurrentProfile().id(),
                    auth.getAccessToken(),
                    Optional.empty(),
                    Optional.empty()
                )
            );
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
        JsonObject jsonObject = super.toJson();
        
        jsonObject.addProperty("token", token);
        
        return jsonObject;
    }
    
    @Override
    public TheAlteningAccount fromJson(JsonObject jsonObject) {
        super.fromJson(jsonObject);
        
        token = jsonObject.get("token").getAsString();
        
        return this;
    }
    
}
