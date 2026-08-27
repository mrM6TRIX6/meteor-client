/*
 * This file is part of the Meteor Client distribution (https:github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.elements;

import meteordevelopment.meteorclient.renderer.color.SettingColor;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.ColorSetting;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElement;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElementInfo;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDRenderer;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectrotatinggradient.BuiltRotatingGradientRectangle;

public class RotatingGradientRectangle extends HUDElement {
    
    public static final HUDElementInfo<RotatingGradientRectangle> INFO = new HUDElementInfo<>(HUD.GROUP, "RotatingGradientRectangle", "HUD element test.", RotatingGradientRectangle::new);
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Integer> width = sgGeneral.add(new IntSetting.Builder()
        .name("Width")
        .description("Custom width.")
        .defaultValue(200)
        .min(0)
        .sliderRange(0, 1920)
        .build()
    );
    
    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
        .name("Height")
        .description("Custom height.")
        .defaultValue(200)
        .min(0)
        .sliderRange(0, 1080)
        .build()
    );
    
    private final Setting<SettingColor> colorTopLeft = sgGeneral.add(new ColorSetting.Builder()
        .name("ColorTopLeft")
        .description("Color used for the top left vertex.")
        .defaultValue(SettingColor.CYAN)
        .build()
    );
    
    private final Setting<SettingColor> colorBottomLeft = sgGeneral.add(new ColorSetting.Builder()
        .name("ColorBottomLeft")
        .description("Color used for the bottom left vertex.")
        .defaultValue(SettingColor.BLUE)
        .build()
    );
    
    private final Setting<SettingColor> colorBottomRight = sgGeneral.add(new ColorSetting.Builder()
        .name("ColorBottomRight")
        .description("Color used for the bottom right vertex.")
        .defaultValue(SettingColor.MAGENTA)
        .build()
    );
    
    private final Setting<SettingColor> colorTopRight = sgGeneral.add(new ColorSetting.Builder()
        .name("ColorTopRight")
        .description("Color used for the top right vertex.")
        .defaultValue(SettingColor.BLUE)
        .build()
    );
    
    private final Setting<Boolean> radiusEachVertex = sgGeneral.add(new BoolSetting.Builder()
        .name("RadiusEachVertex")
        .description("Set custom radius for each vertex.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
        .name("Radius")
        .description("Radius used for the rectangle.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(() -> !radiusEachVertex.get())
        .build()
    );
    
    private final Setting<Double> radiusTopLeft = sgGeneral.add(new DoubleSetting.Builder()
        .name("RadiusTopLeft")
        .description("Custom radius for the top left vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(radiusEachVertex::get)
        .build()
    );
    
    private final Setting<Double> radiusBottomLeft = sgGeneral.add(new DoubleSetting.Builder()
        .name("RadiusBottomLeft")
        .description("Custom radius for the bottom left vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(radiusEachVertex::get)
        .build()
    );
    
    private final Setting<Double> radiusBottomRight = sgGeneral.add(new DoubleSetting.Builder()
        .name("RadiusBottomRight")
        .description("Custom radius for the bottom right vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(radiusEachVertex::get)
        .build()
    );
    
    private final Setting<Double> radiusTopRight = sgGeneral.add(new DoubleSetting.Builder()
        .name("RadiusTopRight")
        .description("Custom radius for the top right vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(radiusEachVertex::get)
        .build()
    );
    
    private final Setting<Double> smoothness = sgGeneral.add(new DoubleSetting.Builder()
        .name("Smoothness")
        .description("Smoothing edges by alpha channel interpolation.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );
    
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("Speed")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );
    
    public RotatingGradientRectangle() {
        super(INFO);
    }
    
    @Override
    public void render(HUDRenderer renderer) {
        setSize(width.get(), height.get());
        
        Render2D.rotatingGradientRect(
            new BuiltRotatingGradientRectangle(
                x,
                y,
                width.get(),
                height.get(),
                radius.get().floatValue(),
                colorTopRight.get().getPacked(),
                colorTopLeft.get().getPacked(),
                colorBottomLeft.get().getPacked(),
                colorBottomRight.get().getPacked(),
                speed.get().floatValue()
            ).withSmoothness(smoothness.get().floatValue())
        );
        
    }
    
}
