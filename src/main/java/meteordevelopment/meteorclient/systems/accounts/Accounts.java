/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.accounts.types.CrackedAccount;
import meteordevelopment.meteorclient.systems.accounts.types.MicrosoftAccount;
import meteordevelopment.meteorclient.systems.accounts.types.SessionAccount;
import meteordevelopment.meteorclient.systems.accounts.types.TheAlteningAccount;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Accounts extends System<Accounts> implements Iterable<Account<?>> {
    
    private List<Account<?>> accounts = new ArrayList<>();
    
    public Accounts() {
        super("accounts");
    }
    
    public static Accounts get() {
        return Systems.get(Accounts.class);
    }
    
    public void add(Account<?> account) {
        accounts.add(account);
        save();
    }
    
    public boolean exists(Account<?> account) {
        return accounts.contains(account);
    }
    
    public void remove(Account<?> account) {
        if (accounts.remove(account)) {
            save();
        }
    }
    
    public void clear() {
        accounts.clear();
        save();
    }
    
    public int getCount() {
        return accounts.size();
    }
    
    @Override
    public @NotNull Iterator<Account<?>> iterator() {
        return accounts.iterator();
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.add("accounts", JsonUtils.listToJson(accounts));
        
        return jsonObject;
    }
    
    @Override
    public Accounts fromJson(JsonObject jsonObject) {
        MeteorExecutor.execute(() -> accounts = JsonUtils.listFromJson(jsonObject.get("accounts").getAsJsonArray(), accountElement -> {
            JsonObject account = (JsonObject) accountElement;
            if (!account.has("type")) {
                return null;
            }
            
            AccountType type = AccountType.valueOf(account.get("type").getAsString());
            
            try {
                return switch (type) {
                    case CRACKED -> new CrackedAccount(null).fromJson(account);
                    case MICROSOFT -> new MicrosoftAccount(null).fromJson(account);
                    case SESSION -> new SessionAccount(null).fromJson(account);
                    case THE_ALTENING -> new TheAlteningAccount(null).fromJson(account);
                };
            } catch (JsonParseException e) {
                return null;
            }
        }));
        return this;
    }
    
}
