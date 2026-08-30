/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.elements;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.ColorSetting;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElement;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;

public class Test extends HUDElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Color> color = sgGeneral.add(
        new ColorSetting.Builder()
            .name("Color")
            .defaultValue(Color.RED)
            .build()
    );
    
    public Test() {
        super("Test");
    }

    @Override
    public void render() {
        Render2D.glow(getX(), getY(), getWidth(), getHeight(), 10f, 20f, 1, color.get().getPacked());
        Render2D.rect(getX(), getY(), getWidth(), getHeight(), 10f, color.get().getPacked());
    }

}
