/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.proxies;

import com.google.common.net.InetAddresses;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.SharedConstants;
import net.minecraft.client.network.RedirectResolver;
import net.minecraft.client.network.ServerAddress;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyPinger {

    private static final int DEFAULT_PORT = 25565;
    private static final int MAX_PACKET_SIZE = 2 * 1024 * 1024;

    // Packet ids, all of them fit in a single byte
    private static final int HANDSHAKE = 0x00;
    private static final int STATUS_REQUEST = 0x00;
    private static final int STATUS_RESPONSE = 0x00;
    private static final int PING_REQUEST = 0x01;
    private static final int PING_RESPONSE = 0x01;

    private static final AtomicInteger IN_FLIGHT = new AtomicInteger();
    private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);

    private static final ThreadFactory THREAD_FACTORY = task -> {
        Thread thread = new Thread(task, "Meteor-Proxy-Pinger-" + THREAD_NUMBER.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    };

    private static RedirectResolver srvResolver;

    private ProxyPinger() {}

    public static boolean isPinging() {
        return IN_FLIGHT.get() > 0;
    }

    public static void ping(Proxy proxy) {
        if (!begin(proxy)) {
            return;
        }

        MeteorExecutor.execute(() -> run(proxy));
    }

    public static void pingAll() {
        List<Proxy> targets = new ArrayList<>(Proxies.get().getCount());
        for (Proxy proxy : Proxies.get()) {
            if (begin(proxy)) {
                targets.add(proxy);
            }
        }

        if (targets.isEmpty()) {
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(Proxies.get().pingThreads.get(), targets.size()), THREAD_FACTORY);
        for (Proxy proxy : targets) {
            executor.execute(() -> run(proxy));
        }
        executor.shutdown();
    }

    private static boolean begin(Proxy proxy) {
        if (proxy.ping == Proxy.PINGING) {
            return false;
        }

        proxy.ping = Proxy.PINGING;
        IN_FLIGHT.incrementAndGet();

        return true;
    }

    private static void run(Proxy proxy) {
        int ping;

        try {
            ping = pingImpl(proxy);
        } catch (Throwable ignored) {
            ping = Proxy.FAILED;
        }

        proxy.ping = ping;
        IN_FLIGHT.decrementAndGet();
    }

    private static int pingImpl(Proxy proxy) throws IOException {
        Proxies proxies = Proxies.get();

        ServerAddress target = resolveTarget(proxies.pingAddress.get());
        if (target == null) {
            return Proxy.FAILED;
        }

        int timeout = proxies.pingTimeout.get();

        try (Socket socket = new Socket()) {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(timeout);
            socket.connect(new InetSocketAddress(proxy.address, proxy.port), timeout);

            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            socks5Connect(in, out, proxy, target);

            writeHandshake(out, target);
            readStatusResponse(in);

            // Everything above is setup: the tcp connect, the socks handshake and the status exchange. Vanilla
            // MultiplayerServerListPinger times the ping round trip on the already established connection and nothing
            // else, so that is all we measure here too.
            long start = System.nanoTime();
            writePingRequest(out, start);
            readPingResponse(in);

            return (int) ((System.nanoTime() - start) / 1_000_000L);
        }
    }

    /**
     * Resolves the configured address the same way the multiplayer screen does: a bare host gets the default
     * port and, if the port is the default one, an SRV record can redirect it elsewhere.
     */
    @Nullable
    private static ServerAddress resolveTarget(String address) {
        address = address.trim();
        if (address.isEmpty() || !ServerAddress.isValid(address)) {
            return null;
        }

        ServerAddress target = ServerAddress.parse(address);
        if (target.getPort() != DEFAULT_PORT) {
            return target;
        }

        try {
            return srvResolver().lookupRedirect(target).orElse(target);
        } catch (Throwable ignored) {
            return target;
        }
    }

    private static synchronized RedirectResolver srvResolver() {
        if (srvResolver == null) {
            srvResolver = RedirectResolver.createSrv();
        }
        return srvResolver;
    }

    // SOCKS5, see RFC 1928 and RFC 1929

    private static void socks5Connect(DataInputStream in, DataOutputStream out, Proxy proxy, ServerAddress target) throws IOException {
        boolean authentication = proxy.hasAuthentication();

        out.write(0x05);
        if (authentication) {
            out.write(2);
            out.write(0x00);
            out.write(0x02);
        } else {
            out.write(1);
            out.write(0x00);
        }
        out.flush();

        if (in.readUnsignedByte() != 0x05) {
            throw new IOException("Not a SOCKS5 proxy");
        }

        int method = in.readUnsignedByte();
        if (method == 0x02) {
            if (!authentication) {
                throw new IOException("Proxy requires authentication");
            }

            byte[] username = proxy.username.getBytes(StandardCharsets.UTF_8);
            byte[] password = proxy.password.getBytes(StandardCharsets.UTF_8);
            if (username.length > 255 || password.length > 255) {
                throw new IOException("Credentials too long");
            }

            out.write(0x01);
            out.write(username.length);
            out.write(username);
            out.write(password.length);
            out.write(password);
            out.flush();

            in.readUnsignedByte(); // Version
            if (in.readUnsignedByte() != 0x00) {
                throw new IOException("Authentication failed");
            }
        } else if (method != 0x00) {
            throw new IOException("Unsupported authentication method " + method);
        }

        out.write(0x05);
        out.write(0x01); // Connect
        out.write(0x00); // Reserved
        writeSocksAddress(out, target.getAddress());
        out.writeShort(target.getPort());
        out.flush();

        in.readUnsignedByte(); // Version
        int reply = in.readUnsignedByte();
        if (reply != 0x00) {
            throw new IOException("Connect request failed with " + reply);
        }
        in.readUnsignedByte(); // Reserved
        skipSocksAddress(in);
    }

    private static void writeSocksAddress(DataOutputStream out, String host) throws IOException {
        // Hostnames are sent as is so the proxy resolves them instead of us
        if (!InetAddresses.isInetAddress(host)) {
            byte[] bytes = host.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 255) {
                throw new IOException("Host too long");
            }

            out.write(0x03);
            out.write(bytes.length);
            out.write(bytes);
            return;
        }

        byte[] bytes = InetAddresses.forString(host).getAddress();
        out.write(bytes.length == 4 ? 0x01 : 0x04);
        out.write(bytes);
    }

    private static void skipSocksAddress(DataInputStream in) throws IOException {
        int type = in.readUnsignedByte();

        switch (type) {
            case 0x01 -> in.skipNBytes(4);
            case 0x03 -> in.skipNBytes(in.readUnsignedByte());
            case 0x04 -> in.skipNBytes(16);
            default -> throw new IOException("Unknown address type " + type);
        }

        in.skipNBytes(2); // Port
    }

    // Server list ping

    private static void writeHandshake(DataOutputStream out, ServerAddress target) throws IOException {
        ByteArrayOutputStream handshake = new ByteArrayOutputStream();
        DataOutputStream handshakeOut = new DataOutputStream(handshake);
        writeVarInt(handshakeOut, HANDSHAKE);
        writeVarInt(handshakeOut, SharedConstants.getProtocolVersion());
        writeString(handshakeOut, target.getAddress());
        handshakeOut.writeShort(target.getPort());
        writeVarInt(handshakeOut, 1); // Next state: status

        writePacket(out, handshake.toByteArray());
        writePacket(out, new byte[] { STATUS_REQUEST });
        out.flush();
    }

    private static void readStatusResponse(DataInputStream in) throws IOException {
        // The status itself is of no interest, but it has to be off the wire before the ping is sent
        in.skipNBytes(readPacketHeader(in, STATUS_RESPONSE));
    }

    private static void writePingRequest(DataOutputStream out, long payload) throws IOException {
        writeVarInt(out, 1 + Long.BYTES);
        out.write(PING_REQUEST);
        out.writeLong(payload);
        out.flush();
    }

    private static void readPingResponse(DataInputStream in) throws IOException {
        in.skipNBytes(readPacketHeader(in, PING_RESPONSE));
    }
    
    private static int readPacketHeader(DataInputStream in, int id) throws IOException {
        int length = readVarInt(in);
        if (length < 1 || length > MAX_PACKET_SIZE) {
            throw new IOException("Invalid packet length " + length);
        }

        int actual = in.readUnsignedByte();
        if (actual != id) {
            throw new IOException("Expected packet " + id + " but got " + actual);
        }

        return length - 1;
    }

    private static void writePacket(DataOutputStream out, byte[] data) throws IOException {
        writeVarInt(out, data.length);
        out.write(data);
    }

    private static void writeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;

        for (int i = 0; i < 5; i++) {
            int b = in.readUnsignedByte();
            value |= (b & 0x7F) << (i * 7);

            if ((b & 0x80) == 0) {
                return value;
            }
        }

        throw new IOException("VarInt too big");
    }

}
