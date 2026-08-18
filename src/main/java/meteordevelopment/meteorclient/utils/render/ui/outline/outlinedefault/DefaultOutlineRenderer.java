package meteordevelopment.meteorclient.utils.render.ui.outline.outlinedefault;

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

public final class DefaultOutlineRenderer implements AutoCloseable {
    
    private static final int MAX_OUTLINES = 512;
    private static final int PARAMS_PER_OUTLINE = 6;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = MAX_OUTLINES * PARAMS_PER_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile DefaultOutlineRenderer instance;
    
    public static final RenderPipeline OUTLINE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/outline_default"))
        .withVertexShader(MeteorClient.identifier("ui/shared/outline_quad"))
        .withFragmentShader(MeteorClient.identifier("ui/outline_default/outline_default"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("OutlineParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<BuiltOutline> preparedOutlines = new ArrayList<>(128);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    
    private DefaultOutlineRenderer() {}
    
    public static DefaultOutlineRenderer getInstance() {
        DefaultOutlineRenderer local = instance;
        if (local == null) {
            synchronized (DefaultOutlineRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new DefaultOutlineRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        DefaultOutlineRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        activeContext = context;
    }
    
    public void draw(DrawContext context, BuiltOutline outline) {
        beginFrame(context);
        enqueue(outline);
        flush();
    }
    
    public void enqueue(BuiltOutline outline) {
        submit(activeContext, outline);
    }
    
    public void flush() {
        activeContext = null;
    }
    
    public void beginGuiFrame() {
        preparedOutlines.clear();
        paramsDirty = false;
    }
    
    public boolean isOutlinePipeline(RenderPipeline pipeline) {
        return pipeline == OUTLINE_PIPELINE;
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedOutlines.isEmpty()) {
            return;
        }
        
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("OutlineParamsArray", buffer);
        }
    }
    
    public void prepareBuffers() {
        if (preparedOutlines.isEmpty() || !paramsDirty) {
            return;
        }
        
        GpuBuffer buffer = ensureWritableParamsBuffer();
        if (buffer == null) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer uniformData = buildUniformData(stack, preparedOutlines);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(buffer.slice(0, uniformData.remaining()), uniformData);
            paramsDirty = false;
        } catch (RuntimeException ignored) {
            paramsDirty = true;
        }
    }
    
    int reserve(BuiltOutline outline) {
        int index = preparedOutlines.size();
        if (index == MAX_OUTLINES) {
            return -1;
        }
        
        preparedOutlines.add(outline);
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltOutline outline) {
        if (context == null || outline == null || !outline.visible()) {
            return;
        }
        
        try {
            BuiltOutline normalized = normalize(outline);
            Matrix3x2f pose = Render2D.pose(context);
            ((DrawContextAccessor) context)
                .meteor$getState()
                .addSimpleElement(new DefaultOutlineRenderState(pose, normalized, ScissorUtil.current()));
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
            ByteBuffer uniformData = buildUniformData(stack, preparedOutlines);
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_outline_default_params", GpuBuffer.USAGE_UNIFORM, uniformData);
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
                () -> "meteor_outline_default_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltOutline> batch) {
        int usedBytes = Math.max(1, batch.size()) * PARAMS_PER_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        
        for (int i = 0; i < batch.size(); i++) {
            BuiltOutline outline = batch.get(i);
            int offset = i * PARAMS_PER_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
            
            data.putFloat(offset, outline.radiusTopLeft());
            data.putFloat(offset + 4, outline.radiusTopRight());
            data.putFloat(offset + 8, outline.radiusBottomRight());
            data.putFloat(offset + 12, outline.radiusBottomLeft());
            
            data.putFloat(offset + 16, outline.width());
            data.putFloat(offset + 20, outline.height());
            data.putFloat(offset + 24, outline.thickness());
            data.putFloat(offset + 28, outline.smoothness());
            
            putColor(data, offset + 32, outline.colorTopLeft());
            putColor(data, offset + 48, outline.colorTopRight());
            putColor(data, offset + 64, outline.colorBottomRight());
            putColor(data, offset + 80, outline.colorBottomLeft());
        }
        
        data.position(0);
        return data;
    }
    
    private BuiltOutline normalize(BuiltOutline outline) {
        float maxRadius = Math.max(0.0f, Math.min(outline.width(), outline.height()) * 0.5f);
        float maxThickness = Math.max(0.0f, Math.min(outline.width(), outline.height()) * 0.5f);
        float radiusTopLeft = clamp(outline.radiusTopLeft(), 0.0f, maxRadius);
        float radiusTopRight = clamp(outline.radiusTopRight(), 0.0f, maxRadius);
        float radiusBottomRight = clamp(outline.radiusBottomRight(), 0.0f, maxRadius);
        float radiusBottomLeft = clamp(outline.radiusBottomLeft(), 0.0f, maxRadius);
        float thickness = clamp(outline.thickness(), 0.0f, maxThickness);
        float smoothness = Math.max(outline.smoothness(), 0.5f);
        
        if (radiusTopLeft == outline.radiusTopLeft()
            && radiusTopRight == outline.radiusTopRight()
            && radiusBottomRight == outline.radiusBottomRight()
            && radiusBottomLeft == outline.radiusBottomLeft()
            && thickness == outline.thickness()
            && smoothness == outline.smoothness()) {
            return outline;
        }
        
        return new BuiltOutline(
            outline.x(),
            outline.y(),
            outline.width(),
            outline.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            outline.colorTopLeft(),
            outline.colorTopRight(),
            outline.colorBottomRight(),
            outline.colorBottomLeft(),
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
    
    private static Identifier id(String path) {
        return Identifier.of("meteor", path);
    }
    
    @Override
    public void close() {
        preparedOutlines.clear();
        activeContext = null;
        closeParamsBuffer();
    }
    
}
