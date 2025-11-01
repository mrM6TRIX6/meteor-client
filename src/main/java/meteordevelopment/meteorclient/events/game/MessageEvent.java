/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;

public class MessageEvent {
    
    public static class Receive extends Cancellable {
        
        private static final Receive INSTANCE = new Receive();
        
        private Text message;
        private MessageIndicator indicator;
        private boolean modified;
        public int id;
        
        public static Receive get(Text message, MessageIndicator indicator, int id) {
            INSTANCE.setCancelled(false);
            INSTANCE.message = message;
            INSTANCE.indicator = indicator;
            INSTANCE.modified = false;
            INSTANCE.id = id;
            
            return INSTANCE;
        }
        
        public Text getMessage() {
            return message;
        }
        
        public MessageIndicator getIndicator() {
            return indicator;
        }
        
        public void setMessage(Text message) {
            this.message = message;
            this.modified = true;
        }
        
        public void setIndicator(MessageIndicator indicator) {
            this.indicator = indicator;
            this.modified = true;
        }
        
        public boolean isModified() {
            return modified;
        }
        
    }
    
    public static class Send extends Cancellable {
        
        private static final Send INSTANCE = new Send();
        
        public String message;
        
        public static Send get(String message) {
            INSTANCE.setCancelled(false);
            INSTANCE.message = message;
            return INSTANCE;
        }
        
    }
    
}
