/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.timer;

public class StopWatch {
    
    private long startTime;
    
    public StopWatch() {
        reset();
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public boolean finished(final double delay) {
        return System.currentTimeMillis() - delay >= startTime;
    }
    
    public boolean every(final double delay) {
        boolean finished = this.finished(delay);
        if (finished) {
            reset();
        }
        return finished;
    }
    
    public void reset() {
        this.startTime = System.currentTimeMillis();
    }
    
    public long elapsedTime() {
        return System.currentTimeMillis() - this.startTime;
    }
    
    public StopWatch setMs(long ms) {
        this.startTime = System.currentTimeMillis() - ms;
        return this;
    }
    
}