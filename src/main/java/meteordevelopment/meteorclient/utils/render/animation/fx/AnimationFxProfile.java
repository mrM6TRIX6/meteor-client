package meteordevelopment.meteorclient.utils.render.animation.fx;

public enum AnimationFxProfile {
    
    ELASTIC_POP(0.72F, 0.58F, 1.38F, 9.0F) {
        @Override
        protected float compute(float cycle) {
            return AnimationFxMath.elastic(cycle);
        }
    },
    BOUNCE_DROP(0.82F, 0.66F, 1.24F, 8.0F) {
        @Override
        protected float compute(float cycle) {
            return AnimationFxMath.bounceOut(cycle);
        }
    },
    BACK_PUNCH(0.76F, 0.62F, 1.30F, 7.0F) {
        @Override
        protected float compute(float cycle) {
            return AnimationFxMath.backOut(cycle);
        }
    },
    JELLY(0.62F, 0.72F, 1.34F, 6.0F) {
        @Override
        protected float compute(float cycle) {
            float base = AnimationFxMath.elasticInOut(cycle);
            float wobble = (float) Math.sin(cycle * AnimationFxMath.TAU * 3.0F) * (1.0F - cycle) * 0.18F;
            return base + wobble;
        }
    },
    RUBBER(0.68F, 0.64F, 1.42F, 10.0F) {
        @Override
        protected float compute(float cycle) {
            float pulse = AnimationFxMath.triangle(cycle);
            return AnimationFxMath.backInOut(pulse);
        }
    },
    SPRING(0.70F, 0.58F, 1.36F, 11.0F) {
        @Override
        protected float compute(float cycle) {
            return AnimationFxMath.dampedSpring(cycle, 2.45F, 5.4F);
        }
    },
    HEARTBEAT(0.92F, 0.72F, 1.32F, 5.0F) {
        @Override
        protected float compute(float cycle) {
            float first = AnimationFxMath.impulse(AnimationFxMath.triangle(cycle * 1.0F), 0.34F) * 0.62F;
            float second = AnimationFxMath.impulse(AnimationFxMath.triangle(cycle * 1.0F + 0.22F), 0.52F) * 0.38F;
            return AnimationFxMath.saturate(first + second);
        }
    },
    SNAP(1.05F, 0.50F, 1.28F, 7.5F) {
        @Override
        protected float compute(float cycle) {
            return AnimationFxMath.expoInOut(AnimationFxMath.triangle(cycle));
        }
    },
    FLOAT_BOUNCE(0.55F, 0.78F, 1.18F, 12.0F) {
        @Override
        protected float compute(float cycle) {
            return AnimationFxMath.smoother(AnimationFxMath.sin01(cycle * AnimationFxMath.TAU));
        }
    },
    POP_CORNER(0.88F, 0.56F, 1.40F, 8.5F) {
        @Override
        protected float compute(float cycle) {
            float kick = AnimationFxMath.circOut(AnimationFxMath.triangle(cycle));
            float recoil = AnimationFxMath.bounceInOut(cycle) * 0.28F;
            return kick + recoil;
        }
    };
    
    private final float speed;
    private final float minScale;
    private final float maxScale;
    private final float offset;
    
    AnimationFxProfile(float speed, float minScale, float maxScale, float offset) {
        this.speed = speed;
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.offset = offset;
    }
    
    public float value(long millis, int index, int total) {
        return AnimationFxMath.saturate(raw(millis, index, total));
    }
    
    public float scale(long millis, int index, int total) {
        float raw = raw(millis, index, total);
        return minScale + (maxScale - minScale) * raw;
    }
    
    public float offset(long millis, int index, int total) {
        return wave(millis, index, total) * offset;
    }
    
    public float wave(long millis, int index, int total) {
        int safeTotal = Math.max(1, total);
        float time = millis / 1000.0F * speed;
        float phase = index / (float) safeTotal;
        return AnimationFxMath.triangle(time + phase) * 2.0F - 1.0F;
    }
    
    private float raw(long millis, int index, int total) {
        int safeTotal = Math.max(1, total);
        float time = millis / 1000.0F * speed;
        float phase = index / (float) safeTotal;
        return compute(AnimationFxMath.cycle(time + phase));
    }
    
    protected abstract float compute(float cycle);
}
