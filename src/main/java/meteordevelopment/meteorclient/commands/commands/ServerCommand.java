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
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.text.TextUtils;
import meteordevelopment.meteorclient.utils.network.PortScanner;
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
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.Strings;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.*;

public class ServerCommand extends Command {
    
    private static final SimpleCommandExceptionType ADDRESS_ERROR = new SimpleCommandExceptionType(Text.literal("Couldn't obtain server address"));
    private static final SimpleCommandExceptionType INVALID_RANGE = new SimpleCommandExceptionType(Text.literal("Invalid range"));
    
    private static final Set<String> ANTICHEAT_LIST = Set.of(
        "nocheatplus",
        "negativity",
        "warden",
        "horizon",
        "illegalstack",
        "coreprotect",
        "exploitsx",
        "vulcan",
        "abc",
        "spartan",
        "kauri",
        "aac",
        "anticheatreloaded",
        "witherac",
        "godseye",
        "matrix",
        "wraith",
        "antixrayheuristics",
        "grimac",
        "intave",
        "watchdog",
        "themis",
        "foxaddition",
        "guardianac",
        "ggintegrity",
        "lightanticheat",
        "anarchyexploitfixes",
        "polar"
    );
    
    private final List<String> plugins = new ArrayList<>();
    private final HashMap<Integer, String> ports = new HashMap<>();
    private final Random random = new Random();
    
    private int ticks = 0;
    private boolean waitingPlugins = false;
    private int completionId;
    
    public ServerCommand() {
        super("Server", "Prints server information.");
        
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
            .executes(context -> {
                basicInfo();
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("plugins")
            .executes(context -> {
                if (!waitingPlugins) {
                    completionId = random.nextInt(0, 32767);
                    mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(completionId, "/"));
                    waitingPlugins = true;
                }
                
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("tps")
            .executes(context -> {
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
            .then(literal("start")
                .executes(context -> {
                    scanKnownPorts(getAddress());
                    return SINGLE_SUCCESS;
                })
                .then(argument("from", IntegerArgumentType.integer(0))
                    .then(argument("to", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            scanRange(getAddress(), IntegerArgumentType.getInteger(context, "from"), IntegerArgumentType.getInteger(context, "to"));
                            return SINGLE_SUCCESS;
                        })
                    )
                )
            )
            .then(literal("stop")
                .executes(context -> {
                    PortScanner.killAllScans();
                    return SINGLE_SUCCESS;
                })
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
        } catch (UnknownHostException ignored) {}
        
        MutableText ipText;
        
        if (ipv4.isEmpty()) {
            ipText = TextUtils.copyable(Text.literal(Formatting.GRAY + server.address), server.address);
        } else {
            ipText = TextUtils.copyable(Text.literal(Formatting.GRAY + server.address), server.address);
            ipText.append(TextUtils.copyable(Text.literal(String.format("%s (%s)", Formatting.GRAY, ipv4)), ipv4));
        }
        
        info(Text.literal(String.format("%sIP: ", Formatting.GRAY)).append(ipText));
        
        info("Port: %d", ServerAddress.parse(server.address).getPort());
        
        info(Text.literal("Brand: ").append(mc.getNetworkHandler().getBrand() != null ? TextUtils.copyable(mc.getNetworkHandler().getBrand()) : Text.literal("unknown")).formatted(Formatting.GRAY));
        
        info("Mode: %s", mc.getNetworkHandler().getConnection().isEncrypted() ? "Online" : "Cracked");
        
        info(Text.literal("Motd: ").formatted(Formatting.GRAY).append(server.label != null ? TextUtils.copyable(server.label) : Text.literal("unknown")));
        
        info("Version: %s", server.version.getString());
        
        info("Protocol version: %d", server.protocolVersion);
        
        info("Difficulty: %s (Local: %.2f)",
            mc.world.getDifficulty().getTranslatableName().getString(),
            new LocalDifficulty(
                mc.world.getDifficulty(),
                mc.world.getTimeOfDay(),
                mc.world.getChunk(mc.player.getBlockPos()).getInhabitedTime(),
                DimensionType.MOON_SIZES[mc.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.MOON_PHASE_VISUAL, mc.player.getBlockPos()).getIndex()] // lol
            ).getLocalDifficulty()
        );
        
        info("Day: %d", mc.world.getTimeOfDay() / 24000L);
        
        info("Permission level: %s", Utils.formatPerms(mc.player.getPermissions()));
    }
    
    // Plugins scanning
    
    private void printPlugins() {
        plugins.sort(String.CASE_INSENSITIVE_ORDER);
        plugins.replaceAll(this::formatPluginName);
        
        if (!plugins.isEmpty()) {
            info("Plugins ((highlight)%d(default)): %s.", plugins.size(), String.join(", ", plugins.toArray(new String[0])));
        } else {
            error("No plugins found.");
        }
    }
    
    private String formatPluginName(String pluginName) {
        if (ANTICHEAT_LIST.contains(pluginName.toLowerCase())) {
            return String.format("%s%s(default)", Formatting.RED, pluginName);
        } else if (Strings.CI.contains(pluginName, "exploit")
            || Strings.CI.contains(pluginName, "cheat")
            || Strings.CI.contains(pluginName, "illegal")
        ) {
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
        
        waitingPlugins = false;
        ticks = 0;
        plugins.clear();
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
        PortScanner.ScanRunner scanRunner = new PortScanner.ScanRunner(address, 5, 3, 200, port_list, scanResults -> {
            int open_ports = 0;
            info("Open ports:");
            for (PortScanner.ScanResult result : scanResults) {
                if (result.isOpen()) {
                    info(formatPort(result.getPort(), address));
                    open_ports++;
                }
            }
            info("Open count: %d/%d", open_ports, scanResults.size());
        });
        PortScanner.scans.add(scanRunner);
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
