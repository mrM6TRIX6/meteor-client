/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render.animation;

import java.util.function.DoubleUnaryOperator;

import static java.lang.Math.*;

public enum Animation {

    LINEAR(x -> x),
    SIGMOID(x -> (1 / (1 + exp(-C.SIGMOID_K * (x - 0.5))) - C.SIGMOID_LOW) / (C.SIGMOID_HIGH - C.SIGMOID_LOW)),
    EASE_IN_QUAD(x -> x * x),
    EASE_OUT_QUAD(x -> x * (2 - x)),
    EASE_IN_OUT_QUAD(x -> x < 0.5 ? 2 * x * x : -1 + (4 - 2 * x) * x),
    EASE_IN_CUBIC(x -> x * x * x),
    EASE_OUT_CUBIC(x -> (--x) * x * x + 1),
    EASE_IN_OUT_CUBIC(x -> x < 0.5 ? 4 * x * x * x : 4 * (x - 1) * (x - 1) * (x - 1) + 1),
    EASE_IN_QUART(x -> x * x * x * x),
    EASE_OUT_QUART(x -> 1 - (--x) * x * x * x),
    EASE_IN_OUT_QUART(x -> x < 0.5 ? 8 * x * x * x * x : 1 - 8 * (--x) * x * x * x),
    EASE_IN_QUINT(x -> x * x * x * x * x),
    EASE_OUT_QUINT(x -> 1 + (--x) * x * x * x * x),
    EASE_IN_OUT_QUINT(x -> x < 0.5 ? 16 * x * x * x * x * x : 1 + 16 * (--x) * x * x * x * x),
    EASE_IN_SINE(x -> 1 - cos(x * PI / 2)),
    EASE_OUT_SINE(x -> sin(x * PI / 2)),
    EASE_IN_OUT_SINE(x -> -(cos(PI * x) - 1) / 2),
    EASE_IN_EXPO(x -> x == 0 ? 0 : pow(2, 10 * x - 10)),
    EASE_OUT_EXPO(x -> x == 1 ? 1 : 1 - pow(2, -10 * x)),
    EASE_IN_OUT_EXPO(x -> x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? pow(2, 20 * x - 10) / 2 : (2 - pow(2, -20 * x + 10)) / 2),
    EASE_IN_CIRC(x -> 1 - sqrt(1 - x * x)),
    EASE_OUT_CIRC(x -> sqrt(1 - (--x) * x)),
    EASE_IN_OUT_CIRC(x -> x < 0.5 ? (1 - sqrt(1 - 4 * x * x)) / 2 : (sqrt(1 - 4 * (x - 1) * (x - 1)) + 1) / 2),
    EASE_IN_BACK(x -> C.BACK_C3 * x * x * x - C.BACK_C1 * x * x),
    EASE_OUT_BACK(x -> 1 + C.BACK_C3 * pow(x - 1, 3) + C.BACK_C1 * pow(x - 1, 2)),
    EASE_IN_OUT_BACK(x -> x < 0.5
        ? (pow(2 * x, 2) * ((C.BACK_C2 + 1) * 2 * x - C.BACK_C2)) / 2
        : (pow(2 * x - 2, 2) * ((C.BACK_C2 + 1) * (x * 2 - 2) + C.BACK_C2) + 2) / 2),
    EASE_IN_ELASTIC(x -> x == 0 ? 0 : x == 1 ? 1 : -pow(2, 10 * x - 10) * sin((x * 10 - 10.75) * C.ELASTIC_C4)),
    EASE_OUT_ELASTIC(x -> x == 0 ? 0 : x == 1 ? 1 : pow(2, -10 * x) * sin((x * 10 - 0.75) * C.ELASTIC_C4) + 1),
    EASE_IN_OUT_ELASTIC(x -> x == 0 ? 0 : x == 1 ? 1 : x < 0.5
        ? -(pow(2, 20 * x - 10) * sin((20 * x - 11.125) * C.ELASTIC_C5)) / 2
        : (pow(2, -20 * x + 10) * sin((20 * x - 11.125) * C.ELASTIC_C5)) / 2 + 1),
    SHRINK_EASING(x -> max(0, 1 + 2.3 * pow(x - 1, 3) + 1.3 * pow(x - 1, 2)));
    
    private final DoubleUnaryOperator function;

    Animation(DoubleUnaryOperator function) {
        this.function = function;
    }

    public double apply(double x) {
        return function.applyAsDouble(x);
    }

    public float apply(float x) {
        return (float) function.applyAsDouble(x);
    }
    
    private static final class C {

        private static final double SIGMOID_K = 12;
        private static final double SIGMOID_LOW = 1 / (1 + exp(SIGMOID_K * 0.5));
        private static final double SIGMOID_HIGH = 1 / (1 + exp(-SIGMOID_K * 0.5));

        private static final double BACK_C1 = 1.70158;
        private static final double BACK_C2 = BACK_C1 * 1.525;
        private static final double BACK_C3 = BACK_C1 + 1;

        private static final double ELASTIC_C4 = (2 * PI) / 3;
        private static final double ELASTIC_C5 = (2 * PI) / 4.5;

        private C() {}

    }

}
