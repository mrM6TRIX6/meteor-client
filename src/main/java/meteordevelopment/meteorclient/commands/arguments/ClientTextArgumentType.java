/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ParserBackedArgumentType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtParsing;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.packrat.Parser;

import java.util.Arrays;
import java.util.Collection;

public class ClientTextArgumentType extends ParserBackedArgumentType<Text> {
    
    public static final DynamicCommandExceptionType INVALID_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(
        text -> Text.stringifiedTranslatable("argument.component.invalid", text)
    );
    private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "'hello world'", "\"\"", "{text:\"hello world\"}", "[\"\"]");
    private static final DynamicOps<NbtElement> OPS = NbtOps.INSTANCE;
    private static final Parser<NbtElement> PARSER = SnbtParsing.createParser(OPS);
    
    private ClientTextArgumentType(RegistryWrapper.WrapperLookup registries) {
        super(PARSER.withDecoding(registries.getOps(OPS), PARSER, TextCodecs.CODEC, INVALID_COMPONENT_EXCEPTION));
    }
    
    public static Text get(CommandContext<?> context, String name) {
        return context.getArgument(name, Text.class);
    }
    
    public static ClientTextArgumentType text(CommandRegistryAccess registryAccess) {
        return new ClientTextArgumentType(registryAccess);
    }
    
    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
    
}
