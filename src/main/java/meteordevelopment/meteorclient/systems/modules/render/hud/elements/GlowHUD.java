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
import meteordevelopment.meteorclient.utils.render.ui.glow.GlowBuilder;

public class GlowHUD extends HUDElement {

    public static final HUDElementInfo<GlowHUD> INFO = new HUDElementInfo<>(HUD.GROUP, "Glow", "HUD element test.", GlowHUD::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Size

    private final Setting<Integer> width = sgGeneral.add(new IntSetting.Builder()
        .name("width")
        .description("Custom width.")
        .defaultValue(200)
        .min(0)
        .sliderRange(0, 1920)
        .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
        .name("height")
        .description("Custom height.")
        .defaultValue(200)
        .min(0)
        .sliderRange(0, 1080)
        .build()
    );

    // Color

    private final Setting<Boolean> colorEachVertex = sgGeneral.add(new BoolSetting.Builder()
        .name("color-each-vertex")
        .description("Set custom color for each vertex.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Color used for the rectangle glow.")
        .defaultValue(SettingColor.CYAN)
        .visible(() -> !colorEachVertex.get())
        .build()
    );

    private final Setting<SettingColor> colorTopLeft = sgGeneral.add(new ColorSetting.Builder()
        .name("color-top-left")
        .description("Color used for the top left vertex.")
        .defaultValue(SettingColor.CYAN)
        .visible(colorEachVertex::get)
        .build()
    );

    private final Setting<SettingColor> colorBottomLeft = sgGeneral.add(new ColorSetting.Builder()
        .name("color-bottom-left")
        .description("Color used for the bottom left vertex.")
        .defaultValue(SettingColor.BLUE)
        .visible(colorEachVertex::get)
        .build()
    );

    private final Setting<SettingColor> colorBottomRight = sgGeneral.add(new ColorSetting.Builder()
        .name("color-bottom-right")
        .description("Color used for the bottom right vertex.")
        .defaultValue(SettingColor.MAGENTA)
        .visible(colorEachVertex::get)
        .build()
    );

    private final Setting<SettingColor> colorTopRight = sgGeneral.add(new ColorSetting.Builder()
        .name("color-top-right")
        .description("Color used for the top right vertex.")
        .defaultValue(SettingColor.BLUE)
        .visible(colorEachVertex::get)
        .build()
    );

    // Radius

    private final Setting<Boolean> radiusEachVertex = sgGeneral.add(new BoolSetting.Builder()
        .name("radius-each-vertex")
        .description("Set custom radius for each vertex.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Corner radius used for the rectangle.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(() -> !radiusEachVertex.get())
        .build()
    );

    private final Setting<Double> radiusTopLeft = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius-top-left")
        .description("Custom radius for the top left vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(radiusEachVertex::get)
        .build()
    );

    private final Setting<Double> radiusBottomLeft = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius-bottom-left")
        .description("Custom radius for the bottom left vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(radiusEachVertex::get)
        .build()
    );

    private final Setting<Double> radiusBottomRight = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius-bottom-right")
        .description("Custom radius for the bottom right vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(radiusEachVertex::get)
        .build()
    );

    private final Setting<Double> radiusTopRight = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius-top-right")
        .description("Custom radius for the top right vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .visible(radiusEachVertex::get)
        .build()
    );

    // Glow properties
    
    private final Setting<Double> glowRadius = sgGeneral.add(new DoubleSetting.Builder()
        .name("glow-radius")
        .description("Radius of the glow effect.")
        .defaultValue(8)
        .min(0)
        .sliderRange(0, 100)
        .build()
    );

    private final Setting<Double> intensity = sgGeneral.add(new DoubleSetting.Builder()
        .name("intensity")
        .description("Intensity of the glow effect.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .build()
    );
    
    private final Setting<Double> padding = sgGeneral.add(new DoubleSetting.Builder()
        .name("padding")
        .description("Additional size for mask.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 30)
        .build()
    );

    private final Setting<Double> alpha = sgGeneral.add(new DoubleSetting.Builder()
        .name("alpha")
        .description("Alpha (opacity) of the glow effect.")
        .defaultValue(1)
        .min(0)
        .max(1)
        .sliderRange(0, 1)
        .build()
    );

    private final Setting<Boolean> cutout = sgGeneral.add(new BoolSetting.Builder()
        .name("cutout")
        .description("Cut the rectangle out of the glow, leaving only the halo. Disable to let the glow fill the rectangle too.")
        .defaultValue(true)
        .build()
    );

    // Advanced glow options

    private final Setting<Boolean> useBuilder = sgGeneral.add(new BoolSetting.Builder()
        .name("use-builder")
        .description("Use GlowBuilder for advanced glow settings.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> secondColor = sgGeneral.add(new ColorSetting.Builder()
        .name("second-color")
        .description("Second color for gradient glow (requires use-builder).")
        .defaultValue(SettingColor.MAGENTA)
        .visible(() -> useBuilder.get())
        .build()
    );

    private final Setting<Double> colorOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("color-offset")
        .description("Offset between primary and secondary colors (requires use-builder).")
        .defaultValue(0)
        .sliderRange(-1, 1)
        .visible(() -> useBuilder.get())
        .build()
    );

    private final Setting<Integer> splitIndex = sgGeneral.add(new IntSetting.Builder()
        .name("split-index")
        .description("Split index for advanced rendering (requires use-builder).")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 10)
        .visible(() -> useBuilder.get())
        .build()
    );

    public GlowHUD() {
        super(INFO);
    }

    @Override
    public void render(HUDRenderer renderer) {
        setSize(width.get(), height.get());
        
        float glowRadiusValue = glowRadius.get().floatValue();
        float intensityValue = intensity.get().floatValue();
        float alphaValue = alpha.get().floatValue();
        
        GlowBuilder builder = Render2D.glowBuilder()
            .rectangle(
                x - padding.get().floatValue(),
                y - padding.get().floatValue(),
                width.get() + padding.get().floatValue() * 2,
                height.get() + padding.get().floatValue() * 2
            );
        
        if (radiusEachVertex.get()) {
            builder.radius(
                radiusTopLeft.get().floatValue(),
                radiusTopRight.get().floatValue(),
                radiusBottomRight.get().floatValue(),
                radiusBottomLeft.get().floatValue()
            );
        } else {
            builder.radius(radius.get().floatValue());
        }
        
        if (colorEachVertex.get()) {
            builder.color(
                colorTopLeft.get().getPacked(),
                colorTopRight.get().getPacked(),
                colorBottomRight.get().getPacked(),
                colorBottomLeft.get().getPacked()
            );
        } else {
            builder.color(color.get().getPacked());
        }
        
        builder.intensity(intensityValue)
            .glowRadius(glowRadiusValue)
            .alpha(alphaValue)
            .cutout(cutout.get());
        
        if (useBuilder.get()) {
            builder.secondColor(secondColor.get().getPacked(), colorOffset.get().floatValue())
                .splitIndex(splitIndex.get());
        }
        
        Render2D.glow(builder.build());
    }

}