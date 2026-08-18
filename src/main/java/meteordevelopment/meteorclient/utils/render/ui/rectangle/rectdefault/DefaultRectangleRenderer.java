package meteordevelopment.meteorclient.utils.render.ui.rectangle.rectdefault;

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

public final class DefaultRectangleRenderer implements AutoCloseable {
    
    private static final int MAX_RECTANGLES = 512;
    private static final int PARAMS_PER_RECTANGLE = 6;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = MAX_RECTANGLES * PARAMS_PER_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile DefaultRectangleRenderer instance;
    
    public static final RenderPipeline RECTANGLE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/rect_default"))
        .withVertexShader(MeteorClient.identifier("ui/rect_default/rect_default"))
        .withFragmentShader(MeteorClient.identifier("ui/rect_default/rect_default"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("RectangleParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<BuiltRectangle> preparedRectangles = new ArrayList<>(128);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    
    private DefaultRectangleRenderer() {}
    
    public static DefaultRectangleRenderer getInstance() {
        DefaultRectangleRenderer local = instance;
        if (local == null) {
            synchronized (DefaultRectangleRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new DefaultRectangleRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        DefaultRectangleRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        activeContext = context;
    }
    
    public void draw(DrawContext context, BuiltRectangle rectangle) {
        beginFrame(context);
        enqueue(rectangle);
        flush();
    }
    
    public void enqueue(BuiltRectangle rectangle) {
        submit(activeContext, rectangle);
    }
    
    public void flush() {
        activeContext = null;
    }
    
    public void beginGuiFrame() {
        preparedRectangles.clear();
        paramsDirty = false;
    }
    
    public boolean isRectanglePipeline(RenderPipeline pipeline) {
        return pipeline != null && RECTANGLE_PIPELINE.getLocation().equals(pipeline.getLocation());
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedRectangles.isEmpty()) {
            return;
        }
        
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("RectangleParamsArray", buffer);
        }
    }
    
    public void prepareBuffers() {
        if (preparedRectangles.isEmpty() || !paramsDirty) {
            return;
        }
        
        GpuBuffer buffer = ensureWritableParamsBuffer();
        if (buffer == null) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer uniformData = buildUniformData(stack, preparedRectangles);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(buffer.slice(0, uniformData.remaining()), uniformData);
            paramsDirty = false;
        } catch (RuntimeException ignored) {
            paramsDirty = true;
        }
    }
    
    int reserve(BuiltRectangle rectangle) {
        int index = preparedRectangles.size();
        if (index == MAX_RECTANGLES) {
            return -1;
        }
        preparedRectangles.add(rectangle);
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltRectangle rectangle) {
        if (context == null || rectangle == null || !rectangle.visible()) {
            return;
        }
        
        try {
            BuiltRectangle normalized = normalize(rectangle);
            Matrix3x2f pose = Render2D.pose(context);
            ((DrawContextAccessor) context)
                .meteor$getState()
                .addSimpleElement(new DefaultRectangleRenderState(pose, normalized, ScissorUtil.current()));
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
            ByteBuffer uniformData = buildUniformData(stack, preparedRectangles);
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_rect_default_params", GpuBuffer.USAGE_UNIFORM, uniformData);
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
                () -> "meteor_rect_default_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltRectangle> batch) {
        int usedBytes = Math.max(1, batch.size()) * PARAMS_PER_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        
        for (int i = 0; i < batch.size(); i++) {
            BuiltRectangle rectangle = batch.get(i);
            int offset = i * PARAMS_PER_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
            
            data.putFloat(offset, rectangle.radiusTopLeft());
            data.putFloat(offset + 4, rectangle.radiusTopRight());
            data.putFloat(offset + 8, rectangle.radiusBottomRight());
            data.putFloat(offset + 12, rectangle.radiusBottomLeft());
            
            data.putFloat(offset + 16, rectangle.width());
            data.putFloat(offset + 20, rectangle.height());
            data.putFloat(offset + 24, rectangle.smoothness());
            data.putFloat(offset + 28, 0.0f);
            
            putColor(data, offset + 32, rectangle.colorTopLeft());
            putColor(data, offset + 48, rectangle.colorTopRight());
            putColor(data, offset + 64, rectangle.colorBottomRight());
            putColor(data, offset + 80, rectangle.colorBottomLeft());
        }
        
        data.position(0);
        return data;
    }
    
    private BuiltRectangle normalize(BuiltRectangle rectangle) {
        float maxRadius = Math.max(0.0f, Math.min(rectangle.width(), rectangle.height()) * 0.5f);
        float radiusTopLeft = clamp(rectangle.radiusTopLeft(), 0.0f, maxRadius);
        float radiusTopRight = clamp(rectangle.radiusTopRight(), 0.0f, maxRadius);
        float radiusBottomRight = clamp(rectangle.radiusBottomRight(), 0.0f, maxRadius);
        float radiusBottomLeft = clamp(rectangle.radiusBottomLeft(), 0.0f, maxRadius);
        float smoothness = Math.max(rectangle.smoothness(), 0.5f);
        
        if (radiusTopLeft == rectangle.radiusTopLeft()
            && radiusTopRight == rectangle.radiusTopRight()
            && radiusBottomRight == rectangle.radiusBottomRight()
            && radiusBottomLeft == rectangle.radiusBottomLeft()
            && smoothness == rectangle.smoothness()) {
            return rectangle;
        }
        
        return new BuiltRectangle(
            rectangle.x(),
            rectangle.y(),
            rectangle.width(),
            rectangle.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            rectangle.colorTopLeft(),
            rectangle.colorTopRight(),
            rectangle.colorBottomRight(),
            rectangle.colorBottomLeft(),
            smoothness
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
        preparedRectangles.clear();
        activeContext = null;
        closeParamsBuffer();
    }
    
}
