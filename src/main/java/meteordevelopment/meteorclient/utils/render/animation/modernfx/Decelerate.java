package meteordevelopment.meteorclient.utils.render.animation.modernfx;

public class Decelerate extends Animation {
    
    @Override
    public double calculation(double value) {
        double x = value / ms;
        return 1 - (x - 1) * (x - 1);
    }
    
}
