/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.proxies;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.settings.impl.StringSetting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.Objects;

public class Proxy implements ISerializable<Proxy> {
    
    public final Settings settings = new Settings();
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAuthentication = settings.createGroup("Authentication");
    
    public Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name of the proxy.")
        .build()
    );
    
    public Setting<String> address = sgGeneral.add(new StringSetting.Builder()
        .name("address")
        .description("The ip address of the proxy.")
        .filter(Utils::ipFilter)
        .build()
    );
    
    public Setting<Integer> port = sgGeneral.add(new IntSetting.Builder()
        .name("port")
        .description("The port of the proxy.")
        .defaultValue(0)
        .range(0, 65535)
        .sliderMax(65535)
        .noSlider()
        .build()
    );
    
    public Setting<Boolean> enabled = sgGeneral.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Whether the proxy is enabled.")
        .defaultValue(true)
        .build()
    );
    
    // Optional
    
    public Setting<String> username = sgAuthentication.add(new StringSetting.Builder()
        .name("username")
        .description("The username of the proxy.")
        .build()
    );
    
    public Setting<String> password = sgAuthentication.add(new StringSetting.Builder()
        .name("password")
        .description("The password of the proxy.")
        .build()
    );
    
    private Proxy() {
    }
    
    public Proxy(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }
    
    public boolean resolveAddress() {
        return Utils.resolveAddress(this.address.get(), this.port.get());
    }
    
    public static class Builder {
        
        protected String address = "";
        protected int port = 0;
        protected String name = "";
        protected String username = "";
        protected String password = "";
        protected boolean enabled = false;
        
        public Builder address(String address) {
            this.address = address;
            return this;
        }
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        public Builder password(String password) {
            this.password = password;
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Proxy build() {
            Proxy proxy = new Proxy();
            
            if (!address.equals(proxy.address.getDefaultValue())) {
                proxy.address.set(address);
            }
            if (port != proxy.port.getDefaultValue()) {
                proxy.port.set(port);
            }
            if (!name.equals(proxy.name.getDefaultValue())) {
                proxy.name.set(name);
            }
            if (!username.equals(proxy.username.getDefaultValue())) {
                proxy.username.set(username);
            }
            if (!password.equals(proxy.password.getDefaultValue())) {
                proxy.password.set(password);
            }
            if (enabled != proxy.enabled.getDefaultValue()) {
                proxy.enabled.set(enabled);
            }
            
            return proxy;
        }
        
    }
    
    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("settings", settings.toTag());
        return tag;
    }
    
    @Override
    public Proxy fromTag(NbtCompound tag) {
        tag.getCompound("settings").ifPresent(settings::fromTag);
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
        Proxy proxy = (Proxy) o;
        return Objects.equals(proxy.address.get(), this.address.get()) && Objects.equals(proxy.port.get(), this.port.get());
    }
    
}
