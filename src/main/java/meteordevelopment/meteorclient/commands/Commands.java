/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.commands.*;
import meteordevelopment.meteorclient.events.game.GameJoinEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.command.CommandSource;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Commands {
    
    private static final List<Command> commands = new ArrayList<>();
    private static final Map<Class<? extends Command>, Command> commandInstances = new Reference2ReferenceOpenHashMap<>();
    private static CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
    
    @PostInit(dependencies = PathManagers.class)
    public static void init() {
        add(new BindsCommand());
        add(new BindCommand());
        add(new CenterCommand());
        add(new CheckCMDCommand());
        add(new ClearInventoryCommand());
        add(new CommandsCommand());
        add(new DamageCommand());
        add(new DismountCommand());
        add(new DisconnectCommand());
        add(new DropCommand());
        add(new EnchantCommand());
        add(new EnderChestCommand());
        add(new EquipCommand());
        add(new FakeMessageCommand());
        add(new FakePlayerCommand());
        add(new FOVCommand());
        add(new FriendsCommand());
        add(new GamemodeCommand());
        add(new GiveCommand());
        add(new GodbridgeAngleCommand());
        add(new HClipCommand());
        add(new InputCommand());
        add(new InventoryCommand());
        add(new LocateCommand());
        add(new MacroCommand());
        add(new MemoryCommand());
        add(new ModulesCommand());
        add(new NameHistoryCommand());
        add(new NbtCommand());
        add(new NotebotCommand());
        add(new PeekCommand());
        add(new PingCommand());
        add(new ProfilesCommand());
        add(new ReconnectCommand());
        add(new ReloadCommand());
        add(new ResetCommand());
        add(new RotationCommand());
        add(new SaveMapCommand());
        add(new SayCommand());
        add(new ServerCommand());
        add(new SetBlockCommand());
        add(new SettingCommand());
        add(new SpectateCommand());
        add(new SPTPCommand());
        add(new StealCommand());
        add(new SwarmCommand());
        add(new TeleportCommand());
        add(new ToggleCommand());
        add(new UUIDCommand());
        add(new VClipCommand());
        add(new WaspCommand());
        
        commands.sort(Comparator.comparing(Command::getName));
        
        MeteorClient.EVENT_BUS.subscribe(Commands.class);
    }
    
    public static void add(Command command) {
        commands.forEach(existing -> {
            if (existing.getName().equalsIgnoreCase(command.getName())) {
                throw new IllegalArgumentException("Command with name '%s' already exists".formatted(command.getName()));
            }
        });
        
        commandInstances.put(command.getClass(), command);
        commands.add(command);
    }
    
    public static CommandDispatcher<CommandSource> getDispatcher() {
        return dispatcher;
    }
    
    public static void dispatch(String message) throws CommandSyntaxException {
        dispatcher.execute(message, mc.getNetworkHandler().getCommandSource());
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Command> T get(Class<T> klass) {
        return (T) commandInstances.get(klass);
    }
    
    public static Command get(String name) {
        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(name)) {
                return command;
            }
        }
        return null;
    }
    
    public static List<Command> getAll() {
        return commands;
    }
    
    public static int getCount() {
        return commands.size();
    }
    
    @EventHandler
    private static void onGameJoin(GameJoinEvent event) {
        dispatcher = new CommandDispatcher<>();
        for (Command command : commands) {
            command.registerTo(dispatcher);
        }
    }
    
}
