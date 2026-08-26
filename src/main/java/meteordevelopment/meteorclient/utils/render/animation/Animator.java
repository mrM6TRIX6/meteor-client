/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render.animation;

import net.minecraft.util.math.MathHelper;

/**
 * Animates a single value over wall clock time. Reading catches the value up on its own, so a frame that never gets
 * drawn cannot leave an animation stranded halfway.
 */
public class Animator {
    
    private Animation animation = Animation.LINEAR;
    private long start;
    private double duration;
    private double fromValue;
    private double toValue;
    private double value;
    private boolean running;
    
    public Animator() {
        this(0);
    }
    
    public Animator(double value) {
        set(value);
    }
    
    public Animator run(Animation animation, double to, double duration) {
        // Time passed even if nothing read the value, so start from where the animation actually is.
        update();
        
        // Already there, or already on the way there - restarting would just reset the progress.
        if (to == toValue && (running || value == to)) {
            return this;
        }
        
        if (duration <= 0) {
            return set(to);
        }
        
        this.animation = animation;
        this.duration = duration;
        this.fromValue = value;
        this.toValue = to;
        this.start = System.currentTimeMillis();
        this.running = true;
        
        return this;
    }
    
    public Animator set(double value) {
        this.fromValue = value;
        this.toValue = value;
        this.value = value;
        this.start = 0L;
        this.running = false;
        
        return this;
    }
    
    public boolean update() {
        if (!running) {
            return false;
        }
        
        double progress = getProgress();
        
        if (progress >= 1) {
            value = toValue;
            running = false;
            
            return false;
        }
        
        value = MathHelper.lerp(animation.apply(progress), fromValue, toValue);
        
        return true;
    }
    
    public double getProgress() {
        if (!running || duration <= 0) {
            return 1;
        }
        
        return MathHelper.clamp((System.currentTimeMillis() - start) / duration, 0, 1);
    }
    
    public boolean isRunning() {
        update();
        
        return running;
    }
    
    public double getValue() {
        update();
        
        return value;
    }
    
    public float getValueF() {
        return (float) getValue();
    }
    
    public double getFromValue() {
        return fromValue;
    }
    
    public double getToValue() {
        return toValue;
    }
    
    public double getDuration() {
        return duration;
    }
    
    public long getStart() {
        return start;
    }
    
    public Animation getAnimation() {
        return animation;
    }
    
}
