/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {
    
    @Shadow
    @Nullable
    volatile ClientConnection connection;
    
    @Unique
    private ServerAddress serverAddress = null;
    
    protected ConnectScreenMixin(Text title) {
        super(title);
    }
    
    @Inject(method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V", at = @At("HEAD"))
    private void onConnect(MinecraftClient client, ServerAddress address, ServerInfo info, CookieStorage cookieStorage, CallbackInfo ci) {
        serverAddress = address;
        MeteorClient.EVENT_BUS.post(ServerConnectBeginEvent.get(address, info));
    }
    
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        /*
         * Make a text demonstration of the connection status
         * This is useful for debugging the connection trace
         *
         * Looks like this: Client <> Proxy <> Server
         *
         * For Client, it should show the actual client IP
         * For Proxy, it should show the proxy IP
         * For Server, it should show the server IP
         */
        
        ClientConnection clientConnection = this.connection;
        ServerAddress serverAddress = this.serverAddress;
        
        if (clientConnection == null || this.serverAddress == null) {
            return;
        }
        
        Text connectionDetails = getConnectionDetails(clientConnection, serverAddress);
        context.drawCenteredTextWithShadow(textRenderer, connectionDetails, width / 2, height / 2 - 60, -1);
    }
    
    @ModifyConstant(method = "render", constant = @Constant(intValue = 50))
    private int modifyStatusY(int original) {
        return original + 30;
    }
    
    @Unique
    private Text getConnectionDetails(ClientConnection clientConnection, ServerAddress serverAddress) {
        // This will either be the socket address or the server address
        String socketAddr = getSocketAddress(clientConnection, serverAddress);
        String serverAddr = String.format(
            "%s:%s",
            serverAddress.getAddress(),
            serverAddress.getPort()
        );
        
        MutableText client = Text.literal("Client").formatted(Formatting.BLUE);
        MutableText spacer = Text.literal(" ⟺ ").formatted(Formatting.DARK_GRAY);
        
        MutableText socket = Text.literal(socketAddr);
        if (Proxies.get().getEnabled() != null) {
            socket.formatted(Formatting.GOLD); // Proxy good
        } else {
            socket.formatted(Formatting.RED); // No proxy - shows server address
        }
        
        MutableText server = Text.literal(serverAddr).formatted(Formatting.GREEN);
        
        return client.append(spacer)
            .append(socket)
            .append(spacer)
            .append(server);
    }
    
    @Unique
    private static String getSocketAddress(ClientConnection clientConnection, ServerAddress serverAddress) {
        String socketAddr;
        if (clientConnection.getAddress() instanceof InetSocketAddress address) {
            // In this we do not redact the host string - it is usually not sensitive
            var hostString = address.getHostString();
            var hostAddress = address.isUnresolved() ?
                "<unresolved>" :
                address.getAddress().getHostAddress();
            
            if (hostString.equals(serverAddress.getAddress())) {
                socketAddr = String.format("%s:%s", hostAddress, address.getPort());
            } else {
                socketAddr = String.format("%s/%s:%s", hostString, hostAddress, address.getPort());
            }
        } else {
            socketAddr = "<unknown>";
        }
        return socketAddr;
    }
    
}
