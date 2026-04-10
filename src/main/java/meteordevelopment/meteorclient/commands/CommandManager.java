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
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CommandManager {
    
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
        add(new ConfigsCommand());
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
        add(new GameModeCommand());
        add(new GiveCommand());
        add(new GodbridgeAngleCommand());
        add(new HClipCommand());
        add(new HelpCommand());
        add(new InputCommand());
        add(new InventoryCommand());
        add(new LocateCommand());
        add(new MacroCommand());
        add(new MemoryCommand());
        add(new ModulesCommand());
        add(new NameHistoryCommand());
        add(new NameCollectCommand());
        add(new NBTCommand());
        add(new NotebotCommand());
        add(new PeekCommand());
        add(new PermissionsCommand());
        add(new PingCommand());
        add(new ReconnectCommand());
        add(new ReloadCommand());
        add(new ResetCommand());
        add(new RotationCommand());
        add(new SaveMapCommand());
        add(new SayCommand());
        add(new SaveSkinCommand());
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
        
        commands.sort(Comparator.comparing(command -> command.name));
        
        MeteorClient.EVENT_BUS.subscribe(CommandManager.class);
    }
    
    public static void add(Command command) {
        commands.forEach(existing -> {
            if (existing.name.equalsIgnoreCase(command.name)) {
                throw new IllegalArgumentException("Command with name '%s' already exists".formatted(command.name));
            }
        });
        
        commands.add(command);
        commandInstances.put(command.getClass(), command);
    }
    
    public static CommandDispatcher<CommandSource> getDispatcher() {
        return dispatcher;
    }
    
    public static void dispatch(String message) throws CommandSyntaxException {
        dispatcher.execute(message, mc.getNetworkHandler().getCommandSource());
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Command> T get(Class<T> clazz) {
        return (T) commandInstances.get(clazz);
    }
    
    public static Command get(String name) {
        for (Command command : commands) {
            if (command.name.equalsIgnoreCase(name)) {
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
    
    /**
     * Argument types that rely on Minecraft registries access those registries through a {@link CommandRegistryAccess}
     * object. Since dynamic registries are specific to each server, we need to make a new CommandRegistryAccess object
     * every time we join a server.
     * <p>
     * The command tree and by extension the {@link CommandDispatcher} also have to be rebuilt because:
     * <ol>
     * <li>Argument types that require registries use a registry wrapper object that is created and stored in the
     *     argument type objects when the command tree is built.
     * <li>Registry entries and keys are compared using referential equality. Even if the data encoded is the same,
     *     registry wrapper objects' dynamic data becomes stale after joining another server.
     * <li>The CommandDispatcher's node merging only adds missing children, it cannot replace stale argument type
     *     objects.
     * </ol>
     *
     * @author Crosby
     */
    @EventHandler
    private static void onGameJoin(GameJoinEvent event) {
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        Command.REGISTRY_ACCESS = CommandRegistryAccess.of(networkHandler.getRegistryManager(), networkHandler.getEnabledFeatures());
        
        dispatcher = new CommandDispatcher<>();
        for (Command command : commands) {
            command.registerTo(dispatcher);
        }
    }
    
}
