/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.clientsettings.ClientSettings;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

import java.util.List;
import java.util.stream.Stream;

public abstract class Command {
    
    protected static CommandRegistryAccess REGISTRY_ACCESS = CommandManager.createRegistryAccess(BuiltinRegistries.createWrapperLookup());
    protected static final int SINGLE_SUCCESS = com.mojang.brigadier.Command.SINGLE_SUCCESS;
    protected static final MinecraftClient mc = MeteorClient.mc;
    
    private final String name;
    private final String separatedName;
    private final String description;
    private final List<String> aliases;
    
    public Command(String name, String description, String... aliases) {
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Command '%s' contains invalid characters in name.".formatted(name));
        }
        
        this.name = name;
        this.separatedName = Utils.separateName(this.name);
        this.description = description;
        this.aliases = Stream.of(aliases)
            .map(String::toLowerCase)
            .distinct()
            .toList();
    }
    
    // Helper methods to painlessly infer the CommandSource generic type argument
    protected static <T> RequiredArgumentBuilder<CommandSource, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
    
    protected static LiteralArgumentBuilder<CommandSource> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }
    
    public final void registerTo(CommandDispatcher<CommandSource> dispatcher) {
        register(dispatcher, name.toLowerCase());
        for (String alias : aliases) {
            register(dispatcher, alias);
        }
    }
    
    public void register(CommandDispatcher<CommandSource> dispatcher, String name) {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.literal(name);
        build(builder);
        dispatcher.register(builder);
    }
    
    public abstract void build(LiteralArgumentBuilder<CommandSource> builder);
    
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return ClientSettings.get().separateNames.get() ? separatedName : name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<String> getAliases() {
        return aliases;
    }
    
    public String toString() {
        return ClientSettings.get().prefix.get() + name.toLowerCase();
    }
    
    public String toString(String... args) {
        StringBuilder base = new StringBuilder(toString());
        for (String arg : args) {
            base.append(' ').append(arg);
        }
        return base.toString();
    }
    
    public void info(Text message) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.sendMsg(getDisplayName(), message);
    }
    
    public void info(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.infoPrefix(getDisplayName(), message, args);
    }
    
    public void warning(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.warningPrefix(getDisplayName(), message, args);
    }
    
    public void error(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.errorPrefix(getDisplayName(), message, args);
    }
    
}