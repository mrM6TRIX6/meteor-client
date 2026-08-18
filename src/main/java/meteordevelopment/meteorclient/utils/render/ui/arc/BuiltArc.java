package meteordevelopment.meteorclient.utils.render.ui.arc;

import net.minecraft.client.gui.DrawContext;

import java.util.Arrays;

public record BuiltArc(
    float x,
    float y,
    float size,
    float thickness,
    float degree,
    float rotation,
    int[] colors
) {
    
    public BuiltArc {
        colors = normalizeColors(colors);
    }
    
    public BuiltArc(float x, float y, float size, float thickness, float degree, float rotation, int color) {
        this(x, y, size, thickness, degree, rotation, new int[] { color });
    }
    
    public void render(DrawContext context) {
        ArcRenderer.getInstance().draw(context, this);
    }
    
    public boolean visible() {
        return size > 0.0f && thickness > 0.0f && degree > 0.0f && hasVisibleColor();
    }
    
    private boolean hasVisibleColor() {
        for (int color : colors) {
            if ((color >>> 24) != 0) {
                return true;
            }
        }
        return false;
    }
    
    private static int[] normalizeColors(int[] colors) {
        if (colors == null || colors.length == 0) {
            return new int[9];
        }
        if (colors.length == 1) {
            int color = colors[0];
            int[] result = new int[9];
            Arrays.fill(result, color);
            return result;
        }
        int[] result = new int[9];
        for (int i = 0; i < result.length; i++) {
            result[i] = i < colors.length ? colors[i] : colors[colors.length - 1];
        }
        return result;
    }
    
}
