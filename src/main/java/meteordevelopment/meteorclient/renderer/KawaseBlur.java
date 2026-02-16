package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gl.DynamicUniformStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class KawaseBlur {
    
    private static final int MAX_PASSES = 8;
    
    private final GpuTextureView[] buffers = new GpuTextureView[2];
    private GpuTextureView sourceFBO;
    private GpuBufferSlice[] ubos;
    
    private int width = -1;
    private int height = -1;
    
    public KawaseBlur() {}
    
    public GpuTextureView getSourceFBO() {
        return sourceFBO;
    }
    
    public void ensure(int w, int h) {
        if (w == width && h == height) {
            return;
        }
        
        destroy();
        
        width = w;
        height = h;
        
        sourceFBO = createFbo("blur_source", w, h);
        buffers[0] = createFbo("blur_down", w, h);
        buffers[1] = createFbo("blur_up", w, h);
    }
    
    /**
     * Делает blur. Возвращает новый FBO с размытым результатом. Исходный sourceFBO не затирается.
     */
    public GpuTextureView blur(int passes, float offset) {
        if (sourceFBO == null || passes <= 0) {
            return sourceFBO;
        }
        
        for (int i = 0; i < 2; i++) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(
                buffers[i].texture(), Color.CLEAR.getPacked()
            );
        }
        
        passes = Math.min(passes, MAX_PASSES);
        updateUniforms(offset);
        
        GpuTextureView input = sourceFBO;
        GpuTextureView output;
        
        // Downsample
        for (int i = 0; i < passes; i++) {
            int targetIndex = i % 2;
            output = buffers[targetIndex];
            
            renderPass(output, input, MeteorRenderPipelines.KAWASE_BLUR_DOWN, ubos[i]);
            input = output;
        }
        
        // Upsample
        for (int i = passes - 1; i >= 0; i--) {
            int targetIndex = i % 2;
            output = buffers[targetIndex];
            
            renderPass(output, input, MeteorRenderPipelines.KAWASE_BLUR_UP, ubos[i]);
            input = output;
        }
        
        return input;
    }
    
    public void drawResult(GpuTextureView result) {
        MeshRenderer.begin()
            .attachments(mc.getFramebuffer())
            .pipeline(MeteorRenderPipelines.BLUR_PASSTHROUGH)
            .fullscreen()
            .sampler("u_Texture", result, RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
            .end();
    }
    
    private void renderPass(GpuTextureView target, GpuTextureView source, RenderPipeline pipeline, GpuBufferSlice ubo) {
        MeshRenderer.begin()
            .attachments(target, null)
            .pipeline(pipeline)
            .fullscreen()
            .uniform("BlurData", ubo)
            .sampler("u_Texture", source, RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
            .end();
    }
    
    public GpuTextureView createFbo(String name, int w, int h) {
        return RenderSystem.getDevice().createTextureView(
            RenderSystem.getDevice().createTexture(
                name,
                15,
                TextureFormat.RGBA8,
                w,
                h,
                1,
                1
            )
        );
    }
    
    public void destroy() {
        if (sourceFBO != null) {
            sourceFBO.close();
        }
        for (int i = 0; i < 2; i++) {
            if (buffers[i] != null) {
                buffers[i].close();
            }
        }
    }
    
    // Uniforms
    
    private void updateUniforms(float offset) {
        UNIFORM_STORAGE.clear();
        
        BlurUniformData[] data = new BlurUniformData[MAX_PASSES];
        for (int i = 0; i < MAX_PASSES; i++) {
            data[i] = new BlurUniformData(
                0.5f / width,
                0.5f / height,
                offset
            );
        }
        
        ubos = UNIFORM_STORAGE.writeAll(data);
    }
    
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec2()
        .putFloat()
        .get();
    
    private static final FixedUniformStorage<BlurUniformData> UNIFORM_STORAGE = new FixedUniformStorage<>("Blur UBO", UNIFORM_SIZE, MAX_PASSES);
    
    private record BlurUniformData(float halfTexelX, float halfTexelY, float offset) implements DynamicUniformStorage.Uploadable {
        
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(halfTexelX, halfTexelY)
                .putFloat(offset);
        }
        
    }
    
}
