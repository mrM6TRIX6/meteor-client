package meteordevelopment.meteorclient.utils.render.animation.fx;

public final class AnimationFxMath {
    
    public static final float PI = (float) Math.PI;
    public static final float TAU = PI * 2.0F;
    
    private AnimationFxMath() {}
    
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static float saturate(float value) {
        return clamp(value, 0.0F, 1.0F);
    }
    
    public static float sin01(float value) {
        return (float) (0.5D + 0.5D * Math.sin(value));
    }
    
    public static float cos01(float value) {
        return (float) (0.5D + 0.5D * Math.cos(value));
    }
    
    public static float smooth(float value) {
        value = saturate(value);
        return value * value * (3.0F - 2.0F * value);
    }
    
    public static float smoother(float value) {
        value = saturate(value);
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
    }
    
    public static float triangle(float value) {
        value = value - (float) Math.floor(value);
        return 1.0F - Math.abs(value * 2.0F - 1.0F);
    }
    
    public static float cycle(float value) {
        return value - (float) Math.floor(value);
    }
    
    public static float impulse(float value, float power) {
        value = saturate(value);
        return (float) (Math.pow(value, Math.max(0.01F, power)) * Math.exp(1.0F - value));
    }
    
    public static float elastic(float value) {
        value = saturate(value);
        if (value == 0.0F || value == 1.0F) {
            return value;
        }
        return (float) (Math.pow(2.0D, -10.0D * value) * Math.sin((value * 10.0D - 0.75D) * (TAU / 3.0D)) + 1.0D);
    }
    
    public static float elasticInOut(float value) {
        value = saturate(value);
        if (value == 0.0F || value == 1.0F) {
            return value;
        }
        return value < 0.5F
            ? (float) (-(Math.pow(2.0D, 20.0D * value - 10.0D) * Math.sin((20.0D * value - 11.125D) * (TAU / 4.5D))) / 2.0D)
            : (float) (Math.pow(2.0D, -20.0D * value + 10.0D) * Math.sin((20.0D * value - 11.125D) * (TAU / 4.5D)) / 2.0D + 1.0D);
    }
    
    public static float backOut(float value) {
        value = saturate(value);
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(value - 1.0F, 3.0D) + c1 * (float) Math.pow(value - 1.0F, 2.0D);
    }
    
    public static float backInOut(float value) {
        value = saturate(value);
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        return value < 0.5F
            ? (float) (Math.pow(2.0F * value, 2.0D) * ((c2 + 1.0F) * 2.0F * value - c2) / 2.0D)
            : (float) ((Math.pow(2.0F * value - 2.0F, 2.0D) * ((c2 + 1.0F) * (value * 2.0F - 2.0F) + c2) + 2.0D) / 2.0D);
    }
    
    public static float bounceOut(float value) {
        value = saturate(value);
        float n1 = 7.5625F;
        float d1 = 2.75F;
        if (value < 1.0F / d1) {
            return n1 * value * value;
        }
        if (value < 2.0F / d1) {
            value -= 1.5F / d1;
            return n1 * value * value + 0.75F;
        }
        if (value < 2.5F / d1) {
            value -= 2.25F / d1;
            return n1 * value * value + 0.9375F;
        }
        value -= 2.625F / d1;
        return n1 * value * value + 0.984375F;
    }
    
    public static float bounceInOut(float value) {
        value = saturate(value);
        return value < 0.5F ? (1.0F - bounceOut(1.0F - 2.0F * value)) * 0.5F : (1.0F + bounceOut(2.0F * value - 1.0F)) * 0.5F;
    }
    
    public static float expoInOut(float value) {
        value = saturate(value);
        if (value == 0.0F || value == 1.0F) {
            return value;
        }
        return value < 0.5F ? (float) Math.pow(2.0D, 20.0D * value - 10.0D) * 0.5F : (2.0F - (float) Math.pow(2.0D, -20.0D * value + 10.0D)) * 0.5F;
    }
    
    public static float circOut(float value) {
        value = saturate(value);
        return (float) Math.sqrt(1.0D - Math.pow(value - 1.0F, 2.0D));
    }
    
    public static float dampedSpring(float value, float frequency, float damping) {
        value = saturate(value);
        return (float) (1.0D - Math.exp(-damping * value) * Math.cos(frequency * TAU * value));
    }
    
    public static float softStep(float edge0, float edge1, float value) {
        if (edge0 == edge1) {
            return value >= edge1 ? 1.0F : 0.0F;
        }
        return smooth((value - edge0) / (edge1 - edge0));
    }
    
}
