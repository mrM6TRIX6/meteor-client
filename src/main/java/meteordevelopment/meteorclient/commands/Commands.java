/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.commands.*;
import meteordevelopment.meteorclient.events.game.GameJoinEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Commands {
    
    public static final List<Command> COMMANDS = new ArrayList<>();
    public static CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();
    
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
        add(new FakePlayerCommand());
        add(new FovCommand());
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
        add(new ReloadCommand());
        add(new ResetCommand());
        add(new RotationCommand());
        add(new SaveMapCommand());
        add(new SayCommand());
        add(new ServerCommand());
        add(new SetBlockCommand());
        add(new SettingCommand());
        add(new SpectateCommand());
        add(new SptpCommand());
        add(new StealCommand());
        add(new SwarmCommand());
        add(new TeleportCommand());
        add(new ToggleCommand());
        add(new UUIDCommand());
        add(new VClipCommand());
        add(new WaspCommand());
        
        COMMANDS.sort(Comparator.comparing(Command::getName));
        
        MeteorClient.EVENT_BUS.subscribe(Commands.class);
    }
    
    public static void add(Command command) {
        COMMANDS.removeIf(existing -> existing.getName().equals(command.getName()));
        COMMANDS.add(command);
    }
    
    public static void dispatch(String message) throws CommandSyntaxException {
        DISPATCHER.execute(message, mc.getNetworkHandler().getCommandSource());
    }
    
    public static Command get(String name) {
        for (Command command : COMMANDS) {
            if (command.getName().equals(name)) {
                return command;
            }
        }
        return null;
    }
    
    @EventHandler
    private static void onGameJoin(GameJoinEvent event) {
        DISPATCHER = new CommandDispatcher<>();
        for (Command command : COMMANDS) {
            command.registerTo(DISPATCHER);
        }
    }
    
}
