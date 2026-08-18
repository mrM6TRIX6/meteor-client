package meteordevelopment.meteorclient.utils.render.item;

public record RenderItemOptions(
    float alpha,
    int color,
    RenderItemGlintMode glintMode,
    boolean showCount,
    boolean showDurability,
    float glintStrength
) {
    
    private static final RenderItemOptions DEFAULTS = new RenderItemOptions(
        1.0f,
        0xFFFFFFFF,
        RenderItemGlintMode.AUTO,
        true,
        true,
        0.62f
    );
    private static final RenderItemOptions COUNT_NO_DURABILITY = new RenderItemOptions(
        1.0f,
        0xFFFFFFFF,
        RenderItemGlintMode.AUTO,
        true,
        false,
        0.62f
    );
    private static final RenderItemOptions NO_DECORATIONS = new RenderItemOptions(
        1.0f,
        0xFFFFFFFF,
        RenderItemGlintMode.AUTO,
        false,
        false,
        0.62f
    );
    
    public RenderItemOptions {
        alpha = clamp(alpha, 0.0f, 1.0f);
        color = normalizeColor(color);
        glintMode = glintMode == null ? RenderItemGlintMode.AUTO : glintMode;
        glintStrength = clamp(glintStrength, 0.0f, 1.0f);
    }
    
    public static RenderItemOptions defaults() {
        return DEFAULTS;
    }
    
    public static RenderItemOptions decorated(float alpha) {
        return alpha >= 0.999f ? DEFAULTS : new RenderItemOptions(alpha, 0xFFFFFFFF, RenderItemGlintMode.AUTO, true, true, 0.62f);
    }
    
    public static RenderItemOptions countNoDurability(float alpha) {
        return alpha >= 0.999f ? COUNT_NO_DURABILITY : new RenderItemOptions(alpha, 0xFFFFFFFF, RenderItemGlintMode.AUTO, true, false, 0.62f);
    }
    
    public static RenderItemOptions noDecorations(float alpha) {
        return alpha >= 0.999f ? NO_DECORATIONS : new RenderItemOptions(alpha, 0xFFFFFFFF, RenderItemGlintMode.AUTO, false, false, 0.62f);
    }
    
    public RenderItemOptions alpha(float alpha) {
        return new RenderItemOptions(alpha, color, glintMode, showCount, showDurability, glintStrength);
    }
    
    public RenderItemOptions color(int color) {
        return new RenderItemOptions(alpha, color, glintMode, showCount, showDurability, glintStrength);
    }
    
    public RenderItemOptions glint(RenderItemGlintMode mode) {
        return new RenderItemOptions(alpha, color, mode, showCount, showDurability, glintStrength);
    }
    
    public RenderItemOptions count(boolean showCount) {
        return new RenderItemOptions(alpha, color, glintMode, showCount, showDurability, glintStrength);
    }
    
    public RenderItemOptions durability(boolean showDurability) {
        return new RenderItemOptions(alpha, color, glintMode, showCount, showDurability, glintStrength);
    }
    
    public RenderItemOptions glintStrength(float glintStrength) {
        return new RenderItemOptions(alpha, color, glintMode, showCount, showDurability, glintStrength);
    }
    
    private static int normalizeColor(int color) {
        if ((color & 0xFF000000) == 0 && (color & 0x00FFFFFF) != 0) {
            return color | 0xFF000000;
        }
        return color;
    }
    
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
}
