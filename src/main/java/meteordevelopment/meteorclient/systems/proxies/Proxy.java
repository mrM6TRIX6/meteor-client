/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.proxies;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Proxy {
    
    public static final int NOT_PINGED = -1;
    public static final int FAILED = -2;
    public static final int PINGING = -3;

    public static final String FORMAT = "ip:port or user:pass@ip:port";

    private static final Pattern PATTERN = Pattern.compile("(?:(?<user>[^:@\\s]+):(?<pass>[^:@\\s]*)@)?(?<host>[^:@\\s]+):(?<port>\\d{1,5})");
    private static final Pattern IPV4 = Pattern.compile("[0-9.]+");

    public final String address;
    public final int port;
    public final String username;
    public final String password;
    
    public volatile int ping = NOT_PINGED;

    public Proxy(String address, int port, String username, String password) {
        this.address = address;
        this.port = port;
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
    }
    
    @Nullable
    public static Proxy parse(String string) {
        Matcher matcher = PATTERN.matcher(string.trim());
        if (!matcher.matches()) {
            return null;
        }

        int port = Integer.parseInt(matcher.group("port"));
        if (port < 1 || port > 65535) {
            return null;
        }

        // Strip leading zeros from ipv4 octets, they aren't handled consistently by resolvers
        String address = matcher.group("host");
        if (IPV4.matcher(address).matches()) {
            address = address.replaceAll("\\b0+\\B", "");
        }

        return new Proxy(address, port, matcher.group("user"), matcher.group("pass"));
    }

    public boolean hasAuthentication() {
        return !username.isEmpty();
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("address", address);
        jsonObject.addProperty("port", port);
        if (hasAuthentication()) {
            jsonObject.addProperty("username", username);
            jsonObject.addProperty("password", password);
        }

        return jsonObject;
    }

    @Nullable
    public static Proxy fromJson(JsonElement jsonElement) {
        if (!(jsonElement instanceof JsonObject jsonObject)) {
            return null;
        }

        if (!jsonObject.has("address") || !jsonObject.has("port")) {
            return null;
        }

        return new Proxy(
            jsonObject.get("address").getAsString(),
            jsonObject.get("port").getAsInt(),
            jsonObject.has("username") ? jsonObject.get("username").getAsString() : "",
            jsonObject.has("password") ? jsonObject.get("password").getAsString() : ""
        );
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }
    
    public String toFullString() {
        return hasAuthentication() ? username + ":" + password + "@" + this : toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Proxy proxy)) {
            return false;
        }
        return port == proxy.port
            && address.equals(proxy.address)
            && username.equals(proxy.username)
            && password.equals(proxy.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port, username, password);
    }

}
