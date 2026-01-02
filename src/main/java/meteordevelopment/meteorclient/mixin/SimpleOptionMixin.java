/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.ISimpleOption;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin implements ISimpleOption {
    
    @Shadow
    Object value;
    
    @Shadow
    @Final
    private Consumer<Object> changeCallback;
    
    @Override
    public void meteor$set(Object value) {
        if (!mc.isRunning()) {
            this.value = value;
        } else {
            if (!Objects.equals(this.value, value)) {
                this.value = value;
                this.changeCallback.accept(this.value);
            }
        }
    }
    
}
