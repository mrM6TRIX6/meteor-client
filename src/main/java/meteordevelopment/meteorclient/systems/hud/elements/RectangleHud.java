package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.ColorSetting;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.state.QuadColorState;
import meteordevelopment.meteorclient.utils.render.state.QuadRadiusState;

public class RectangleHud extends HudElement {
    
    public static final HudElementInfo<RectangleHud> INFO = new HudElementInfo<>(Hud.GROUP, "rectangle", "HUD element test.", RectangleHud::new);
    
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
    
    private final Setting<SettingColor> colorTopLeft = sgGeneral.add(new ColorSetting.Builder()
        .name("color-top-left")
        .description("Color used for the top left vertex.")
        .defaultValue(SettingColor.RED)
        .build()
    );
    
    private final Setting<SettingColor> colorTopRight = sgGeneral.add(new ColorSetting.Builder()
        .name("color-top-right")
        .description("Color used for the top right vertex.")
        .defaultValue(SettingColor.RED)
        .build()
    );
    
    private final Setting<SettingColor> colorBottomRight = sgGeneral.add(new ColorSetting.Builder()
        .name("color-bottom-right")
        .description("Color used for the bottom right vertex.")
        .defaultValue(SettingColor.RED)
        .build()
    );
    
    private final Setting<SettingColor> colorBottomLeft = sgGeneral.add(new ColorSetting.Builder()
        .name("color-bottom-left")
        .description("Color used for the bottom left vertex.")
        .defaultValue(SettingColor.RED)
        .build()
    );
    
    // Radius
    
    private final Setting<Double> radiusTopLeft = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius-top-left")
        .description("Custom radius for the top left vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );
    
    private final Setting<Double> radiusTopRight = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius-top-right")
        .description("Custom radius for the top right vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );
    
    private final Setting<Double> radiusBottomLeft = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius-bottom-left")
        .description("Custom radius for the bottom left vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );
    
    private final Setting<Double> radiusBottomRight = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius-bottom-right")
        .description("Custom radius for the bottom right vertex.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 20)
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
    
    public RectangleHud() {
        super(INFO);
    }
    
    @Override
    public void render(HudRenderer renderer) {
        setSize(width.get(), height.get());
        
        Renderer2D.COLOR.rectangle(
            x,
            y,
            width.get(),
            height.get(),
            new QuadColorState(
                colorTopLeft.get(),
                colorTopRight.get(),
                colorBottomRight.get(),
                colorBottomLeft.get()
            ),
            new QuadRadiusState(
                radiusTopLeft.get(),
                radiusTopRight.get(),
                radiusBottomRight.get(),
                radiusBottomLeft.get()
            ),
            smoothness.get()
        );
    }
    
}