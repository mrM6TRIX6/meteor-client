/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network.portscanner;

import java.util.ArrayList;
import java.util.List;

public class PortScannerManager {
    
    public static List<PortScanRunner> scans = new ArrayList<>();
    
    public static void killAllScans() {
        for (PortScanRunner runner : scans) {
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
    
}
