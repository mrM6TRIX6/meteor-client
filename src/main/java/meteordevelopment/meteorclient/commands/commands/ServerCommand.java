/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import joptsimple.internal.Strings;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.network.portscanner.PortScanRunner;
import meteordevelopment.meteorclient.utils.network.portscanner.PortScannerManager;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.*;

public class ServerCommand extends Command {
    
    private final static SimpleCommandExceptionType ADDRESS_ERROR = new SimpleCommandExceptionType(Text.literal("Couldn't obtain server address"));
    private final static SimpleCommandExceptionType INVALID_RANGE = new SimpleCommandExceptionType(Text.literal("Invalid range"));
    
    private static final Set<String> ANTICHEAT_LIST = Set.of("nocheatplus", "negativity", "warden", "horizon", "illegalstack", "coreprotect", "exploitsx", "vulcan", "abc", "spartan", "kauri", "aac", "anticheatreloaded", "witherac", "godseye", "matrix", "wraith", "antixrayheuristics", "grimac", "intave", "watchdog");
    
    private final List<String> plugins = new ArrayList<>();
    private final HashMap<Integer, String> ports = new HashMap<>();
    private final Random random = new Random();
    
    private int ticks = 0;
    private boolean waitingPlugins = false;
    private int completionId;
    
    public ServerCommand() {
        super("server", "Prints server information.");
        
        ports.put(20, "FTP");
        ports.put(22, "SSH");
        ports.put(80, "HTTP");
        ports.put(443, "HTTPS");
        ports.put(25565, "Java Server");
        ports.put(25575, "Java Server RCON");
        ports.put(19132, "Bedrock Server");
        ports.put(19133, "Bedrock Server IPv6");
        ports.put(8123, "DynMap");
        ports.put(25566, "Minequery");
        ports.put(3306, "MySQL");
        ports.put(3389, "RDP");
        
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            basicInfo();
            return SINGLE_SUCCESS;
        });
        
        builder.then(literal("info")
            .executes(ctx -> {
                basicInfo();
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("plugins")
            .executes(ctx -> {
                if (!waitingPlugins) {
                    completionId = random.nextInt(0, 32767);
                    mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(completionId, "/"));
                    waitingPlugins = true;
                }
                
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("tps")
            .executes(ctx -> {
                float tps = TickRate.INSTANCE.getTickRate();
                Formatting color;
                
                if (tps > 17.0f) {
                    color = Formatting.GREEN;
                } else if (tps > 12.0f) {
                    color = Formatting.YELLOW;
                } else {
                    color = Formatting.RED;
                }
                
                info("Current TPS: %s%.2f(default).", color, tps);
                
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("ports")
            .executes(ctx -> {
                scanKnownPorts(getAddress());
                return SINGLE_SUCCESS;
            })
            .then(literal("known")
                .executes(ctx -> {
                    scanKnownPorts(getAddress());
                    return SINGLE_SUCCESS;
                })
            )
            .then(argument("from", IntegerArgumentType.integer(0))
                .then(argument("to", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        scanRange(getAddress(), IntegerArgumentType.getInteger(ctx, "from"), IntegerArgumentType.getInteger(ctx, "to"));
                        return SINGLE_SUCCESS;
                    })
                )
            )
        );
    }
    
    private void basicInfo() {
        if (mc.isIntegratedServerRunning()) {
            IntegratedServer server = mc.getServer();
            
            info("Singleplayer");
            if (server != null) {
                info("Version: %s", server.getVersion());
            }
            
            return;
        }
        
        ServerInfo server = mc.getCurrentServerEntry();
        
        if (server == null) {
            info("Couldn't obtain any server information.");
            return;
        }
        
        String ipv4 = "";
        try {
            ipv4 = InetAddress.getByName(server.address).getHostAddress();
        } catch (UnknownHostException ignored) {
        }
        
        MutableText ipText;
        
        if (ipv4.isEmpty()) {
            ipText = Text.literal(Formatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(server.address))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Copy to clipboard")))
            );
        } else {
            ipText = Text.literal(Formatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(server.address))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Copy to clipboard")))
            );
            MutableText ipv4Text = Text.literal(String.format("%s (%s)", Formatting.GRAY, ipv4));
            ipv4Text.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(ipv4))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Copy to clipboard")))
            );
            ipText.append(ipv4Text);
        }
        
        info(Text.literal(String.format("%sIP: ", Formatting.GRAY)).append(ipText));
        
        info("Port: %d", ServerAddress.parse(server.address).getPort());
        
        info("Type: %s", mc.getNetworkHandler().getBrand() != null ? mc.getNetworkHandler().getBrand() : "unknown");
        
        info("Motd: %s", server.label != null ? server.label.getString() : "unknown");
        
        info("Version: %s", server.version.getString());
        
        info("Protocol version: %d", server.protocolVersion);
        
        info("Difficulty: %s (Local: %.2f)", mc.world.getDifficulty().getTranslatableName().getString(), mc.world.getLocalDifficulty(mc.player.getBlockPos()).getLocalDifficulty());
        
        info("Day: %d", mc.world.getTimeOfDay() / 24000L);
        
        info("Permission level: %s", formatPerms());
    }
    
    public String formatPerms() {
        int p = 5;
        while (!mc.player.hasPermissionLevel(p) && p > 0) {
            p--;
        }
        
        return switch (p) {
            case 0 -> "0 (No Perms)";
            case 1 -> "1 (No Perms)";
            case 2 -> "2 (Player Command Access)";
            case 3 -> "3 (Server Command Access)";
            case 4 -> "4 (Operator)";
            default -> p + " (Unknown)";
        };
    }
    
    
    // Plugins scanning
    
    private void printPlugins() {
        plugins.sort(String.CASE_INSENSITIVE_ORDER);
        plugins.replaceAll(this::formatPluginName);
        
        if (!plugins.isEmpty()) {
            info("Plugins ((highlight)%d(default)): %s.", plugins.size(), Strings.join(plugins.toArray(new String[0]), ", "));
        } else {
            error("No plugins found.");
        }
        
        waitingPlugins = false;
        ticks = 0;
        plugins.clear();
    }
    
    private String formatPluginName(String pluginName) {
        if (ANTICHEAT_LIST.contains(pluginName.toLowerCase())) {
            return String.format("%s%s(default)", Formatting.RED, pluginName);
        } else if (StringUtils.containsIgnoreCase(pluginName, "exploit") || StringUtils.containsIgnoreCase(pluginName, "cheat") || StringUtils.containsIgnoreCase(pluginName, "illegal")) {
            return String.format("%s%s(default)", Formatting.RED, pluginName);
        }
        return String.format("%s%s(default)", Formatting.GREEN, pluginName);
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!waitingPlugins) {
            return;
        }
        if (++ticks >= 100) {
            error("Timeout for get plugins.");
            
            waitingPlugins = false;
            ticks = 0;
            plugins.clear();
        }
    }
    
    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (waitingPlugins && event.packet instanceof RequestCommandCompletionsC2SPacket) {
            event.cancel();
        }
    }
    
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!waitingPlugins) {
            return;
        }
        
        try {
            if (event.packet instanceof CommandSuggestionsS2CPacket packet) {
                Suggestions matches = packet.getSuggestions();
                
                if (matches.isEmpty()) {
                    info("No plugins found.");
                    return;
                }
                
                for (Suggestion suggestion : matches.getList()) {
                    String[] command = suggestion.getText().split(":");
                    if (command.length > 1) {
                        String pluginName = command[0].replace("/", "");
                        if (!plugins.contains(pluginName)) {
                            plugins.add(pluginName);
                        }
                    }
                }
                printPlugins();
            }
        } catch (Exception e) {
            error("An error occurred while trying to get plugins.");
        }
    }
    
    // Ports scanning
    
    private InetAddress getAddress() throws CommandSyntaxException {
        if (mc.isIntegratedServerRunning()) {
            try {
                return InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw ADDRESS_ERROR.create();
            }
        } else {
            ServerInfo server = mc.getCurrentServerEntry();
            if (server == null) {
                throw ADDRESS_ERROR.create();
            }
            try {
                return InetAddress.getByName(server.address);
            } catch (UnknownHostException e) {
                throw ADDRESS_ERROR.create();
            }
        }
    }
    
    private void scanPorts(InetAddress address, Collection<Integer> port_list) {
        info("Started scanning %d ports", port_list.size());
        PortScanRunner portScanRunner = new PortScanRunner(address, 5, 3, 200, port_list, scanResults -> {
            int open_ports = 0;
            info("Open ports:");
            for (PortScannerManager.ScanResult result : scanResults) {
                if (result.isOpen()) {
                    info(formatPort(result.getPort(), address));
                    open_ports++;
                }
            }
            info("Open count: %d/%d", open_ports, scanResults.size());
        });
        PortScannerManager.scans.add(portScanRunner);
    }
    
    private void scanKnownPorts(InetAddress address) {
        scanPorts(address, ports.keySet());
    }
    
    private void scanRange(InetAddress address, int min, int max) throws CommandSyntaxException {
        if (max < min) {
            throw INVALID_RANGE.create();
        }
        List<Integer> port_list = new LinkedList<>();
        for (int i = min; i <= max; i++) {
            port_list.add(i);
        }
        scanPorts(address, port_list);
    }
    
    private MutableText formatPort(int port, InetAddress address) {
        MutableText text = Text.literal(String.format("- %s%d%s ", Formatting.GREEN, port, Formatting.GRAY));
        if (ports.containsKey(port)) {
            text.append(ports.get(port));
            if (ports.get(port).startsWith("HTTP") || ports.get(port).startsWith("FTP")) {
                text.setStyle(text.getStyle()
                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(String.format("%s://%s:%d", ports.get(port).toLowerCase(), address.getHostAddress(), port))))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Open in browser")))
                );
            } else if (Objects.equals(ports.get(port), "DynMap")) {
                text.setStyle(text.getStyle()
                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(String.format("http://%s:%d", address.getHostAddress(), port))))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Open in browser")))
                );
            } else {
                text.setStyle(text.getStyle()
                    .withClickEvent(new ClickEvent.CopyToClipboard(String.format("%s:%d", address.getHostAddress(), port)))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Copy to clipboard")))
                );
            }
        } else {
            text.setStyle(text.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(String.format("%s:%d", address.getHostAddress(), port)))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Copy to clipboard")))
            );
        }
        return text;
    }
    
}
