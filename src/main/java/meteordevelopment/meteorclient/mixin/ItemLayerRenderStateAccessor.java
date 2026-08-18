/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface ItemLayerRenderStateAccessor {
    
    @Accessor("glint")
    ItemRenderState.Glint meteor$getFoilType();
    
    @Accessor("transform")
    Transformation meteor$getItemTransform();
    
    @Accessor("useLight")
    boolean meteor$getUsesBlockLight();
    
    @Accessor("specialModelType")
    SpecialModelRenderer<Object> meteor$getSpecialRenderer();
    
    @Accessor("tints")
    int[] meteor$getTintLayers();
    
}
