/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PortScanner {
    
    public static final List<ScanRunner> scans = new ArrayList<>();
    
    public static void killAllScans() {
        for (ScanRunner runner : scans) {
            if (runner.running) {
                runner.cancel();
            }
        }
        scans.clear();
    }
    
    public static class ScanResult {
        
        private int port;
        
        private boolean isOpen;
        
        public ScanResult(int port, boolean isOpen) {
            super();
            this.port = port;
            this.isOpen = isOpen;
        }
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public boolean isOpen() {
            return isOpen;
        }
        
        public void setOpen(boolean isOpen) {
            this.isOpen = isOpen;
        }
        
    }
    
    public static class ScanRunner {
        
        public boolean running = true;
        public int portsScanned = 0;
        
        ExecutorService es;
        List<Future<ScanResult>> futures = new ArrayList<>();
        Thread runner;
        
        public ScanRunner(InetAddress address, int threads, int threadDelay, int timeoutMS, Collection<Integer> ports, Consumer<List<ScanResult>> callback) {
            runner = new Thread(() -> {
                es = Executors.newFixedThreadPool(threads);
                ports.forEach(port -> futures.add(isPortOpen(es, address.getHostAddress(), port, timeoutMS, threadDelay)));
                
                try {
                    es.awaitTermination(200L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {}
                
                List<ScanResult> results = new ArrayList<>();
                
                for (Future<ScanResult> fsc : futures) {
                    try {
                        results.add(fsc.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                
                callback.accept(results);
            });
            
            runner.start();
        }
        
        public void cancel() {
            running = false;
        }
        
        private Future<ScanResult> isPortOpen(ExecutorService es, String ip, int port, int timeout, int delay) {
            return es.submit(() -> {
                if (!running) {
                    return new ScanResult(port, false);
                }
                
                Thread.sleep(delay);
                portsScanned++;
                
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    socket.close();
                    return new ScanResult(port, true);
                } catch (Exception exc) {
                    return new ScanResult(port, false);
                }
            });
        }
        
    }
    
}
