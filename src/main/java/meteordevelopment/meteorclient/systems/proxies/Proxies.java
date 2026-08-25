/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.proxies;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.settings.impl.StringSetting;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Proxies extends System<Proxies> implements Iterable<Proxy> {

    public final Settings settings = new Settings();

    private final SettingGroup sgPing = settings.createGroup("Ping");

    public final Setting<String> pingAddress = sgPing.add(new StringSetting.Builder()
        .name("Address")
        .description("Minecraft server pinged through each proxy. Supports SRV records and a custom port.")
        .defaultValue("mc.hypixel.net")
        .build()
    );

    public final Setting<Integer> pingTimeout = sgPing.add(new IntSetting.Builder()
        .name("Timeout")
        .description("How long to wait for a response, in milliseconds.")
        .defaultValue(5000)
        .range(100, 60000)
        .sliderRange(500, 15000)
        .build()
    );

    public final Setting<Integer> pingThreads = sgPing.add(new IntSetting.Builder()
        .name("Threads")
        .description("How many proxies to ping at the same time.")
        .defaultValue(16)
        .range(1, 256)
        .sliderRange(1, 64)
        .build()
    );

    private List<Proxy> proxies = new ArrayList<>();
    private Proxy current;

    public Proxies() {
        super("proxies");
    }

    public static Proxies get() {
        return Systems.get(Proxies.class);
    }

    public boolean add(Proxy proxy) {
        if (proxies.contains(proxy)) {
            return false;
        }

        proxies.add(proxy);
        if (current == null) {
            current = proxy;
        }
        save();

        return true;
    }

    public void remove(Proxy proxy) {
        if (proxies.remove(proxy)) {
            if (current == proxy) {
                current = proxies.isEmpty() ? null : proxies.getFirst();
            }
            save();
        }
    }

    /**
     * Swaps an existing proxy for an edited copy, keeping its place in the list and whether it is the current one.
     *
     * @return false if the proxy is unknown or another entry already is the replacement
     */
    public boolean replace(Proxy proxy, Proxy replacement) {
        int i = proxies.indexOf(proxy);
        if (i == -1) {
            return false;
        }

        int duplicate = proxies.indexOf(replacement);
        if (duplicate != -1 && duplicate != i) {
            return false;
        }

        proxies.set(i, replacement);
        if (current == proxy) {
            current = replacement;
        }
        save();

        return true;
    }

    /**
     * Sorts the list by ping, fastest first, with proxies that have not been pinged yet after those and the ones that
     * failed last.
     */
    public void sortByPing() {
        // Pings are written by the pinger threads, so sort on a snapshot instead of letting the keys change mid sort
        Map<Proxy, Integer> keys = new IdentityHashMap<>(proxies.size());
        for (Proxy proxy : proxies) {
            keys.put(proxy, sortKey(proxy.ping));
        }

        proxies.sort(Comparator.comparingInt(keys::get));
        save();
    }

    private static int sortKey(int ping) {
        return switch (ping) {
            case Proxy.NOT_PINGED, Proxy.PINGING -> Integer.MAX_VALUE - 1;
            case Proxy.FAILED -> Integer.MAX_VALUE;
            default -> ping;
        };
    }

    public void clear() {
        proxies.clear();
        current = null;
        save();
    }

    public int getCount() {
        return proxies.size();
    }

    public boolean isEmpty() {
        return proxies.isEmpty();
    }

    @Nullable
    public Proxy getCurrent() {
        return current;
    }

    public boolean isCurrent(Proxy proxy) {
        return current == proxy;
    }
    
    public void setCurrent(@Nullable Proxy proxy) {
        current = proxy != null && proxies.contains(proxy) ? proxy : null;
        save();
    }

    @NotNull
    @Override
    public Iterator<Proxy> iterator() {
        return proxies.iterator();
    }

    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.add("settings", settings.toJson());

        JsonArray array = new JsonArray();
        for (Proxy proxy : proxies) {
            array.add(proxy.toJson());
        }
        jsonObject.add("proxies", array);
        jsonObject.addProperty("current", proxies.indexOf(current));

        return jsonObject;
    }

    @Override
    public Proxies fromJson(JsonObject jsonObject) {
        if (jsonObject.has("settings")) {
            settings.fromJson(jsonObject.getAsJsonObject("settings"));
        }

        proxies = JsonUtils.listFromJson(jsonObject.getAsJsonArray("proxies"), Proxy::fromJson);

        int currentIndex = jsonObject.has("current") ? jsonObject.get("current").getAsInt() : 0;
        current = currentIndex >= 0 && currentIndex < proxies.size() ? proxies.get(currentIndex) : null;

        return this;
    }

}
