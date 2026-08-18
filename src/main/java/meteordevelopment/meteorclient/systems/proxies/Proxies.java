/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.proxies;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class Proxies extends System<Proxies> implements Iterable<Proxy> {
    
    public static final Pattern PROXY_PATTERN = Pattern.compile("^(?<user>[^:@\\s]+):(?<pass>[^:@\\s]+)@(?<host>[a-zA-Z0-9.-]+):(?<port>6553[0-5]|655[0-2][0-9]|65[0-4][0-9]{2}|6[0-4][0-9]{3}|[1-9][0-9]{0,3})$", Pattern.MULTILINE);
    
    private List<Proxy> proxies = new ArrayList<>();
    
    public Proxies() {
        super("proxies");
    }
    
    public static Proxies get() {
        return Systems.get(Proxies.class);
    }
    
    public boolean add(Proxy proxy) {
        if (proxies.isEmpty()) {
            proxy.enabled.set(true);
        }
        
        proxies.add(proxy);
        save();
        
        return true;
    }
    
    public void remove(Proxy proxy) {
        if (proxies.remove(proxy)) {
            save();
        }
    }
    
    public int getCount() {
        return proxies.size();
    }
    
    public void clear() {
        proxies.clear();
        save();
    }
    
    public Proxy getEnabled() {
        for (Proxy proxy : proxies) {
            if (proxy.enabled.get()) {
                return proxy;
            }
        }
        return null;
    }
    
    public void setEnabled(Proxy proxy, boolean enabled) {
        for (Proxy p : proxies) {
            p.enabled.set(false);
        }
        proxy.enabled.set(enabled);
        save();
    }
    
    public boolean isEmpty() {
        return proxies.isEmpty();
    }
    
    @NotNull
    @Override
    public Iterator<Proxy> iterator() {
        return proxies.iterator();
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.add("proxies", JsonUtils.listToJson(proxies));
        
        return jsonObject;
    }
    
    @Override
    public Proxies fromJson(JsonObject jsonObject) {
        proxies = JsonUtils.listFromJson(jsonObject.get("proxies").getAsJsonArray(), Proxy::new);
        return this;
    }
    
}
