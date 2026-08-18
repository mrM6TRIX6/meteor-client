package meteordevelopment.meteorclient.utils.render.world.targetesp;

import net.minecraft.util.math.MathHelper;

public final class TargetEspMath {
    
    private TargetEspMath() {}
    
    public static float approach(float current, float target, float delta) {
        if (current < target) {
            return Math.min(current + delta, target);
        }
        return Math.max(current - delta, target);
    }
    
    public static float easeOutCubic(float value) {
        float clamped = MathHelper.clamp(value, 0.0f, 1.0f);
        float inv = 1.0f - clamped;
        return 1.0f - (inv * inv * inv);
    }
    
}
