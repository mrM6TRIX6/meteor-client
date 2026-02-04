/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public abstract class MeteorVertexFormats {
    
    public static final VertexFormat POS2 = VertexFormat.builder()
        .add("Position", MeteorVertexFormatElements.POS2)
        .build();
    
    public static final VertexFormat POS2_COLOR = VertexFormat.builder()
        .add("Position", MeteorVertexFormatElements.POS2)
        .add("Color", VertexFormatElement.COLOR)
        .build();
    
    public static final VertexFormat POS2_TEXTURE_COLOR = VertexFormat.builder()
        .add("Position", MeteorVertexFormatElements.POS2)
        .add("Texture", VertexFormatElement.UV)
        .add("Color", VertexFormatElement.COLOR)
        .build();
    
    public static final VertexFormat POS2_COLOR_UV0_SIZE_RADIUS_SMOOTHNESS = VertexFormat.builder()
        .add("Position", MeteorVertexFormatElements.POS2)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV0", VertexFormatElement.UV0)
        .add("Size", MeteorVertexFormatElements.SIZE)
        .add("Radius", MeteorVertexFormatElements.RADIUS)
        .add("Smoothness", MeteorVertexFormatElements.SMOOTHNESS)
        .build();
    
    private MeteorVertexFormats() {}
    
}
