package meteordevelopment.meteorclient.systems.modules.render.hud.elements;

import meteordevelopment.meteorclient.renderer.color.SettingColor;
import meteordevelopment.meteorclient.renderer.state.QuadColorState;
import meteordevelopment.meteorclient.renderer.state.QuadRadiusState;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.*;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElement;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElementInfo;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDRenderer;
import net.minecraft.util.Identifier;

public class TextureHUD extends HUDElement {
    
    public static final HUDElementInfo<TextureHUD> INFO = new HUDElementInfo<>(HUD.GROUP, "Texture", "HUD element test.", TextureHUD::new);
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    
    // Texture
    
    public final Setting<String> texture = sgGeneral.add(new StringSetting.Builder()
        .name("Texture")
        .description("The texture resource path.")
        .defaultValue("meteor-client:textures/meteor.png")
        .build()
    );
    
    // Size
    
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
    
    // Color
    
    private final Setting<Boolean> colorEachVertex = sgGeneral.add(new BoolSetting.Builder()
        .name("ColorEachVertex")
        .description("Set custom color for each vertex.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("Color")
        .description("Color used for the rectangle.")
        .defaultValue(SettingColor.RED)
        .visible(() -> !colorEachVertex.get())
        .build()
    );
    
    private final Setting<SettingColor> colorTopLeft = sgGeneral.add(new ColorSetting.Builder()
        .name("ColorTopLeft")
        .description("Color used for the top left vertex.")
        .defaultValue(SettingColor.CYAN)
        .visible(colorEachVertex::get)
        .build()
    );
    
    private final Setting<SettingColor> colorBottomLeft = sgGeneral.add(new ColorSetting.Builder()
        .name("ColorBottomLeft")
        .description("Color used for the bottom left vertex.")
        .defaultValue(SettingColor.BLUE)
        .visible(colorEachVertex::get)
        .build()
    );
    
    private final Setting<SettingColor> colorBottomRight = sgGeneral.add(new ColorSetting.Builder()
        .name("ColorBottomRight")
        .description("Color used for the bottom right vertex.")
        .defaultValue(SettingColor.MAGENTA)
        .visible(colorEachVertex::get)
        .build()
    );
    
    private final Setting<SettingColor> colorTopRight = sgGeneral.add(new ColorSetting.Builder()
        .name("ColorTopRight")
        .description("Color used for the top right vertex.")
        .defaultValue(SettingColor.BLUE)
        .visible(colorEachVertex::get)
        .build()
    );
    
    // Radius
    
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
    
    // Smoothness
    
    private final Setting<Double> smoothness = sgGeneral.add(new DoubleSetting.Builder()
        .name("Smoothness")
        .description("Smoothing edges by alpha channel interpolation.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );
    
    public TextureHUD() {
        super(INFO);
    }
    
    @Override
    public void render(HUDRenderer renderer) {
        setSize(width.get(), height.get());
        
        QuadColorState colorState = colorEachVertex.get()
            ? QuadColorState.of(
                colorTopLeft.get(),
                colorBottomLeft.get(),
                colorBottomRight.get(),
                colorTopRight.get()
            )
            : QuadColorState.of(color.get());
        
        QuadRadiusState radiusState = radiusEachVertex.get()
            ? QuadRadiusState.of(
                radiusTopLeft.get(),
                radiusBottomLeft.get(),
                radiusBottomRight.get(),
                radiusTopRight.get()
            )
            : QuadRadiusState.of(radius.get());
        
        Identifier id = Identifier.tryParse(texture.get());
        
        if (id == null) {
            return;
        }
        
        renderer.textureTest(
            id,
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