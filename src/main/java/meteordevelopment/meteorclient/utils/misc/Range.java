/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class Range {
    
    public final int min;
    public final int max;
    
    public Range(int min, int max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }
    
    public static Range of(int min, int max) {
        return new Range(min, max);
    }
    
    public static Range of(int singleValue) {
        return new Range(singleValue, singleValue);
    }
    
    public static Range parse(String rangeStr) {
        try {
            String[] parts = rangeStr.split("-");
            if (parts.length == 2) {
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return of(min, max);
            } else if (parts.length == 1) {
                int value = Integer.parseInt(parts[0].trim());
                return of(value);
            }
            throw new IllegalArgumentException("Invalid range format: " + rangeStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in range: " + rangeStr, e);
        }
    }
    
    public boolean contains(int value) {
        return value >= min && value <= max;
    }
    
    public int getRandomValue() {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    public IntStream stream() {
        return IntStream.rangeClosed(min, max);
    }
    
    @Override
    public String toString() {
        return min + "-" + max;
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
        return min == range.min && max == range.max;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }
    
}
