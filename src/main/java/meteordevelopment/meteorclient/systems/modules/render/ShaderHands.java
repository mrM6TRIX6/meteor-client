/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import meteordevelopment.meteorclient.utils.render.post.handsflame.HandsFlameRenderer;
import meteordevelopment.meteorclient.utils.render.post.shaderhands.ShaderHandsRenderer;
import meteordevelopment.orbit.EventHandler;

public class ShaderHands extends Module {
    
    private static final int CLIENT_COLOR = ColorUtil.rgba(145, 61, 226, 255);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgGlass = settings.createGroup("Glass");
    private final SettingGroup sgGlow = settings.createGroup("Glow");
    private final SettingGroup sgFlame = settings.createGroup("Flame");
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("Mode")
        .description("Which shader used. Glass is compatible with both.")
        .defaultValue(Mode.GLOW)
        .build()
    );
    
    // Glass
    
    private final Setting<Boolean> glass = sgGlass.add(new BoolSetting.Builder()
        .name("Glass")
        .description("World reflection through the model with distortion.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Double> glassSaturation = sgGlass.add(new DoubleSetting.Builder()
        .name("GlassSaturation")
        .description("Saturation of the world reflection in hands.")
        .visible(glass::get)
        .defaultValue(1.45)
        .range(0, 3)
        .sliderMax(3)
        .build()
    );
    
    private final Setting<Double> glassBrightness = sgGlass.add(new DoubleSetting.Builder()
        .name("GlassBrightness")
        .description("Lift to white: 0 = dark, 1 = white.")
        .visible(glass::get)
        .defaultValue(0.78)
        .range(0, 1)
        .sliderMax(1)
        .build()
    );
    
    private final Setting<Double> glassDistortion = sgGlass.add(new DoubleSetting.Builder()
        .name("GlassDistortion")
        .description("Reflection distortion strength. 0 = flat glass.")
        .visible(glass::get)
        .defaultValue(0.012)
        .range(0, 0.05)
        .sliderMax(0.05)
        .build()
    );
    
    private final Setting<Double> glassTint = sgGlass.add(new DoubleSetting.Builder()
        .name("GlassTint")
        .description("Tints the reflection with the client color.")
        .visible(glass::get)
        .defaultValue(0.22)
        .range(0, 1)
        .sliderMax(1)
        .build()
    );

    private final Setting<Boolean> glassFlip = sgGlass.add(new BoolSetting.Builder()
        .name("GlassFlip")
        .description("Flip the world reflection vertically.")
        .visible(glass::get)
        .defaultValue(false)
        .build()
    );
    
    // Glow
    
    private final Setting<Boolean> glow = sgGlow.add(new BoolSetting.Builder()
        .name("Glow")
        .description("Draw glow/outline around hands.")
        .visible(() -> mode.get() == Mode.GLOW)
        .defaultValue(false)
        .build()
    );
    
    private final Setting<GlowMode> glowMode = sgGlow.add(new EnumChoiceSetting.Builder<GlowMode>()
        .name("GlowMode")
        .description("What to draw: glow only, outline only, or both.")
        .visible(() -> mode.get() == Mode.GLOW && glow.get())
        .defaultValue(GlowMode.GLOW)
        .build()
    );
    
    private final Setting<Integer> glowRadius = sgGlow.add(new IntSetting.Builder()
        .name("GlowRadius")
        .description("Glow radius around hands.")
        .visible(() -> mode.get() == Mode.GLOW && glow.get())
        .defaultValue(6)
        .sliderRange(1, 12)
        .build()
    );
    
    private final Setting<Double> glowStrength = sgGlow.add(new DoubleSetting.Builder()
        .name("GlowStrength")
        .description("Brightness of the outer glow.")
        .visible(() -> mode.get() == Mode.GLOW && glow.get())
        .defaultValue(1.8)
        .range(0.1, 3)
        .sliderMax(3)
        .build()
    );
    
    private final Setting<Boolean> blending = sgGlow.add(new BoolSetting.Builder()
        .name("Blending")
        .description("Additively blend glow into the scene (like light/bloom).")
        .visible(() -> mode.get() == Mode.GLOW && glow.get())
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Double> outlineWidth = sgGlow.add(new DoubleSetting.Builder()
        .name("OutlineWidth")
        .description("Outline thickness around hands.")
        .visible(() -> mode.get() == Mode.GLOW && glow.get() && glowMode.get() != GlowMode.GLOW)
        .defaultValue(0.3)
        .range(0.1, 0.5)
        .sliderMax(0.5)
        .build()
    );
    
    private final Setting<GlowColorMode> glowColorMode = sgGlow.add(new EnumChoiceSetting.Builder<GlowColorMode>()
        .name("GlowColorMode")
        .description("Glow/outline color mode: rainbow, client color, or custom.")
        .visible(() -> mode.get() == Mode.GLOW && glow.get())
        .defaultValue(GlowColorMode.CLIENT)
        .build()
    );
    
    private final Setting<Boolean> glowSecondColor = sgGlow.add(new BoolSetting.Builder()
        .name("GlowSecondColor")
        .description("Use a second custom color (gradient).")
        .visible(() -> mode.get() == Mode.GLOW && glow.get() && glowColorMode.get() == GlowColorMode.CUSTOM)
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Color> glowColor = sgGlow.add(new ColorSetting.Builder()
        .name("GlowColor")
        .description("Primary glow color for hands.")
        .visible(() -> mode.get() == Mode.GLOW && glow.get() && glowColorMode.get() == GlowColorMode.CUSTOM)
        .defaultValue(new Color(91, 108, 249, 255))
        .build()
    );
    
    private final Setting<Color> glowColor2 = sgGlow.add(new ColorSetting.Builder()
        .name("GlowColor2")
        .description("Secondary glow color for hands.")
        .visible(() -> mode.get() == Mode.GLOW && glow.get() && glowColorMode.get() == GlowColorMode.CUSTOM && glowSecondColor.get())
        .defaultValue(new Color(255, 50, 150, 255))
        .build()
    );
    
    // Flame
    
    private final Setting<Boolean> flame = sgFlame.add(new BoolSetting.Builder()
        .name("Flame")
        .description("Enables shader flame around hands.")
        .visible(() -> mode.get() == Mode.FLAME)
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> onlyWithItem = sgFlame.add(new BoolSetting.Builder()
        .name("OnlyWithItem")
        .description("Draws flame only when holding an item.")
        .visible(() -> mode.get() == Mode.FLAME && flame.get())
        .defaultValue(false)
        .build()
    );
    
    private final Setting<FlameColorMode> flameColorMode = sgFlame.add(new EnumChoiceSetting.Builder<FlameColorMode>()
        .name("FlameColorMode")
        .description("Flame color mode for hands.")
        .visible(() -> mode.get() == Mode.FLAME && flame.get())
        .defaultValue(FlameColorMode.ITEM)
        .build()
    );
    
    private final Setting<Color> flameColor = sgFlame.add(new ColorSetting.Builder()
        .name("FlameColor")
        .description("Custom flame color for hands.")
        .visible(() -> mode.get() == Mode.FLAME && flame.get() && flameColorMode.get() == FlameColorMode.CUSTOM)
        .defaultValue(new Color(255, 255, 255, 255))
        .build()
    );
    
    private final Setting<Double> flameStrength = sgFlame.add(new DoubleSetting.Builder()
        .name("FlameStrength")
        .description("Intensity of the hand flame.")
        .visible(() -> mode.get() == Mode.FLAME && flame.get())
        .defaultValue(0.85)
        .range(0, 2)
        .sliderMax(2)
        .build()
    );
    
    private final Setting<Double> flameRiseSpeed = sgFlame.add(new DoubleSetting.Builder()
        .name("FlameRiseSpeed")
        .description("Rise speed of the hand flame.")
        .visible(() -> mode.get() == Mode.FLAME && flame.get())
        .defaultValue(0.0)
        .range(0, 2)
        .sliderMax(2)
        .build()
    );
    
    private final Setting<Double> flameWobble = sgFlame.add(new DoubleSetting.Builder()
        .name("FlameWobble")
        .description("Lateral turbulence of the hand flame.")
        .visible(() -> mode.get() == Mode.FLAME && flame.get())
        .defaultValue(0.65)
        .range(0, 2)
        .sliderMax(2)
        .build()
    );
    
    private final Setting<Double> flameLength = sgFlame.add(new DoubleSetting.Builder()
        .name("FlameLength")
        .description("Trail length of the hand flame.")
        .visible(() -> mode.get() == Mode.FLAME && flame.get())
        .defaultValue(0.95)
        .range(0.1, 2.5)
        .sliderMax(2.5)
        .build()
    );
    
    private final Setting<Double> flameBrightness = sgFlame.add(new DoubleSetting.Builder()
        .name("FlameBrightness")
        .description("Brightness of the hand flame.")
        .visible(() -> mode.get() == Mode.FLAME && flame.get())
        .defaultValue(0.9)
        .range(0, 2)
        .sliderMax(2)
        .build()
    );
    
    public ShaderHands() {
        super(Category.RENDER, "ShaderHands", "Applies shaders to the hands framebuffer.");
    }
    
    @Override
    public void onActivate() {
        syncFlameRenderer();
    }
    
    @Override
    public void onDeactivate() {
        HandsFlameRenderer.setFlameEnabled(false);
        HandsFlameRenderer.shutdown();
        ShaderHandsRenderer.clear();
    }
    
    @EventHandler
    private void onTick(TickEvent.Pre tickEvent) {
        syncFlameRenderer();
    }
    
    public void composite() {
        if (!isActive()) {
            return;
        }
        
        boolean doGlass = glass.get();
        boolean doGlow = mode.get() == Mode.GLOW && glow.get();
        
        int glowFlags = glowMode.get() == GlowMode.OUTLINE ? 1 : (glowMode.get() == GlowMode.BOTH ? 2 : 0);
        
        ShaderHandsRenderer.composite(
            baseColor(),
            glowGradientColors(),
            doGlass,
            doGlow,
            glowFlags,
            glowRadius.get().floatValue(),
            outlineWidth.get().floatValue(),
            glowStrength.get().floatValue(),
            blending.get(),
            glassSaturation.get().floatValue(),
            glassBrightness.get().floatValue(),
            glassDistortion.get().floatValue(),
            glassTint.get().floatValue(),
            glassFlip.get()
        );
    }
    
    private int baseColor() {
        return CLIENT_COLOR;
    }
    
    private int glowFade(int n, int n2, int n3) {
        int n4 = (int) ((System.currentTimeMillis() / 8L + (long) n) % 360L);
        n4 = n4 >= 180 ? 360 - n4 : n4;
        return ColorUtil.lerpColor(n2, n3, (float) n4 / 180.0f);
    }
    
    private int[] glowGradientColors() {
        return new int[] { this.glowColorAt(0), this.glowColorAt(90), this.glowColorAt(180), this.glowColorAt(270) };
    }
    
    private void syncFlameRenderer() {
        if (!isFlameMode()) {
            HandsFlameRenderer.setFlameEnabled(false);
            return;
        }
        
        HandsFlameRenderer.setFlameEnabled(flame.get());
        HandsFlameRenderer.configure(
            flameStrength.get().floatValue(),
            flameRiseSpeed.get().floatValue(),
            flameWobble.get().floatValue(),
            flameLength.get().floatValue(),
            flameBrightness.get().floatValue(),
            flameColorMode.get() == FlameColorMode.ITEM ? 0 : 1,
            resolveFlameColor(),
            onlyWithItem.get(),
            flameColorMode.get() == FlameColorMode.CLIENT
        );
    }
    
    private int glowColorAt(int n) {
        int n2;
        int n3;
        
        if (glowColorMode.get() == GlowColorMode.RAINBOW) {
            return glowRainbow(n);
        }
        
        if (glowColorMode.get() == GlowColorMode.CUSTOM) {
            n3 = ColorUtil.withAlpha(glowColor.get().getPacked(), 255);
            n2 = glowSecondColor.get() ? ColorUtil.withAlpha(glowColor2.get().getPacked(), 255) : n3;
        } else {
            n3 = CLIENT_COLOR;
            n2 = CLIENT_COLOR;
        }
        
        if (n3 == n2) {
            return n3 | 0xFF000000;
        }
        
        return glowFade(n, n3, n2) | 0xFF000000;
    }
    
    private int glowRainbow(int n) {
        int n2 = (int) ((System.currentTimeMillis() / 8L + (long) n) % 360L);
        float hue = (((n2 % 360) + 360) % 360) / 360.0f;
        int n3 = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);
        return 0xFF000000 | n3 & 0xFFFFFF;
    }
    
    public boolean isGlowMode() {
        if (!isActive()) {
            return false;
        }
        if (glass.get()) {
            return true;
        }
        return mode.get() == Mode.GLOW && glow.get();
    }
    
    public boolean isFlameMode() {
        return isActive() && mode.get() == Mode.FLAME && flame.get();
    }
    
    public boolean needsHandCapture() {
        return isGlowMode() || isFlameMode();
    }
    
    public boolean needsComposite() {
        if (!isActive()) {
            return false;
        }
        return glass.get() || (mode.get() == Mode.GLOW && glow.get());
    }
    
    private int resolveFlameColor() {
        return switch (flameColorMode.get()) {
            case CUSTOM -> ColorUtil.withAlpha(flameColor.get().getPacked(), 255);
            case CLIENT -> baseColor();
            case RAINBOW -> 0xFF000000 | java.awt.Color.HSBtoRGB((float) (System.currentTimeMillis() % 4000L) / 4000.0f, 0.72f, 1.0f) & 0xFFFFFF;
            // ITEM samples the held item in the trail shader; keep opaque fallback
            case ITEM -> ColorUtil.withAlpha(flameColor.get().getPacked(), 255);
        };
    }
    
    private enum Mode {
        GLOW,
        FLAME
    }
    
    private enum GlowMode {
        GLOW,
        OUTLINE,
        BOTH
    }
    
    private enum GlowColorMode {
        RAINBOW,
        CLIENT,
        CUSTOM
    }
    
    private enum FlameColorMode {
        ITEM,
        CUSTOM,
        CLIENT,
        RAINBOW
    }
    
}
