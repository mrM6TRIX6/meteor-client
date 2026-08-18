/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.timer;

import java.time.Instant;

public class TimerUtil {
    
    private long lastMS = System.currentTimeMillis();
    private long startTime;
    
    public TimerUtil() {
        resetCounter();
    }
    
    public long getLastMS() {
        return lastMS;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void reset() {
        lastMS = Instant.now().toEpochMilli();
    }
    
    public void resetCounter() {
        lastMS = System.currentTimeMillis();
    }
    
    public boolean isReached(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }
    
    public void setLastMS(long newValue) {
        lastMS = System.currentTimeMillis() + newValue;
    }
    
    public void setTime(long time) {
        lastMS = time;
    }
    
    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }
    
    public boolean isRunning() {
        return System.currentTimeMillis() - lastMS <= 0;
    }
    
    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }
    
    public boolean finished(final double delay) {
        return System.currentTimeMillis() - delay >= startTime;
    }
    
    public boolean hasTimeElapsed() {
        return lastMS < System.currentTimeMillis();
    }
    
}
