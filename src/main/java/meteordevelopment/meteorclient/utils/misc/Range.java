/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents an inclusive integer range with a lower and upper bound.
 */
public class Range {
    
    public final int from;
    public final int to;
    
    public Range(int from, int to) {
        this.from = Math.min(from, to);
        this.to = Math.max(from, to);
    }
    
    public static Range of(int from, int to) {
        return new Range(from, to);
    }
    
    public static Range of(int value) {
        return new Range(value, value);
    }
    
    /**
     * Checks if the number is in the range.
     */
    public boolean contains(int value) {
        return value >= from && value <= to;
    }
    
    /**
     * Returns a random number from the specified range, inclusive.
     */
    public int random() {
        return ThreadLocalRandom.current().nextInt(from, to + 1);
    }
    
    @Override
    public String toString() {
        return from + ".." + to;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Range range = (Range) obj;
        return from == range.from && to == range.to;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
    
}
