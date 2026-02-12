/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public record FontInfo(String family, Type type) {
    
    @Override
    public String toString() {
        return family + " " + type;
    }
    
    public boolean equals(FontInfo info) {
        if (this == info) {
            return true;
        }
        if (info == null || family == null || type == null) {
            return false;
        }
        return family.equals(info.family) && type == info.type;
    }
    
    public enum Type implements IDisplayName {
        
        REGULAR("Regular"),
        BOLD("Bold"),
        ITALIC("Italic"),
        BOLD_ITALIC("Bold Italic");
        
        private final String displayName;
        
        Type(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
        public static Type fromString(String str) {
            return switch (str) {
                case "Bold" -> BOLD;
                case "Italic" -> ITALIC;
                case "Bold Italic", "BoldItalic" -> BOLD_ITALIC;
                default -> REGULAR;
            };
        }
    }
    
}
