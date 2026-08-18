package meteordevelopment.meteorclient.utils.render.color;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public final class ColorUtil {
    
    public static final int WHITE = rgba(255, 255, 255, 255);
    public static final int TRANSPARENT = rgba(0, 0, 0, 0);
    
    private ColorUtil() {}
    
    public static int rgba(int red, int green, int blue, int alpha) {
        return (clamp(alpha) << 24)
            | (clamp(red) << 16)
            | (clamp(green) << 8)
            | clamp(blue);
    }
    
    public static int rgba(int red, int green, int blue, int alpha, float alphaMultiplier) {
        return rgba(red, green, blue, Math.round(alpha * alphaMultiplier));
    }
    
    public static float[] rgba(final int color) {
        return new float[] {
            (color >> 16 & 0xFF) / 255f,
            (color >> 8 & 0xFF) / 255f,
            (color & 0xFF) / 255f,
            (color >> 24 & 0xFF) / 255f
        };
    }
    
    public static int getAlpha(int color) {
        return (color >>> 24) & 0xFF;
    }
    
    public static int getRed(int color) {
        return (color >>> 16) & 0xFF;
    }
    
    public static int getGreen(int color) {
        return (color >>> 8) & 0xFF;
    }
    
    public static int getBlue(int color) {
        return color & 0xFF;
    }
    
    public static int withAlpha(int color, int alpha) {
        return rgba(getRed(color), getGreen(color), getBlue(color), alpha);
    }
    
    public static int multAlpha(int color, float alphaMultiplier) {
        return withAlpha(color, Math.round(getAlpha(color) * clamp01(alphaMultiplier)));
    }
    
    public static int multRed(int color, float multiplier) {
        float safeMultiplier = Math.max(0.0001f, multiplier);
        return rgba(
            getRed(color),
            Math.round(Math.min(255.0f, getGreen(color) / safeMultiplier)),
            Math.round(Math.min(255.0f, getBlue(color) / safeMultiplier)),
            getAlpha(color)
        );
    }
    
    public static int scaleAlpha(int color, float alphaMultiplier) {
        return multAlpha(color, alphaMultiplier);
    }
    
    public static int lerpColor(int first, int second, float factor) {
        float t = clamp01(factor);
        return rgba(
            Math.round(getRed(first) + (getRed(second) - getRed(first)) * t),
            Math.round(getGreen(first) + (getGreen(second) - getGreen(first)) * t),
            Math.round(getBlue(first) + (getBlue(second) - getBlue(first)) * t),
            Math.round(getAlpha(first) + (getAlpha(second) - getAlpha(first)) * t)
        );
    }
    
    public static int interpolateColor(int first, int second, float factor) {
        return lerpColor(first, second, factor);
    }
    
    public static int tintForDamage(int color, float hurtProgress) {
        return lerpColor(color, rgba(255, 60, 60, getAlpha(color)), clamp01(hurtProgress) * 0.45f);
    }
    
    public static int gradient(int index, float speed, boolean normalizeIndex, int... colors) {
        if (colors == null || colors.length == 0) {
            return 0;
        }
        
        if (colors.length == 1) {
            return colors[0];
        }
        
        if (normalizeIndex) {
            index /= colors.length;
        }
        speed /= colors.length;
        
        float time = (System.currentTimeMillis() - MeteorClient.initTime) * (speed / 10.0f);
        
        float phase = ((time + index) % 360.0f) / 360.0f;
        
        if (phase < 0.0f) {
            phase += 1.0f;
        }
        
        float scaled = phase * colors.length;
        
        int index1 = (int) Math.floor(scaled);
        int index2 = (index1 + 1) % colors.length;
        
        float progress = scaled - index1;
        
        return interpolateColor(
            colors[index1],
            colors[index2],
            progress
        );
    }
    
    public static int gradientAlt(int index, float speed, int... colors) {
        if (colors == null || colors.length == 0) {
            return 0;
        }
        if (colors.length == 1) {
            return colors[0];
        }
        
        index /= colors.length;
        speed /= colors.length;
        
        int angle = (int) (((System.currentTimeMillis() - MeteorClient.initTime) * (speed / 10) + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        
        float progress = MathHelper.clamp(angle / 180f - 1f, 0f, 1f);
        
        float scaled = progress * (colors.length - 1);
        
        int index1 = (int) Math.floor(scaled);
        int index2 = Math.min(index1 + 1, colors.length - 1);
        
        float localProgress = scaled - index1;
        
        int color = interpolateColor(
            colors[index1],
            colors[index2],
            localProgress
        );
        
        float[] hs = rgba(color);
        
        float[] hsb = Color.RGBtoHSB(
            (int) (hs[0] * 255),
            (int) (hs[1] * 255),
            (int) (hs[2] * 255),
            null
        );
        
        hsb[1] *= 1.5F;
        hsb[1] = Math.min(hsb[1], 1.0f);
        
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }
    
    private static int clamp(int value) {
        return Math.clamp(value, 0, 255);
    }
    
    private static float clamp01(float value) {
        return Math.clamp(value, 0.0f, 1.0f);
    }
    
}
