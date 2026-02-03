/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public class EntityRenderAfterTranslateEvent {
    
    private static final EntityRenderAfterTranslateEvent INSTANCE = new EntityRenderAfterTranslateEvent();
    
    public EntityRenderState renderState;
    public MatrixStack matrices;
    
    public static EntityRenderAfterTranslateEvent get(EntityRenderState renderState, MatrixStack matrices) {
        INSTANCE.renderState = renderState;
        INSTANCE.matrices = matrices;
        
        return INSTANCE;
    }
    
}
