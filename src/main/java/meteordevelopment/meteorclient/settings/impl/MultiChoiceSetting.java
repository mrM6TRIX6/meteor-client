/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.NameFormat;
import meteordevelopment.meteorclient.utils.misc.Namer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Choice setting over an arbitrary set of values: pick any number of them.
 *
 * <p>{@code T} is deliberately unbounded. Names come from a {@link Namer} on the setting, not from an interface
 * the value type has to implement, so vanilla enums and registry objects can be offered as choices directly
 * instead of being wrapped in a Meteor owned type first.
 *
 * @param <T> the type to pick values of
 */
public class MultiChoiceSetting<T> extends Setting<SequencedSet<T>> {

    private final SequencedSet<T> choices;
    private final Namer<T> namer;

    /**
     * Canonical id to choice. Populated once, so parsing and loading never rescan the choices.
     */
    private final Map<String, T> choicesById;

    public final boolean canBeNone;
    public final double maxWidth;

    public MultiChoiceSetting(String name, String description, SequencedSet<T> defaultValue, Consumer<SequencedSet<T>> onChanged, Consumer<Setting<SequencedSet<T>>> onModuleActivated, IVisible visible, SequencedSet<T> choices, @Nullable Namer<T> namer, boolean canBeNone, double maxWidth) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.choices = Collections.unmodifiableSequencedSet(new LinkedHashSet<>(choices));
        this.namer = namer != null ? namer : Namer.auto();

        this.choicesById = new LinkedHashMap<>(this.choices.size());
        for (T choice : this.choices) {
            String id = NameFormat.validate(this.namer.id(choice));

            T previous = choicesById.putIfAbsent(NameFormat.canonical(id), choice);
            if (previous != null) {
                throw new IllegalStateException("Setting '" + name + "' has two choices named '" + id + "': " + previous + " and " + choice + ".");
            }
        }

        this.canBeNone = canBeNone;
        this.maxWidth = maxWidth;

        value.retainAll(this.choices);

        if (!canBeNone) {
            if (this.choices.isEmpty()) {
                throw new IllegalStateException("Setting '" + name + "' has no choices, but at least one has to be selected (canBeNone = false).");
            }
            if (value.isEmpty()) {
                throw new IllegalStateException("Setting '" + name + "' has no default choices selected, but at least one has to be selected (canBeNone = false).");
            }
        }
    }

    @Override
    protected void resetImpl() {
        value = new LinkedHashSet<>(defaultValue);

        if (choices != null) {
            value.retainAll(choices);
        }
    }

    public SequencedSet<T> getChoices() {
        return choices;
    }

    public Namer<T> getNamer() {
        return namer;
    }

    /**
     * The choice going by the given name, matched leniently, so an id, a raw enum constant name and the gui
     * label all resolve to the same choice.
     */
    @Nullable
    public T getChoice(String name) {
        return name == null ? null : choicesById.get(NameFormat.canonical(name));
    }

    @Override
    protected SequencedSet<T> parseImpl(String str) {
        if (str == null || str.isBlank()) {
            return canBeNone ? new LinkedHashSet<>() : null;
        }

        SequencedSet<T> parsed = new LinkedHashSet<>();

        for (String name : str.split(",")) {
            T choice = getChoice(name);

            // Typed by hand, so a name that means nothing is a mistake worth reporting rather than dropping.
            if (choice == null) {
                return null;
            }

            parsed.add(choice);
        }

        return parsed;
    }

    @Override
    protected boolean isValueValid(SequencedSet<T> value) {
        return choices.containsAll(value) && (canBeNone || !value.isEmpty());
    }

    @Override
    public List<String> getSuggestions() {
        return choices.stream()
            .map(namer::id)
            .toList();
    }

    @Override
    public JsonObject save(JsonObject jsonObject) {
        JsonArray valueArray = new JsonArray();

        for (T choice : value) {
            valueArray.add(namer.id(choice));
        }

        jsonObject.add("value", valueArray);

        return jsonObject;
    }

    @Override
    public SequencedSet<T> load(JsonObject jsonObject) {
        get().clear();

        JsonArray valueArray = jsonObject.get("value").getAsJsonArray();
        for (JsonElement element : valueArray) {
            // Unlike parse, a config is allowed to mention choices that no longer exist.
            T choice = getChoice(element.getAsString());
            if (choice != null) {
                get().add(choice);
            }
        }

        if (!canBeNone && get().isEmpty()) {
            get().addAll(defaultValue);
        }

        return get();
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",");

        for (T choice : value) {
            joiner.add(namer.id(choice));
        }

        return joiner.toString();
    }

    public static class Builder<T> extends SettingBuilder<Builder<T>, SequencedSet<T>, MultiChoiceSetting<T>> {

        private SequencedSet<T> choices = new LinkedHashSet<>();
        private Namer<T> namer;
        private boolean canBeNone = true;
        private double maxWidth = 200;

        public Builder() {
            super(new LinkedHashSet<>());
        }

        @SafeVarargs
        public final Builder<T> choices(T... choices) {
            return choices(Arrays.asList(choices));
        }

        public Builder<T> choices(Collection<T> choices) {
            this.choices = new LinkedHashSet<>(choices);
            return this;
        }

        @SafeVarargs
        public final Builder<T> defaultValue(T... defaultValue) {
            return defaultValue(new LinkedHashSet<>(Arrays.asList(defaultValue)));
        }

        /**
         * Overrides how the choices are named. Required for types that carry no name of their own, see
         * {@link Namer#auto()}.
         */
        public Builder<T> namer(Namer<T> namer) {
            this.namer = namer;
            return this;
        }

        public Builder<T> canBeNone(boolean canBeNone) {
            this.canBeNone = canBeNone;
            return this;
        }

        public Builder<T> maxWidth(double maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        @Override
        public MultiChoiceSetting<T> build() {
            return new MultiChoiceSetting<>(name, description, defaultValue, onChanged, onModuleActivated, visible, choices, namer, canBeNone, maxWidth);
        }

    }

}
