package meteordevelopment.meteorclient.utils.render.ui.ripple;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.DrawContextAccessor;
import meteordevelopment.meteorclient.utils.render.ScissorUtil;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class RippleRenderer implements AutoCloseable {
    
    private static final int MAX_RIPPLES = 512;
    private static final int PARAMS_PER_RIPPLE = 5;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = MAX_RIPPLES * PARAMS_PER_RIPPLE * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile RippleRenderer instance;
    
    public static final RenderPipeline RIPPLE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/ripple"))
        .withVertexShader(MeteorClient.identifier("ui/ripple/ripple"))
        .withFragmentShader(MeteorClient.identifier("ui/ripple/ripple"))
        .withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("RippleParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<BuiltRipple> preparedRipples = new ArrayList<>(128);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    
    private RippleRenderer() {}
    
    public static RippleRenderer getInstance() {
        RippleRenderer local = instance;
        if (local == null) {
            synchronized (RippleRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new RippleRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        RippleRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        activeContext = context;
    }
    
    public void enqueue(BuiltRipple ripple) {
        submit(activeContext, ripple);
    }
    
    public void flush() {
        activeContext = null;
    }
    
    public void beginGuiFrame() {
        preparedRipples.clear();
        paramsDirty = false;
    }
    
    public boolean isRipplePipeline(RenderPipeline pipeline) {
        return pipeline == RIPPLE_PIPELINE;
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedRipples.isEmpty()) {
            return;
        }
        
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("RippleParamsArray", buffer);
        }
    }
    
    public void prepareBuffers() {
        if (preparedRipples.isEmpty() || !paramsDirty) {
            return;
        }
        
        GpuBuffer buffer = ensureWritableParamsBuffer();
        if (buffer == null) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer uniformData = buildUniformData(stack, preparedRipples);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(buffer.slice(0, uniformData.remaining()), uniformData);
            paramsDirty = false;
        } catch (RuntimeException ignored) {
            paramsDirty = true;
        }
    }
    
    boolean reserve(BuiltRipple ripple) {
        if (preparedRipples.size() == MAX_RIPPLES) {
            return false;
        }
        
        preparedRipples.add(ripple);
        paramsDirty = true;
        return true;
    }
    
    private void submit(DrawContext context, BuiltRipple ripple) {
        if (context == null || ripple == null || !ripple.visible()) {
            return;
        }
        
        try {
            Matrix3x2f pose = Render2D.pose(context);
            ((DrawContextAccessor) context)
                .meteor$getState()
                .addSimpleElement(new RippleRenderState(pose, ripple, ScissorUtil.current()));
        } catch (RuntimeException ignored) {
        }
    }
    
    private GpuBuffer ensureParamsBuffer() {
        if (!paramsDirty && paramsBuffer != null) {
            return paramsBuffer;
        }
        
        prepareBuffers();
        if (!paramsDirty && paramsBuffer != null) {
            return paramsBuffer;
        }
        
        closeParamsBuffer();
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer uniformData = buildUniformData(stack, preparedRipples);
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_ripple_params", GpuBuffer.USAGE_UNIFORM, uniformData);
            paramsDirty = false;
            return paramsBuffer;
        }
    }
    
    private GpuBuffer ensureWritableParamsBuffer() {
        if (paramsBuffer != null && !paramsBuffer.isClosed() && paramsBuffer.size() >= UNIFORM_BYTES) {
            return paramsBuffer;
        }
        
        closeParamsBuffer();
        
        try {
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_ripple_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltRipple> batch) {
        int usedBytes = Math.max(1, batch.size()) * PARAMS_PER_RIPPLE * FLOATS_PER_PARAM * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        
        for (int i = 0; i < batch.size(); i++) {
            BuiltRipple ripple = batch.get(i);
            int offset = i * PARAMS_PER_RIPPLE * FLOATS_PER_PARAM * Float.BYTES;
            
            data.putFloat(offset, ripple.radiusTopLeft());
            data.putFloat(offset + 4, ripple.radiusTopRight());
            data.putFloat(offset + 8, ripple.radiusBottomRight());
            data.putFloat(offset + 12, ripple.radiusBottomLeft());
            
            data.putFloat(offset + 16, ripple.width());
            data.putFloat(offset + 20, ripple.height());
            data.putFloat(offset + 24, ripple.smoothness());
            data.putFloat(offset + 28, ripple.isTextured() ? 1.0f : 0.0f);
            
            data.putFloat(offset + 32, ripple.centerX());
            data.putFloat(offset + 36, ripple.centerY());
            data.putFloat(offset + 40, ripple.rippleRadius());
            data.putFloat(offset + 44, ripple.rippleSmoothness());
            
            putColor(data, offset + 48, ripple.sourceColor());
            
            putColor(data, offset + 64, ripple.targetColor());
        }
        
        data.position(0);
        return data;
    }
    
    private static void putColor(ByteBuffer data, int offset, int color) {
        data.putFloat(offset, ((color >>> 16) & 0xFF) / 255.0f);
        data.putFloat(offset + 4, ((color >>> 8) & 0xFF) / 255.0f);
        data.putFloat(offset + 8, (color & 0xFF) / 255.0f);
        data.putFloat(offset + 12, ((color >>> 24) & 0xFF) / 255.0f);
    }
    
    private void closeParamsBuffer() {
        if (paramsBuffer != null) {
            paramsBuffer.close();
            paramsBuffer = null;
        }
    }
    
    private static Identifier id(String path) {
        return Identifier.of("meteor", path);
    }
    
    @Override
    public void close() {
        preparedRipples.clear();
        activeContext = null;
        closeParamsBuffer();
    }
    
}
