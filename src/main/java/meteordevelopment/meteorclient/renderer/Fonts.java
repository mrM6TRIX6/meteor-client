/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.systems.clientsettings.ClientSettings;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.render.FontUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Fonts {
    
    public static final String[] FONTS = { "Montserrat-500", "Montserrat-600", "Montserrat-700" };
    
    public static String DEFAULT_FONT_FAMILY;
    public static FontFace DEFAULT_FONT;
    
    public static final List<FontFamily> FONT_FAMILIES = new ArrayList<>();
    public static CustomTextRenderer RENDERER;
    
    private Fonts() {}
    
    @PreInit
    public static void refresh() {
        FONT_FAMILIES.clear();
        
        for (String font : FONTS) {
            FontUtils.load(FONT_FAMILIES, font);
        }
        
        FONT_FAMILIES.sort(Comparator.comparing(FontFamily::getName));
        
        MeteorClient.LOG.info("Found {} font families.", FONT_FAMILIES.size());
        
        DEFAULT_FONT_FAMILY = FontUtils.getFontInfo(FontUtils.stream(FONTS[0])).family();
        DEFAULT_FONT = getFamily(DEFAULT_FONT_FAMILY).get(FontInfo.Type.Regular);
        
        ClientSettings clientSettings = ClientSettings.get();
        load(clientSettings != null ? clientSettings.font.get() : DEFAULT_FONT);
    }
    
    public static void load(FontFace fontFace) {
        if (RENDERER != null) {
            if (RENDERER.fontFace.equals(fontFace)) {
                return;
            } else {
                RENDERER.destroy();
            }
        }
        
        try {
            RENDERER = new CustomTextRenderer(fontFace);
            MeteorClient.EVENT_BUS.post(CustomFontChangedEvent.get());
        } catch (Exception e) {
            if (fontFace.equals(DEFAULT_FONT)) {
                throw new RuntimeException("Failed to load default font: " + fontFace, e);
            }
            
            MeteorClient.LOG.error("Failed to load font: {}", fontFace, e);
            load(Fonts.DEFAULT_FONT);
        }
        
        if (mc.currentScreen instanceof WidgetScreen) {
            ((WidgetScreen) mc.currentScreen).invalidate();
        }
    }
    
    public static FontFamily getFamily(String name) {
        for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
            if (fontFamily.getName().equalsIgnoreCase(name)) {
                return fontFamily;
            }
        }
        return null;
    }
    
}
