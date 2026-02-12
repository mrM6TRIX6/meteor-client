/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.IParent;
import meteordevelopment.meteorclient.utils.misc.IRunInMainMenu;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import net.minecraft.client.MinecraftClient;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Choice setting, based on {@link IDisplayName} enum constants, but more advanced to support multimode systems.
 *
 * @param <T> enum, which implements {@link IDisplayName}
 */
public class ModeEnumChoiceSetting<T extends Enum<T> & IDisplayName & ModeEnumChoiceSetting.IModeImpl<P>, P extends IRunInMainMenu> extends EnumChoiceSetting<T> {
    
    public ModeEnumChoiceSetting(String name, String title, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
    }
    
    /**
     * Subscribe / unsubscribe value {@link ModeImpl ModeImpl}
     *
     * @param state if {@code true}, {@link ModeImpl ModeImpl} will be subscribed. If {@code false} - unsubscribed.
     */
    public void updateState(boolean state) {
        ModeImpl<P> modeImpl = value.getModeImpl();
        
        if (state) {
            if (!modeImpl.subscribed) {
                MeteorClient.EVENT_BUS.subscribe(modeImpl);
                modeImpl.subscribed = true;
                modeImpl.onSelect();
            }
        } else {
            if (modeImpl.subscribed) {
                MeteorClient.EVENT_BUS.unsubscribe(modeImpl);
                modeImpl.subscribed = false;
                modeImpl.onUnselect();
            }
        }
    }
    
    @Override // This needs to override so that set() is used to change the value.
    protected void resetImpl() {
        set(defaultValue);
    }
    
    @Override // This needs to override so that set() is used to change the value.
    public boolean parse(String str) {
        return set(parseImpl(str));
    }
    
    @Override
    public boolean set(T value) {
        if (!isValueValid(value)) {
            return false;
        }
        
        if (this.value != null) {
            ModeImpl<P> modeImpl = this.value.getModeImpl();
            boolean shouldSubscribe = modeImpl.getParent().getRunInMainMenu() || Utils.canUpdate();
            
            // Unsubscribe old value ModeImpl
            if (modeImpl.subscribed && shouldSubscribe) {
                MeteorClient.EVENT_BUS.unsubscribe(modeImpl);
                modeImpl.subscribed = false;
                modeImpl.onUnselect();
            }
            
            this.value = value;
            modeImpl = this.value.getModeImpl();
            
            // Subscribe new value ModeImpl
            if (!modeImpl.subscribed && shouldSubscribe) {
                MeteorClient.EVENT_BUS.subscribe(modeImpl);
                modeImpl.subscribed = true;
                modeImpl.onSelect();
            }
        } else {
            this.value = value;
        }
        
        onChanged();
        return true;
    }
    
    public static class Builder<T extends Enum<T> & IDisplayName & IModeImpl<P>, P extends IRunInMainMenu> extends SettingBuilder<Builder<T, P>, T, ModeEnumChoiceSetting<T, P>> {
        
        public Builder() {
            super(null);
        }
        
        @Override
        public ModeEnumChoiceSetting<T, P> build() {
            return new ModeEnumChoiceSetting<>(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        }
        
    }
    
    public interface IModeImpl<P extends IRunInMainMenu> {
        
        ModeImpl<P> getModeImpl();
        
    }
    
    /**
     * It is an implementation of the mode in {@code ModeEnumChoiceSetting}. Can listen for events if is selected.
     *
     * @param <P> activable parent.
     */
    public static abstract class ModeImpl<P extends IRunInMainMenu> implements IParent<P> {
        
        protected static final MinecraftClient mc = MeteorClient.mc;
        
        /**
         * A flag that indicates whether a {@code ModeImpl} object is subscribed.
         */
        public boolean subscribed;
        
        private final Supplier<P> parentSupplier;
        private P parent;
        
        public ModeImpl(Supplier<P> parent) {
            this.parentSupplier = parent;
        }
        
        /**
         * Use this to access parent and prevent NPE.
         */
        @Override
        public P getParent() {
            if (parent == null) {
                parent = parentSupplier.get();
            }
            return parent;
        }
        
        /**
         * It will be called when this mode is selected. Similar to {@link Module#onActivate()}.
         */
        protected void onSelect() {}
        
        /**
         * It will be called when this mode is unselected. Similar to {@link Module#onDeactivate()}.
         */
        protected void onUnselect() {}
        
    }
    
}
