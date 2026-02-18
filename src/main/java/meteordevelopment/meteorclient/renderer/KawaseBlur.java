package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gl.DynamicUniformStorage;

import java.nio.ByteBuffer;

public class KawaseBlur {
    
    public static final int MAX_PASSES = 8;
    
    private final GpuTextureView[] down = new GpuTextureView[MAX_PASSES];
    private final GpuTextureView[] up = new GpuTextureView[MAX_PASSES];
    
    private GpuTextureView source;
    private GpuTextureView result;
    
    private int width = -1;
    private int height = -1;
    
    private GpuBufferSlice[] ubos;
    
    public void ensure(int w, int h) {
        if (w == width && h == height) {
            return;
        }
        
        destroy();
        
        width = w;
        height = h;
        
        source = create(w, h);
        result = create(w, h);
        
        int cw = w;
        int ch = h;
        
        for (int i = 0; i < MAX_PASSES; i++) {
            cw = Math.max(1, cw / 2);
            ch = Math.max(1, ch / 2);
            
            down[i] = create(cw, ch);
            up[i] = create(cw, ch);
        }
        
    }
    
    private GpuTextureView create(int w, int h) {
        GpuTexture tex = RenderSystem.getDevice().createTexture(
            "kawase",
            15,
            TextureFormat.RGBA8,
            w,
            h,
            1,
            1
        );
        return RenderSystem.getDevice().createTextureView(tex);
    }
    
    public GpuTextureView getSource() {
        return source;
    }
    
    public GpuTextureView blur(int passes, float offset) {
        passes = Math.min(passes, MAX_PASSES);
        if (passes <= 0) {
            return source;
        }
        
        updateUniforms(passes, offset);
        
        GpuTextureView input = source;
        
        // Downsampling
        for (int i = 0; i < passes; i++) {
            GpuTextureView target = down[i];
            
            MeshRenderer.begin()
                .attachments(target, null)
                .pipeline(MeteorRenderPipelines.KAWASE_BLUR_DOWN)
                .clearColor(Color.CLEAR)
                .fullscreen()
                .uniform("BlurData", ubos[i])
                .sampler("u_Texture", input, RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
                .end();
            
            input = target;
        }
        
        // Upsampling
        for (int i = passes - 1; i >= 0; i--) {
            GpuTextureView target = (i == 0) ? result : up[i - 1];
            
            MeshRenderer.begin()
                .attachments(target, null)
                .pipeline(MeteorRenderPipelines.KAWASE_BLUR_UP)
                .clearColor(Color.CLEAR)
                .fullscreen()
                .uniform("BlurData", ubos[i])
                .sampler("u_Texture", input, RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
                .end();
            
            input = target;
        }
        
        return result;
    }
    
    private void updateUniforms(int passes, float offset) {
        UNIFORM_STORAGE.clear();
        
        BlurUniformData[] data = new BlurUniformData[passes];
        
        int cw = width;
        int ch = height;
        
        for (int i = 0; i < passes; i++) {
            cw = Math.max(1, cw / 2);
            ch = Math.max(1, ch / 2);
            
            data[i] = new BlurUniformData(
                1F / cw,
                1F / ch,
                offset
            );
        }
        
        ubos = UNIFORM_STORAGE.writeAll(data);
    }
    
    public void destroy() {
        close(source);
        close(result);
        
        for (int i = 0; i < MAX_PASSES; i++) {
            close(down[i]);
            close(up[i]);
        }
    }
    
    private void close(GpuTextureView v) {
        if (v != null) {
            v.close();
        }
    }
    
    // UBO
    
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec2()
        .putFloat()
        .get();
    
    private static final FixedUniformStorage<BlurUniformData> UNIFORM_STORAGE = new FixedUniformStorage<>("KawaseUBO", UNIFORM_SIZE, MAX_PASSES);
    
    private record BlurUniformData(float texelW, float texelH, float offset) implements DynamicUniformStorage.Uploadable {
        
        @Override
        public void write(ByteBuffer buf) {
            Std140Builder.intoBuffer(buf)
                .putVec2(texelW, texelH)
                .putFloat(offset);
        }
        
    }
    
}
