package meteordevelopment.meteorclient.utils.render.ui.zippy;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.DrawContextAccessor;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class ZippyRenderer implements AutoCloseable {
    
    private static final int MAX_ZIPPY = 256;
    private static final int PARAMS_PER_ZIPPY = 3;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = MAX_ZIPPY * PARAMS_PER_ZIPPY * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile ZippyRenderer instance;
    
    public static final RenderPipeline ZIPPY_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/zippy"))
        .withVertexShader(MeteorClient.identifier("ui/zippy/zippy"))
        .withFragmentShader(MeteorClient.identifier("ui/zippy/zippy"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("ZippyParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<BuiltZippy> preparedZippy = new ArrayList<>(32);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    
    private ZippyRenderer() {}
    
    public static ZippyRenderer getInstance() {
        ZippyRenderer local = instance;
        if (local == null) {
            synchronized (ZippyRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new ZippyRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        ZippyRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        activeContext = context;
    }
    
    public void draw(DrawContext context, BuiltZippy zippy) {
        beginFrame(context);
        enqueue(zippy);
        flush();
    }
    
    public void enqueue(BuiltZippy zippy) {
        submit(activeContext, zippy);
    }
    
    public void flush() {
        activeContext = null;
    }
    
    public void beginGuiFrame() {
        preparedZippy.clear();
        paramsDirty = false;
    }
    
    public boolean isZippyPipeline(RenderPipeline pipeline) {
        return pipeline == ZIPPY_PIPELINE;
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedZippy.isEmpty()) {
            return;
        }
        
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("ZippyParamsArray", buffer);
        }
    }
    
    public void prepareBuffers() {
        if (preparedZippy.isEmpty() || !paramsDirty) {
            return;
        }
        
        GpuBuffer buffer = ensureWritableParamsBuffer();
        if (buffer == null) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer uniformData = buildUniformData(stack, preparedZippy);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(buffer.slice(0, uniformData.remaining()), uniformData);
            paramsDirty = false;
        } catch (RuntimeException ignored) {
            paramsDirty = true;
        }
    }
    
    int reserve(BuiltZippy zippy) {
        int index = preparedZippy.size();
        if (index == MAX_ZIPPY) {
            return -1;
        }
        
        preparedZippy.add(zippy);
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltZippy zippy) {
        if (context == null || zippy == null || !zippy.visible()) {
            return;
        }
        
        try {
            BuiltZippy normalized = normalize(zippy);
            Matrix3x2f pose = Render2D.pose(context);
            ((DrawContextAccessor) context)
                .meteor$getState()
                .addSimpleElement(new ZippyRenderState(pose, normalized));
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
            ByteBuffer uniformData = buildUniformData(stack, preparedZippy);
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_zippy_params", GpuBuffer.USAGE_UNIFORM, uniformData);
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
                () -> "meteor_zippy_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltZippy> batch) {
        int usedBytes = Math.max(1, batch.size()) * PARAMS_PER_ZIPPY * FLOATS_PER_PARAM * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        float time = (System.currentTimeMillis() - MeteorClient.initTime) / 1000f;
        
        for (int i = 0; i < batch.size(); i++) {
            BuiltZippy zippy = batch.get(i);
            int offset = i * PARAMS_PER_ZIPPY * FLOATS_PER_PARAM * Float.BYTES;
            
            data.putFloat(offset, zippy.radiusTopLeft());
            data.putFloat(offset + 4, zippy.radiusTopRight());
            data.putFloat(offset + 8, zippy.radiusBottomRight());
            data.putFloat(offset + 12, zippy.radiusBottomLeft());
            
            data.putFloat(offset + 16, zippy.width());
            data.putFloat(offset + 20, zippy.height());
            data.putFloat(offset + 24, zippy.smoothness());
            data.putFloat(offset + 28, time + zippy.timeOffset());
            
            putColor(data, offset + 32, zippy.color());
        }
        
        data.position(0);
        return data;
    }
    
    private BuiltZippy normalize(BuiltZippy zippy) {
        float maxRadius = Math.max(0.0f, Math.min(zippy.width(), zippy.height()) * 0.5f);
        float radiusTopLeft = clamp(zippy.radiusTopLeft(), 0.0f, maxRadius);
        float radiusTopRight = clamp(zippy.radiusTopRight(), 0.0f, maxRadius);
        float radiusBottomRight = clamp(zippy.radiusBottomRight(), 0.0f, maxRadius);
        float radiusBottomLeft = clamp(zippy.radiusBottomLeft(), 0.0f, maxRadius);
        float smoothness = Math.max(zippy.smoothness(), 0.5f);
        
        if (radiusTopLeft == zippy.radiusTopLeft()
            && radiusTopRight == zippy.radiusTopRight()
            && radiusBottomRight == zippy.radiusBottomRight()
            && radiusBottomLeft == zippy.radiusBottomLeft()
            && smoothness == zippy.smoothness()) {
            return zippy;
        }
        
        return new BuiltZippy(
            zippy.x(),
            zippy.y(),
            zippy.width(),
            zippy.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            zippy.color(),
            smoothness,
            zippy.timeOffset()
        );
    }
    
    private static void putColor(ByteBuffer data, int offset, int color) {
        data.putFloat(offset, ((color >>> 16) & 0xFF) / 255.0f);
        data.putFloat(offset + 4, ((color >>> 8) & 0xFF) / 255.0f);
        data.putFloat(offset + 8, (color & 0xFF) / 255.0f);
        data.putFloat(offset + 12, ((color >>> 24) & 0xFF) / 255.0f);
    }
    
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    private void closeParamsBuffer() {
        if (paramsBuffer != null) {
            paramsBuffer.close();
            paramsBuffer = null;
        }
    }
    
    @Override
    public void close() {
        preparedZippy.clear();
        activeContext = null;
        closeParamsBuffer();
    }
    
}
