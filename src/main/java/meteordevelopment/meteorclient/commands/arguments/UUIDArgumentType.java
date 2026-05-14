/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UUIDArgumentType implements ArgumentType<UUID> {
    
    public static final SimpleCommandExceptionType INVALID_UUID = new SimpleCommandExceptionType(Text.translatable("argument.uuid.invalid"));
    private static final Collection<String> EXAMPLES = Arrays.asList("dd12be42-52a9-4a91-a8a1-11c01849e498");
    private static final Pattern VALID_CHARACTERS = Pattern.compile("^([-A-Fa-f0-9]+)");
    
    public static UUID getUUID(CommandContext<CommandSource> context, String name) {
        return context.getArgument(name, UUID.class);
    }
    
    public static net.minecraft.command.argument.UuidArgumentType uuid() {
        return new net.minecraft.command.argument.UuidArgumentType();
    }
    
    public UUID parse(StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.getRemaining();
        Matcher matcher = VALID_CHARACTERS.matcher(string);
        if (matcher.find()) {
            String string2 = matcher.group(1);
            
            try {
                UUID uUID = UUID.fromString(string2);
                stringReader.setCursor(stringReader.getCursor() + string2.length());
                return uUID;
            } catch (IllegalArgumentException var6) {
            }
        }
        
        throw INVALID_UUID.createWithContext(stringReader);
    }
    
    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
    
}