package meteordevelopment.meteorclient.utils.render.animation.modernfx;

public class EasyBackIn extends Animation {
    
    private float easeAmount = 1.25F;
    
    public EasyBackIn() {}
    
    public EasyBackIn(int ms, double value, float easeAmount) {
        this.ms = ms;
        this.value = value;
        this.easeAmount = easeAmount;
    }
    
    public float getEaseAmount() {
        return this.easeAmount;
    }
    
    public EasyBackIn setEaseAmount(float easeAmount) {
        this.easeAmount = easeAmount;
        return this;
    }
    
    @Override
    public EasyBackIn setMs(int ms) {
        super.setMs(ms);
        return this;
    }
    
    @Override
    public EasyBackIn setValue(double value) {
        super.setValue(value);
        return this;
    }
    
    @Override
    public EasyBackIn setDirection(Direction direction) {
        super.setDirection(direction);
        return this;
    }
    
    @Override
    public double calculation(double value) {
        double x = value / ms;
        float shrink = easeAmount + 1.0F;
        return Math.max(0.0, 1.0 + shrink * Math.pow(x - 1.0, 3.0) + easeAmount * Math.pow(x - 1.0, 2.0));
    }
    
}
