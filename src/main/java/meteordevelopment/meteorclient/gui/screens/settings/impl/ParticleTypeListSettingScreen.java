/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.impl;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;

import java.util.List;

public class ParticleTypeListSettingScreen extends CollectionListSettingScreen<ParticleType<?>> {
    
    public ParticleTypeListSettingScreen(GuiTheme theme, Setting<List<ParticleType<?>>> setting) {
        super(theme, "Select Particles", setting, setting.get(), Registries.PARTICLE_TYPE);
    }
    
    protected boolean includeValue(ParticleType<?> value) {
        return value instanceof ParticleEffect;
    }
    
    protected WWidget getValueWidget(ParticleType<?> value) {
        return theme.label(Names.get(value));
    }
    
    protected String[] getValueNames(ParticleType<?> value) {
        return new String[] { Names.get(value), Registries.PARTICLE_TYPE.getId(value).toString() };
    }
    
}
