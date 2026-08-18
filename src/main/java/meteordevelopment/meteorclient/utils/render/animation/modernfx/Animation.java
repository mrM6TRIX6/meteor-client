package meteordevelopment.meteorclient.utils.render.animation.modernfx;


import meteordevelopment.meteorclient.utils.timer.TimerUtil;

public class Animation implements AnimationCalculation {
    
    public final TimerUtil counter = new TimerUtil();
    protected int ms;
    protected double value;
    protected Direction direction = Direction.FORWARDS;
    
    public Animation() {}
    
    public Animation setMs(int ms) {
        this.ms = ms;
        return this;
    }
    
    public Animation setValue(double value) {
        this.value = value;
        return this;
    }
    
    public Animation setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            adjustTimer();
        }
        return this;
    }
    
    public int getMs() {
        return this.ms;
    }
    
    public double getValue() {
        return this.value;
    }
    
    public Direction getDirection() {
        return this.direction;
    }
    
    public void reset() {
        counter.resetCounter();
    }
    
    public void update() {
    }
    
    public boolean isDone() {
        return counter.isReached(ms);
    }
    
    public boolean isFinished(Direction direction) {
        return this.direction == direction && isDone();
    }
    
    public boolean isDirection(Direction direction) {
        return this.direction == direction;
    }
    
    private void adjustTimer() {
        counter.setTime(
            System.currentTimeMillis() - ((long) ms - Math.min(ms, counter.getTime()))
        );
    }
    
    public Double getOutput() {
        double time = (1 - calculation(counter.getTime())) * value;
        
        return direction == Direction.FORWARDS
            ? endValue()
            : isDone() ? 0.0 : time;
    }
    
    protected double endValue() {
        return isDone()
            ? value
            : calculation(counter.getTime()) * value;
    }
    
}