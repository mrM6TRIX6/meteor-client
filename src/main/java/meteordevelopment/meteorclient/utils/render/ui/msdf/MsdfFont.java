/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render.ui.msdf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import meteordevelopment.meteorclient.IMinecraft;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public enum MsdfFont implements IMinecraft {
    
    MONTSERRAT_REGULAR("montserrat_regular"),
    MONTSERRAT_MEDIUM("montserrat_medium"),
    MONTSERRAT_SEMIBOLD("montserrat_semibold"),
    MONTSERRAT_BOLD("montserrat_bold"),
    JETBRAINS_MONO_REGULAR("jetbrains_mono_regular");
    
    private static final int MAX_WIDTH_CACHE = 512;
    private final Map<MsdfWidthKey, Float> widthCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<MsdfWidthKey, Float> eldest) {
            return size() > MAX_WIDTH_CACHE;
        }
    };
    
    private final String fontName;
    private final MsdfAtlas atlas;
    
    MsdfFont(String fontName) {
        this.fontName = fontName;
        this.atlas = load();
    }
    
    public String fontName() {
        return fontName;
    }
    
    public MsdfAtlas atlas() {
        return atlas;
    }
    
    public boolean canRender(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        if (!atlas.ready()) {
            return false;
        }
        
        int index = 0;
        
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            
            if (Character.isISOControl(codePoint)) {
                return false;
            }
            
            if (glyph(codePoint) == null) {
                return false;
            }
        }
        return true;
    }
    
    public float width(String text, float size) {
        if (text == null || text.isEmpty() || size <= 0.0f) {
            return 0.0f;
        }
        
        if (!atlas.ready()) {
            return 0.0f;
        }
        
        if (text.codePointCount(0, text.length()) == 1) {
            MsdfGlyph glyph = glyph(text.codePointAt(0));
            return glyph == null ? 0.0f : glyph.advance() * (size / atlas.fontSize());
        }
        
        MsdfWidthKey key = new MsdfWidthKey(fontName(), text, normalizeSize(size));
        Float cached = widthCache.get(key);
        
        if (cached != null) {
            return cached;
        }
        
        float scale = size / atlas.fontSize();
        float width = 0.0f;
        int index = 0;
        
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            MsdfGlyph glyph = glyph(codePoint);
            if (glyph != null) {
                width += glyph.advance() * scale;
            }
        }
        
        widthCache.put(key, width);
        return width;
    }
    
    private MsdfAtlas load() {
        Map<Integer, MsdfGlyph> glyphs = new HashMap<>();
        MsdfGlyph[] asciiGlyphs = new MsdfGlyph[128];
        
        if (mc.getResourceManager() == null) {
            return new MsdfAtlas(glyphs, asciiGlyphs, null, 96.0f, 8.0f);
        }
        
        Identifier jsonId = MeteorClient.identifier("fonts/" + fontName + ".json");
        Optional<Resource> resource = mc.getResourceManager().getResource(jsonId);
        
        if (resource.isEmpty()) {
            return new MsdfAtlas(glyphs, asciiGlyphs, null, 96.0f, 8.0f);
        }
        
        float fontSize = 96.0f;
        float distanceRange = 8.0f;
        
        try (InputStream stream = resource.get().getInputStream();
            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject atlas = root.getAsJsonObject("atlas");
            float atlasWidth = getFloat(atlas, "width", 4096.0f);
            float atlasHeight = getFloat(atlas, "height", 4096.0f);
            fontSize = getFloat(atlas, "size", 96.0f);
            distanceRange = Math.max(1.0f, getFloat(atlas, "distanceRange", 8.0f));
            boolean originBottom = atlas.has("yOrigin") && "bottom".equalsIgnoreCase(atlas.get("yOrigin").getAsString());
            
            for (JsonElement element : root.getAsJsonArray("glyphs")) {
                JsonObject glyph = element.getAsJsonObject();
                if (!glyph.has("unicode")) {
                    continue;
                }
                
                int unicode = glyph.get("unicode").getAsInt();
                MsdfGlyph msdfGlyph;
                
                if (glyph.has("atlasBounds") && glyph.has("planeBounds")) {
                    JsonObject atlasBounds = glyph.getAsJsonObject("atlasBounds");
                    JsonObject planeBounds = glyph.getAsJsonObject("planeBounds");
                    
                    float left = getFloat(atlasBounds, "left", 0.0f);
                    float bottom = getFloat(atlasBounds, "bottom", 0.0f);
                    float right = getFloat(atlasBounds, "right", 0.0f);
                    float top = getFloat(atlasBounds, "top", 0.0f);
                    float textureY = originBottom ? atlasHeight - top : bottom;
                    
                    float planeLeft = getFloat(planeBounds, "left", 0.0f);
                    float planeBottom = getFloat(planeBounds, "bottom", 0.0f);
                    float planeRight = getFloat(planeBounds, "right", 0.0f);
                    float planeTop = getFloat(planeBounds, "top", 0.0f);
                    float ascender = 0.95f;
                    
                    msdfGlyph = new MsdfGlyph(
                        planeLeft * fontSize,
                        (ascender - planeTop) * fontSize,
                        (planeRight - planeLeft) * fontSize,
                        (planeTop - planeBottom) * fontSize,
                        getFloat(glyph, "advance", 1.0f) * fontSize,
                        left / atlasWidth,
                        textureY / atlasHeight,
                        right / atlasWidth,
                        (textureY + top - bottom) / atlasHeight
                    );
                } else {
                    msdfGlyph = new MsdfGlyph(
                        0.0f, 0.0f, 0.0f, 0.0f,
                        getFloat(glyph, "advance", 1.0f) * fontSize,
                        0.0f, 0.0f, 0.0f, 0.0f
                    );
                }
                glyphs.put(unicode, msdfGlyph);
                if (unicode >= 0 && unicode < asciiGlyphs.length) {
                    asciiGlyphs[unicode] = msdfGlyph;
                }
            }
            MeteorClient.LOGGER.info("[MsdfFont] Font '{}' loaded", fontName);
        } catch (Exception ignored) {
            glyphs.clear();
            Arrays.fill(asciiGlyphs, null);
        }
        return new MsdfAtlas(glyphs, asciiGlyphs, null, fontSize, distanceRange);
    }
    
    TextureSetup textureSetup() {
        if (atlas.textureSetup() != null) {
            return atlas.textureSetup();
        }
        
        if (mc.getTextureManager() == null) {
            return null;
        }
        
        Identifier textureId = MeteorClient.identifier("fonts/" + fontName + ".png");
        AbstractTexture texture = mc.getTextureManager().getTexture(textureId);
        
        if (texture == null || texture.getGlTextureView() == null) {
            return null;
        }
        return TextureSetup.of(
            texture.getGlTextureView(),
            RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
        );
    }
    
    public MsdfGlyph glyph(int codePoint) {
        if (codePoint >= 0 && codePoint < atlas.asciiGlyphs().length) {
            return atlas.asciiGlyphs()[codePoint];
        }
        return atlas.glyphs().get(codePoint);
    }
    
    private static float normalizeSize(float size) {
        return Math.clamp(Math.round(size * 4.0f) / 4.0f, 1.0f, 512.0f);
    }
    
    private static float getFloat(JsonObject object, String key, float fallback) {
        return object != null && object.has(key) ? object.get(key).getAsFloat() : fallback;
    }
    
    private record MsdfWidthKey(String fontName, String text, float size) {
    
    }
    
}
