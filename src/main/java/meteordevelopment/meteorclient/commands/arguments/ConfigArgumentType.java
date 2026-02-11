/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.config.Config;
import meteordevelopment.meteorclient.config.ConfigManager;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.command.CommandSource.suggestMatching;

public class ConfigArgumentType implements ArgumentType<String> {
    
    private static final ConfigArgumentType INSTANCE = new ConfigArgumentType();
    
    private static final DynamicCommandExceptionType NO_SUCH_PROFILE = new DynamicCommandExceptionType(name -> Text.literal("Config with name " + name + " doesn't exist."));
    private static final Collection<String> EXAMPLES = List.of("pvp.meteorclient.com", "anarchy");
    
    private ConfigArgumentType() {}
    
    public static ConfigArgumentType create() {
        return INSTANCE;
    }
    
    public static Config get(CommandContext<?> context, String name) {
        return ConfigManager.get(context.getArgument(name, String.class));
    }
    
    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String argument = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        
        if (ConfigManager.get(argument) == null) {
            throw NO_SUCH_PROFILE.create(argument);
        }
        
        return argument;
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return suggestMatching(Streams.stream(ConfigManager.getAll()).map(profile -> profile.name.get()), builder);
    }
    
    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
    
}
