package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class RenderLayerFactory {
    
    private RenderLayerFactory() {}
    
    public static RenderLayer of(String name, int expectedBufferSize, RenderPipeline pipeline) {
        return of(name, expectedBufferSize, pipeline, null);
    }
    
    public static RenderLayer of(String name, int expectedBufferSize, RenderPipeline pipeline, @Nullable Identifier texture) {
        RenderSetup.Builder builder = RenderSetup.builder(pipeline).expectedBufferSize(expectedBufferSize);
        if (texture != null) {
            builder.texture("Sampler0", texture);
        }
        return RenderLayer.of(name, builder.build());
    }
    
}
