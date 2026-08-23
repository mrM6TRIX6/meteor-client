/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import java.util.function.Function;

public interface Namer<T> {
    
    String id(T value);

    default String display(T value) {
        return NameFormat.display(id(value));
    }
    
    @SuppressWarnings("unchecked")
    static <T> Namer<T> auto() {
        return (Namer<T>) Auto.INSTANCE;
    }

    static <T> Namer<T> of(Function<T, String> id) {
        return id::apply;
    }
    
    static <T> Namer<T> of(Function<T, String> id, Function<T, String> display) {
        return new Namer<>() {
            @Override
            public String id(T value) {
                return id.apply(value);
            }

            @Override
            public String display(T value) {
                return display.apply(value);
            }
        };
    }
    
    final class Auto implements Namer<Object> {

        private static final Auto INSTANCE = new Auto();

        private Auto() {}

        @Override
        public String id(Object value) {
            if (value instanceof IName name) {
                return name.getName();
            }
            if (value instanceof Enum<?> constant) {
                return NameFormat.toId(constant.name());
            }

            throw unnameable(value);
        }

        @Override
        public String display(Object value) {
            if (value instanceof IDisplayName name) {
                return name.getDisplayName();
            }
            if (value instanceof Enum<?> constant) {
                return NameFormat.display(constant.name());
            }

            throw unnameable(value);
        }

        private static IllegalStateException unnameable(Object value) {
            return new IllegalStateException(
                "Cannot derive a name for '" + value + "' of type " + value.getClass().getName() + ". " +
                    "Make it implement IName, or give the setting an explicit Namer."
            );
        }

    }

}
