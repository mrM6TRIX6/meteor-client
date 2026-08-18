/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render.ui.msdf;

import net.minecraft.client.texture.TextureSetup;

import java.util.Map;

public record MsdfAtlas(
    Map<Integer, MsdfGlyph> glyphs,
    MsdfGlyph[] asciiGlyphs,
    TextureSetup textureSetup,
    float fontSize,
    float distanceRange
) {
    
    boolean ready() {
        return !glyphs.isEmpty();
    }
    
    MsdfAtlas withTextureSetup(TextureSetup setup) {
        return new MsdfAtlas(glyphs, asciiGlyphs, setup, fontSize, distanceRange);
    }
    
}