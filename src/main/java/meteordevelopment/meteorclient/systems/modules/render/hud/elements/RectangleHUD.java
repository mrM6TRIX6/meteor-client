package meteordevelopment.meteorclient.systems.modules.render.hud.elements;

import it.unimi.dsi.fastutil.ints.IntFloatImmutablePair;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ResolutionChangedEvent;
import meteordevelopment.meteorclient.renderer.*;
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
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.state.QuadColorState;
import meteordevelopment.meteorclient.utils.render.state.QuadRadiusState;
import meteordevelopment.orbit.listeners.ConsumerListener;

public class RectangleHUD extends HUDElement {
    private final IntFloatImmutablePair[] strengths = new IntFloatImmutablePair[]{
        IntFloatImmutablePair.of(1, 1.25f), // LVL 1
        IntFloatImmutablePair.of(1, 2.25f), // LVL 2
        IntFloatImmutablePair.of(2, 2.0f), // LVL 3
        IntFloatImmutablePair.of(2, 3.0f), // LVL 4
        IntFloatImmutablePair.of(2, 4.25f), // LVL 5
        IntFloatImmutablePair.of(3, 2.5f), // LVL 6
        IntFloatImmutablePair.of(3, 3.25f), // LVL 7
        IntFloatImmutablePair.of(3, 4.25f), // LVL 8
        IntFloatImmutablePair.of(3, 5.5f), // LVL 9
        IntFloatImmutablePair.of(4, 3.25f), // LVL 10
        IntFloatImmutablePair.of(4, 4.0f), // LVL 11
        IntFloatImmutablePair.of(4, 5.0f), // LVL 12
        IntFloatImmutablePair.of(4, 6.0f), // LVL 13
        IntFloatImmutablePair.of(4, 7.25f), // LVL 14
        IntFloatImmutablePair.of(4, 8.25f), // LVL 15
        IntFloatImmutablePair.of(5, 4.5f), // LVL 16
        IntFloatImmutablePair.of(5, 5.25f), // LVL 17
        IntFloatImmutablePair.of(5, 6.25f), // LVL 18
        IntFloatImmutablePair.of(5, 7.25f), // LVL 19
        IntFloatImmutablePair.of(5, 8.5f) // LVL 20
    };
    
    public static final HUDElementInfo<RectangleHUD> INFO = new HUDElementInfo<>(HUD.GROUP, "rectangle", "HUD element test.", RectangleHUD::new);
    
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
    
    private final Setting<SettingColor> colorTopRight = sgGeneral.add(new ColorSetting.Builder()
        .name("color-top-right")
        .description("Color used for the top right vertex.")
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
    
    private final Setting<SettingColor> colorBottomLeft = sgGeneral.add(new ColorSetting.Builder()
        .name("color-bottom-left")
        .description("Color used for the bottom left vertex.")
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
    
    private final Setting<Double> radiusTopRight = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius-top-right")
        .description("Custom radius for the top right vertex.")
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
    
    // Smoothness
    
    private final Setting<Double> smoothness = sgGeneral.add(new DoubleSetting.Builder()
        .name("smoothness")
        .description("Smoothing edges by alpha channel interpolation.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );
    
    // Blur
    
    private final Setting<Boolean> blur = sgGeneral.add(new BoolSetting.Builder()
        .name("blur")
        .description("Enable blur.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Integer> iterations = sgGeneral.add(new IntSetting.Builder()
        .name("iterations")
        .description("Blur iterations.")
        .defaultValue(1)
        .range(0, 8)
        .visible(blur::get)
        .build()
    );
    
    private final Setting<Double> offset = sgGeneral.add(new DoubleSetting.Builder()
        .name("offset")
        .description("Blur offset.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 20)
        .visible(blur::get)
        .build()
    );
    
    private final Setting<Double> padding = sgGeneral.add(new DoubleSetting.Builder()
        .name("padding")
        .description("Blur padding.")
        .defaultValue(6)
        .min(0)
        .sliderRange(0, 1000)
        .visible(blur::get)
        .build()
    );
    
    public RectangleHUD() {
        super(INFO);
    }
    
    @Override
    public void render(HUDRenderer renderer) {
        setSize(width.get(), height.get());
        
        QuadColorState colorState = colorEachVertex.get()
            ? new QuadColorState(
                colorTopLeft.get(),
                colorTopRight.get(),
                colorBottomRight.get(),
                colorBottomLeft.get()
            )
            : new QuadColorState(color.get()
        );
        
        QuadRadiusState radiusState = radiusEachVertex.get()
            ? new QuadRadiusState(
                radiusTopLeft.get(),
                radiusTopRight.get(),
                radiusBottomRight.get(),
                radiusBottomLeft.get()
            )
            : new QuadRadiusState(radius.get()
        );
        
        if (blur.get()) {
            renderer.blurredRectangle(
                x - padding.get(),
                y - padding.get(),
                width.get(),
                height.get(),
                colorState,
                radiusState,
                smoothness.get(),
                iterations.get(),
                offset.get(),
                padding.get()
            );
        } else {
            renderer.rectangle(
                x,
                y,
                width.get(),
                height.get(),
                colorState,
                radiusState,
                smoothness.get()
            );
        }
    }
}