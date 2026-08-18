package meteordevelopment.meteorclient.utils.render.ui.rectangle.recthalftone;

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
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class HalftoneRectangleRenderer implements AutoCloseable {
    
    private static final int MAX_RECTANGLES = 384;
    private static final int PARAMS_PER_RECTANGLE = 8;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = MAX_RECTANGLES * PARAMS_PER_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile HalftoneRectangleRenderer instance;
    
    public static final RenderPipeline HALFTONE_RECTANGLE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/rect_halftone"))
        .withVertexShader(MeteorClient.identifier("ui/rect_halftone/rect_halftone"))
        .withFragmentShader(MeteorClient.identifier("ui/rect_halftone/rect_halftone"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("HalftoneRectangleParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<BuiltHalftoneRectangle> preparedRectangles = new ArrayList<>(64);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    
    private HalftoneRectangleRenderer() {
    }
    
    public static HalftoneRectangleRenderer getInstance() {
        HalftoneRectangleRenderer local = instance;
        if (local == null) {
            synchronized (HalftoneRectangleRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new HalftoneRectangleRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        HalftoneRectangleRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        activeContext = context;
    }
    
    public void draw(DrawContext context, BuiltHalftoneRectangle rectangle) {
        beginFrame(context);
        enqueue(rectangle);
        flush();
    }
    
    public void enqueue(BuiltHalftoneRectangle rectangle) {
        submit(activeContext, rectangle);
    }
    
    public void flush() {
        activeContext = null;
    }
    
    public void beginGuiFrame() {
        preparedRectangles.clear();
        paramsDirty = false;
    }
    
    public boolean isHalftoneRectanglePipeline(RenderPipeline pipeline) {
        return pipeline == HALFTONE_RECTANGLE_PIPELINE;
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedRectangles.isEmpty()) {
            return;
        }
        
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("HalftoneRectangleParamsArray", buffer);
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
    
    int reserve(BuiltHalftoneRectangle rectangle) {
        int index = preparedRectangles.size();
        if (index == MAX_RECTANGLES) {
            return -1;
        }
        
        preparedRectangles.add(rectangle);
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltHalftoneRectangle rectangle) {
        if (context == null || rectangle == null || !rectangle.visible()) {
            return;
        }
        
        try {
            BuiltHalftoneRectangle normalized = normalize(rectangle);
            Matrix3x2f pose = Render2D.pose(context);
            ((DrawContextAccessor) context)
                .meteor$getState()
                .addSimpleElement(new HalftoneRectangleRenderState(pose, normalized));
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
            ByteBuffer uniformData = buildUniformData(stack, preparedRectangles);
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_rect_halftone_params", GpuBuffer.USAGE_UNIFORM, uniformData);
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
                () -> "meteor_rect_halftone_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltHalftoneRectangle> batch) {
        int usedBytes = Math.max(1, batch.size()) * PARAMS_PER_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        
        for (int i = 0; i < batch.size(); i++) {
            BuiltHalftoneRectangle rectangle = batch.get(i);
            int offset = i * PARAMS_PER_RECTANGLE * FLOATS_PER_PARAM * Float.BYTES;
            
            data.putFloat(offset, rectangle.radiusTopLeft());
            data.putFloat(offset + 4, rectangle.radiusTopRight());
            data.putFloat(offset + 8, rectangle.radiusBottomRight());
            data.putFloat(offset + 12, rectangle.radiusBottomLeft());
            
            data.putFloat(offset + 16, rectangle.width());
            data.putFloat(offset + 20, rectangle.height());
            data.putFloat(offset + 24, rectangle.smoothness());
            data.putFloat(offset + 28, rectangle.dotSize());
            
            putColor(data, offset + 32, rectangle.colorTopLeft());
            putColor(data, offset + 48, rectangle.colorTopRight());
            putColor(data, offset + 64, rectangle.colorBottomRight());
            putColor(data, offset + 80, rectangle.colorBottomLeft());
            putColor(data, offset + 96, rectangle.dotColor());
            
            data.putFloat(offset + 112, rectangle.dotSpacing());
            data.putFloat(offset + 116, 0.0f);
            data.putFloat(offset + 120, 0.0f);
            data.putFloat(offset + 124, 0.0f);
        }
        
        data.position(0);
        return data;
    }
    
    private BuiltHalftoneRectangle normalize(BuiltHalftoneRectangle rectangle) {
        float maxRadius = Math.max(0.0f, Math.min(rectangle.width(), rectangle.height()) * 0.5f);
        float maxDotSize = Math.max(Math.min(rectangle.width(), rectangle.height()), 0.0f);
        float radiusTopLeft = clamp(rectangle.radiusTopLeft(), 0.0f, maxRadius);
        float radiusTopRight = clamp(rectangle.radiusTopRight(), 0.0f, maxRadius);
        float radiusBottomRight = clamp(rectangle.radiusBottomRight(), 0.0f, maxRadius);
        float radiusBottomLeft = clamp(rectangle.radiusBottomLeft(), 0.0f, maxRadius);
        float smoothness = Math.max(rectangle.smoothness(), 0.5f);
        float dotSize = clamp(rectangle.dotSize(), 0.0f, maxDotSize);
        float dotSpacing = Math.max(rectangle.dotSpacing(), 0.0f);
        
        if (radiusTopLeft == rectangle.radiusTopLeft()
            && radiusTopRight == rectangle.radiusTopRight()
            && radiusBottomRight == rectangle.radiusBottomRight()
            && radiusBottomLeft == rectangle.radiusBottomLeft()
            && smoothness == rectangle.smoothness()
            && dotSize == rectangle.dotSize()
            && dotSpacing == rectangle.dotSpacing()) {
            return rectangle;
        }
        
        return new BuiltHalftoneRectangle(
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
            smoothness,
            rectangle.dotColor(),
            dotSize,
            dotSpacing
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
    
    private static Identifier id(String path) {
        return Identifier.of("meteor", path);
    }
    
    @Override
    public void close() {
        preparedRectangles.clear();
        activeContext = null;
        closeParamsBuffer();
    }
    
}
