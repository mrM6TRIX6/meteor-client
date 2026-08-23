/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.name.NameFormat;
import meteordevelopment.meteorclient.utils.name.Namer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EnumChoiceSetting<T extends Enum<T>> extends Setting<T> {

    private final List<T> choices;
    private final Namer<T> namer;
    
    private final Map<String, T> choicesById;

    public EnumChoiceSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        this(name, description, defaultValue, onChanged, onModuleActivated, visible, null);
    }

    public EnumChoiceSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible, @Nullable Namer<T> namer) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.choices = List.of(defaultValue.getDeclaringClass().getEnumConstants());
        this.namer = namer != null ? namer : Namer.auto();

        this.choicesById = new LinkedHashMap<>(this.choices.size());
        for (T choice : this.choices) {
            String id = NameFormat.validate(this.namer.id(choice));

            T previous = choicesById.putIfAbsent(NameFormat.canonical(id), choice);
            if (previous != null) {
                throw new IllegalStateException("Setting '" + name + "' has two choices named '" + id + "': " + previous + " and " + choice + ".");
            }
        }
    }

    public List<T> getChoices() {
        return choices;
    }

    public Namer<T> getNamer() {
        return namer;
    }

    @Nullable
    public T getChoice(String name) {
        return name == null ? null : choicesById.get(NameFormat.canonical(name));
    }

    @Override
    protected T parseImpl(String str) {
        return getChoice(str);
    }

    @Override
    protected boolean isValueValid(T value) {
        /*
         * Since value is guaranteed to be an enum (T) constant used for this setting,
         * there's no need to double-check whether it is contained in the list.
         */
        return true;
    }

    @Override
    public List<String> getSuggestions() {
        return choices.stream()
            .map(namer::id)
            .toList();
    }

    @Override
    public JsonObject save(JsonObject jsonObject) {
        jsonObject.addProperty("value", namer.id(get()));

        return jsonObject;
    }

    @Override
    public T load(JsonObject jsonObject) {
        parse(jsonObject.get("value").getAsString());

        return get();
    }

    public static class Builder<T extends Enum<T>> extends SettingBuilder<Builder<T>, T, EnumChoiceSetting<T>> {

        protected Namer<T> namer;

        public Builder() {
            super(null);
        }
        
        public Builder<T> namer(Namer<T> namer) {
            this.namer = namer;
            return this;
        }

        @Override
        public EnumChoiceSetting<T> build() {
            return new EnumChoiceSetting<>(name, description, defaultValue, onChanged, onModuleActivated, visible, namer);
        }

    }

}
