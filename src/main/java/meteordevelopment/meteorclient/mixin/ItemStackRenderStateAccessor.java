/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface ItemStackRenderStateAccessor {
    
    @Accessor("layerCount")
    int meteor$getActiveLayerCount();
    
    @Accessor("layers")
    ItemRenderState.LayerRenderState[] meteor$getLayers();
    
}
