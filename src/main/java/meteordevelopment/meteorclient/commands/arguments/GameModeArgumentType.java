/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GameModeArgumentType implements ArgumentType<GameMode> {
    
    private static final GameModeArgumentType INSTANCE = new GameModeArgumentType();
    private static final DynamicCommandExceptionType INVALID_GAMEMODE = new DynamicCommandExceptionType(gameMode -> Text.literal("Invalid game mode '" + gameMode + "'."));
    private static final Collection<String> EXAMPLES = List.of("creative", "1");
    
    private GameModeArgumentType() {}
    
    public static GameModeArgumentType create() {
        return INSTANCE;
    }
    
    public static GameMode get(CommandContext<?> context, String name) {
        return context.getArgument(name, GameMode.class);
    }
    
    @Override
    public GameMode parse(StringReader reader) throws CommandSyntaxException {
        String argument = reader.readString();
        
        for (GameMode gameMode : GameMode.values()) {
            if (gameMode.getId().equalsIgnoreCase(argument) || String.valueOf(gameMode.getIndex()).equals(argument)) {
                return gameMode;
            }
        }
        
        throw INVALID_GAMEMODE.create(argument);
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Arrays.stream(GameMode.values())
            .toList()
            .stream()
            .map(gameMode -> gameMode.getId()), builder
        );
    }
    
    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
    
}
