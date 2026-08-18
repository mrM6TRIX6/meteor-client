package meteordevelopment.meteorclient.utils.render.animation.modernfx;

import meteordevelopment.meteorclient.utils.timer.TimerUtil;

public class GuiAnimation {
    
    public final TimerUtil counter = new TimerUtil();
    protected int ms = 250;
    protected double value = 1.0;
    protected Direction direction = Direction.FORWARDS;
    
    public GuiAnimation() {}
    
    public int getMs() {
        return this.ms;
    }
    
    public double getValue() {
        return this.value;
    }
    
    public Direction getDirection() {
        return this.direction;
    }
    
    public GuiAnimation setMs(int ms) {
        this.ms = ms;
        return this;
    }
    
    public GuiAnimation setValue(double value) {
        this.value = value;
        return this;
    }
    
    public GuiAnimation setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
        }
        return this;
    }
    
    public GuiAnimation reset() {
        counter.resetCounter();
        return this;
    }
    
    public boolean isDone() {
        return counter.isReached(ms);
    }
    
    public boolean isFinished(Direction direction) {
        return this.direction == direction && isDone();
    }
    
    public Double getOutput() {
        double progress = Math.min(1.0, (double) counter.getTime() / ms);
        double eased = easeOutQuart(progress);
        
        if (direction == Direction.FORWARDS) {
            return eased * value;
        } else {
            return (1.0 - eased) * value;
        }
    }
    
    private double easeOutQuart(double x) {
        return 1.0 - Math.pow(1.0 - x, 4);
    }
    
}