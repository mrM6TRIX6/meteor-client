package meteordevelopment.meteorclient.utils.render.ui.rectangle.rectrotatinggradient;

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

public final class RotatingGradientRectangleRenderer implements AutoCloseable {
    
    private static final int MAX_ROTATING_GRADIENT_RECTANGLES = 512;
    private static final int PARAMS_PER_ROTATING_GRADIENT_RECTANGLE = 7;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = MAX_ROTATING_GRADIENT_RECTANGLES * PARAMS_PER_ROTATING_GRADIENT_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile RotatingGradientRectangleRenderer instance;
    
    public static final RenderPipeline ROTATING_GRADIENT_RECTANGLE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/rect_rotating_gradient"))
        .withVertexShader(MeteorClient.identifier("ui/rect_rotating_gradient/rect_rotating_gradient"))
        .withFragmentShader(MeteorClient.identifier("ui/rect_rotating_gradient/rect_rotating_gradient"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("RotatingGradientRectangleParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<BuiltRotatingGradientRectangle> preparedRotatingGradientRectangles = new ArrayList<>(128);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    
    private RotatingGradientRectangleRenderer() {}
    
    public static RotatingGradientRectangleRenderer getInstance() {
        RotatingGradientRectangleRenderer local = instance;
        if (local == null) {
            synchronized (RotatingGradientRectangleRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new RotatingGradientRectangleRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        RotatingGradientRectangleRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        activeContext = context;
    }
    
    public void draw(DrawContext context, BuiltRotatingGradientRectangle built) {
        beginFrame(context);
        enqueue(built);
        flush();
    }
    
    public void enqueue(BuiltRotatingGradientRectangle built) {
        submit(activeContext, built);
    }
    
    public void flush() {
        activeContext = null;
    }
    
    public void beginGuiFrame() {
        preparedRotatingGradientRectangles.clear();
        paramsDirty = false;
    }
    
    public boolean isRotatingGradientRectanglePipeline(RenderPipeline pipeline) {
        return pipeline != null && ROTATING_GRADIENT_RECTANGLE_PIPELINE.getLocation().equals(pipeline.getLocation());
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedRotatingGradientRectangles.isEmpty()) {
            return;
        }
        
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("RotatingGradientRectangleParamsArray", buffer);
        }
    }
    
    public void prepareBuffers() {
        if (preparedRotatingGradientRectangles.isEmpty() || !paramsDirty) {
            return;
        }
        
        GpuBuffer buffer = ensureWritableParamsBuffer();
        if (buffer == null) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer uniformData = buildUniformData(stack, preparedRotatingGradientRectangles);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(buffer.slice(0, uniformData.remaining()), uniformData);
            paramsDirty = false;
        } catch (RuntimeException ignored) {
            paramsDirty = true;
        }
    }
    
    int reserve(BuiltRotatingGradientRectangle built) {
        int index = preparedRotatingGradientRectangles.size();
        if (index == MAX_ROTATING_GRADIENT_RECTANGLES) {
            return -1;
        }
        preparedRotatingGradientRectangles.add(built);
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltRotatingGradientRectangle built) {
        if (context == null || built == null || !built.visible()) {
            return;
        }
        
        try {
            BuiltRotatingGradientRectangle normalized = normalize(built);
            Matrix3x2f pose = Render2D.pose(context);
            ((DrawContextAccessor) context)
                .meteor$getState()
                .addSimpleElement(new RotatingGradientRectangleRenderState(pose, normalized, ScissorUtil.current()));
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
            ByteBuffer uniformData = buildUniformData(stack, preparedRotatingGradientRectangles);
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_rect_rotating_gradient_params",
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
                () -> "meteor_rect_rotating_gradient_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltRotatingGradientRectangle> batch) {
        int usedBytes = Math.max(1, batch.size()) * PARAMS_PER_ROTATING_GRADIENT_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        
        float time = (System.currentTimeMillis() - MeteorClient.initTime) / 1000f;
        
        for (int i = 0; i < batch.size(); i++) {
            BuiltRotatingGradientRectangle r = batch.get(i);
            int offset = i * PARAMS_PER_ROTATING_GRADIENT_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
            
            data.putFloat(offset,      r.radiusTopLeft());
            data.putFloat(offset + 4,  r.radiusTopRight());
            data.putFloat(offset + 8,  r.radiusBottomRight());
            data.putFloat(offset + 12, r.radiusBottomLeft());
            
            data.putFloat(offset + 16, r.width());
            data.putFloat(offset + 20, r.height());
            data.putFloat(offset + 24, time);
            data.putFloat(offset + 28, 0.0f);
            
            data.putFloat(offset + 32, r.smoothness());
            data.putFloat(offset + 36, r.speed());
            data.putFloat(offset + 40, 0.0f);
            data.putFloat(offset + 44, 0.0f);
            
            putColor(data, offset + 48,  r.firstColor());
            putColor(data, offset + 64,  r.secondColor());
            putColor(data, offset + 80,  r.thirdColor());
            putColor(data, offset + 96,  r.fourthColor());
        }
        
        data.position(0);
        return data;
    }
    
    private BuiltRotatingGradientRectangle normalize(BuiltRotatingGradientRectangle built) {
        float maxRadius = Math.max(0.0f, Math.min(built.width(), built.height()) * 0.5f);
        
        float radiusTopLeft = clamp(built.radiusTopLeft(), 0.0f, maxRadius);
        float radiusTopRight = clamp(built.radiusTopRight(), 0.0f, maxRadius);
        float radiusBottomRight = clamp(built.radiusBottomRight(), 0.0f, maxRadius);
        float radiusBottomLeft = clamp(built.radiusBottomLeft(), 0.0f, maxRadius);
        
        float smoothness = Math.max(built.smoothness(), 0.5f);
        
        if (radiusTopLeft == built.radiusTopLeft()
            && radiusTopRight == built.radiusTopRight()
            && radiusBottomRight == built.radiusBottomRight()
            && radiusBottomLeft == built.radiusBottomLeft()
            && smoothness == built.smoothness()) {
            return built;
        }
        
        return new BuiltRotatingGradientRectangle(
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
            built.thirdColor(),
            built.fourthColor(),
            smoothness,
            built.speed()
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
        preparedRotatingGradientRectangles.clear();
        activeContext = null;
        closeParamsBuffer();
    }
    
}