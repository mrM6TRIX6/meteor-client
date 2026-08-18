package meteordevelopment.meteorclient.utils.render.ui.rectangle.rectgradient;

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
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class GradientRectangleRenderer implements AutoCloseable {
    
    private static final int MAX_GRADIENT_RECTANGLES = 512;
    private static final int PARAMS_PER_GRADIENT_RECTANGLE = 5;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = MAX_GRADIENT_RECTANGLES * PARAMS_PER_GRADIENT_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile GradientRectangleRenderer instance;
    
    public static final RenderPipeline GRADIENT_RECTANGLE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/rect_gradient"))
        .withVertexShader(MeteorClient.identifier("ui/rect_gradient/rect_gradient"))
        .withFragmentShader(MeteorClient.identifier("ui/rect_gradient/rect_gradient"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("GradientRectangleParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<BuiltGradientRectangle> preparedGradientRectangles = new ArrayList<>(128);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    
    private GradientRectangleRenderer() {}
    
    public static GradientRectangleRenderer getInstance() {
        GradientRectangleRenderer local = instance;
        if (local == null) {
            synchronized (GradientRectangleRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new GradientRectangleRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        GradientRectangleRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        activeContext = context;
    }
    
    public void draw(DrawContext context, BuiltGradientRectangle built) {
        beginFrame(context);
        enqueue(built);
        flush();
    }
    
    public void enqueue(BuiltGradientRectangle built) {
        submit(activeContext, built);
    }
    
    public void flush() {
        activeContext = null;
    }
    
    public void beginGuiFrame() {
        preparedGradientRectangles.clear();
        paramsDirty = false;
    }
    
    public boolean isGradientRectanglePipeline(RenderPipeline pipeline) {
        return pipeline != null && GRADIENT_RECTANGLE_PIPELINE.getLocation().equals(pipeline.getLocation());
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedGradientRectangles.isEmpty()) {
            return;
        }
        
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("GradientRectangleParamsArray", buffer);
        }
    }
    
    public void prepareBuffers() {
        if (preparedGradientRectangles.isEmpty() || !paramsDirty) {
            return;
        }
        
        GpuBuffer buffer = ensureWritableParamsBuffer();
        if (buffer == null) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer uniformData = buildUniformData(stack, preparedGradientRectangles);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(buffer.slice(0, uniformData.remaining()), uniformData);
            paramsDirty = false;
        } catch (RuntimeException ignored) {
            paramsDirty = true;
        }
    }
    
    int reserve(BuiltGradientRectangle built) {
        int index = preparedGradientRectangles.size();
        if (index == MAX_GRADIENT_RECTANGLES) {
            return -1;
        }
        preparedGradientRectangles.add(built);
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltGradientRectangle built) {
        if (context == null || built == null || !built.visible()) {
            return;
        }
        
        try {
            BuiltGradientRectangle normalized = normalize(built);
            Matrix3x2f pose = Render2D.pose(context);
            ((DrawContextAccessor) context)
                .meteor$getState()
                .addSimpleElement(new GradientRectangleRenderState(pose, normalized, ScissorUtil.current()));
        } catch (RuntimeException ignored) {}
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
            ByteBuffer uniformData = buildUniformData(stack, preparedGradientRectangles);
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_rect_gradient_params",
                GpuBuffer.USAGE_UNIFORM,
                uniformData
            );
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
                () -> "meteor_rect_gradient_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltGradientRectangle> batch) {
        int usedBytes = Math.max(1, batch.size()) * PARAMS_PER_GRADIENT_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        
        float time = (System.currentTimeMillis() - MeteorClient.initTime) / 1000f;
        
        for (int i = 0; i < batch.size(); i++) {
            BuiltGradientRectangle r = batch.get(i);
            int offset = i * PARAMS_PER_GRADIENT_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
            
            data.putFloat(offset, r.radiusTopLeft());
            data.putFloat(offset + 4, r.radiusTopRight());
            data.putFloat(offset + 8, r.radiusBottomRight());
            data.putFloat(offset + 12, r.radiusBottomLeft());
            
            data.putFloat(offset + 16, r.width());
            data.putFloat(offset + 20, r.height());
            data.putFloat(offset + 24, time);
            data.putFloat(offset + 28, 0.0f);
            
            data.putFloat(offset + 32, r.smoothness());
            data.putFloat(offset + 36, r.speed());
            data.putFloat(offset + 40, r.frequency());
            data.putFloat(offset + 44, r.angle());
            
            putColor(data, offset + 48, r.firstColor());
            putColor(data, offset + 64, r.secondColor());
        }
        
        data.position(0);
        return data;
    }
    
    private BuiltGradientRectangle normalize(BuiltGradientRectangle built) {
        float maxRadius = Math.max(0.0f, Math.min(built.width(), built.height()) * 0.5f);
        
        float radiusTopLeft = clamp(built.radiusTopLeft(), 0.0f, maxRadius);
        float radiusTopRight = clamp(built.radiusTopRight(), 0.0f, maxRadius);
        float radiusBottomRight = clamp(built.radiusBottomRight(), 0.0f, maxRadius);
        float radiusBottomLeft = clamp(built.radiusBottomLeft(), 0.0f, maxRadius);
        
        float smoothness = Math.max(built.smoothness(), 0.5f);
        float frequency = Math.max(built.frequency(), 0.0001f);
        
        if (radiusTopLeft == built.radiusTopLeft()
            && radiusTopRight == built.radiusTopRight()
            && radiusBottomRight == built.radiusBottomRight()
            && radiusBottomLeft == built.radiusBottomLeft()
            && smoothness == built.smoothness()
            && frequency == built.frequency()) {
            return built;
        }
        
        return new BuiltGradientRectangle(
            built.x(),
            built.y(),
            built.width(),
            built.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            built.firstColor(),
            built.secondColor(),
            smoothness,
            built.speed(),
            frequency,
            built.angle()
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
        preparedGradientRectangles.clear();
        activeContext = null;
        closeParamsBuffer();
    }
    
}