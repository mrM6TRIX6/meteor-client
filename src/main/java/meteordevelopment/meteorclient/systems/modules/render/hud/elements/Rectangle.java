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
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectdefault.BuiltRectangle;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.awt.*;

public class Rectangle extends HUDElement {
    
    public static final HUDElementInfo<Rectangle> INFO = new HUDElementInfo<>(HUD.GROUP, "Rectangle", "HUD element test.", Rectangle::new);
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
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
    
    private final Setting<Boolean> colorEachVertex = sgGeneral.add(new BoolSetting.Builder()
        .name("color-each-vertex")
        .description("Set custom color for each vertex.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Color used for the rectangle.")
        .defaultValue(SettingColor.RED)
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
    
    private final Setting<Boolean> radiusEachVertex = sgGeneral.add(new BoolSetting.Builder()
        .name("radius-each-vertex")
        .description("Set custom radius for each vertex.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Radius used for the rectangle.")
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
    
    private final Setting<Double> smoothness = sgGeneral.add(new DoubleSetting.Builder()
        .name("smoothness")
        .description("Smoothing edges by alpha channel interpolation.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );
    
    public Rectangle() {
        super(INFO);
    }
    
    @Override
    public void render(HUDRenderer renderer) {
        setSize(width.get(), height.get());
        
        String message = "Hello, World!";
        MutableText text = Text.empty();
        
        int[] colors = {
            Color.RED.getRGB(),
            Color.YELLOW.getRGB()
        };
        
        int[] colorsRa = {
            Color.RED.getRGB(),
            Color.YELLOW.getRGB(),
            Color.GREEN.getRGB(),
            Color.CYAN.getRGB(),
            Color.BLUE.getRGB(),
            Color.MAGENTA.getRGB()
        };
        
        int[] colorsRa4 = {
            Color.RED.getRGB(),
            Color.YELLOW.getRGB(),
            Color.CYAN.getRGB(),
            0xFF8B00FF
        };
        
        int[] colorsCelka = {
            Color.CYAN.getRGB(),
            Color.BLUE.getRGB(),
            Color.MAGENTA.getRGB()
        };
        
        int[] colorsTest = {
            Color.RED.getRGB(),
            Color.GREEN.getRGB(),
            Color.BLUE.getRGB()
        };
        
        for (int i = 0; i < message.length(); i++) {
            text.append(
                Text.literal(String.valueOf(message.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(
                        ColorUtil.gradient(
                            i * -32,
                            3f,
                            true,
                            colorsRa
                        )
                    ))
            );
        }
        
//        Render2D.rect(
//            new BuiltRectangle(
//                x,
//                y,
//                width.get(),
//                height.get(),
//                radius.get().floatValue(),
//                ColorUtil.gradient(0 , 2, false, colorsCelka),
//                ColorUtil.gradient(90, 2, false, colorsCelka),
//                ColorUtil.gradient(180, 2, false, colorsCelka),
//                ColorUtil.gradient(270, 2, false, colorsCelka)
//            ).withSmoothness(smoothness.get().floatValue())
//        );
        
//        Render2D.rect(
//            new BuiltRectangle(
//                x,
//                y,
//                width.get(),
//                height.get(),
//                radius.get().floatValue(),
//                ColorUtil.gradient(90 , 2, false, colors),
//                ColorUtil.gradient(180, 2, false, colors),
//                ColorUtil.gradient(180, 2, false, colors),
//                ColorUtil.gradient(90, 2, false, colors)
//            ).withSmoothness(smoothness.get().floatValue())
//        );
        
        float pad = 3f;
        int col = color.get().getPacked();
        
        // glow
        
//        Render2D.glow(
//            x - pad,
//            y - pad,
//            width.get() + pad * 2,
//            height.get() + pad * 2,
//            radius.get().floatValue(),
//            50,
//            1,
//            col
//        );
        
        // outline
        
//        Render2D.rect(
//            x - 1,
//            y - 1,
//            width.get() + 2,
//            height.get() + 2,
//            glowRadius.get().floatValue(),
//            col
//        );
//
        Render2D.rect(
            new BuiltRectangle(
                x,
                y,
                width.get(),
                height.get(),
                radiusEachVertex.get() ? radiusTopLeft.get().floatValue() : radius.get().floatValue(),
                radiusEachVertex.get() ? radiusTopRight.get().floatValue() : radius.get().floatValue(),
                radiusEachVertex.get() ? radiusBottomRight.get().floatValue() : radius.get().floatValue(),
                radiusEachVertex.get() ? radiusBottomLeft.get().floatValue() : radius.get().floatValue(),
                colorEachVertex.get() ? colorTopLeft.get().getPacked() : color.get().getPacked(),
                colorEachVertex.get() ? colorTopRight.get().getPacked() : color.get().getPacked(),
                colorEachVertex.get() ? colorBottomRight.get().getPacked() : color.get().getPacked(),
                colorEachVertex.get() ? colorBottomLeft.get().getPacked() : color.get().getPacked(),
                smoothness.get().floatValue()
            )
        );
        
//        Render2D.msdf(
//            new BuiltMsdf(
//                MsdfFont.MONTSERRAT_MEDIUM,
//                text,
//                x + 20,
//                y + 20,
//                16
//            )
//                .withFade(
//                x + 20,
//                x + 100,
//                10.0f,
//                1.0f,
//                1.0f
//            )
        //);
    }
    
}
