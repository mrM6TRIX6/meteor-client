/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.proxies;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Proxies extends System<Proxies> implements Iterable<Proxy> {
    
    // https://regex101.com/r/gRHjnd/latest
    public static final Pattern PROXY_PATTERN = Pattern.compile("^(?:([\\w\\s]+)=)?((?:0*(?:\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])(?:\\.(?!:)|)){4}):(?!0)(\\d{1,4}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])(?::([^:]+):([^:]+))?$", Pattern.MULTILINE);
    
    private List<Proxy> proxies = new ArrayList<>();
    
    public Proxies() {
        super("proxies");
    }
    
    public static Proxies get() {
        return Systems.get(Proxies.class);
    }
    
    public boolean add(Proxy proxy) {
        for (Proxy proxy1 : proxies) {
            if (proxy1.address.get().equals(proxy.address.get()) && Objects.equals(proxy1.port.get(), proxy.port.get())) {
                return false;
            }
        }
        
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
