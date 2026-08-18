package meteordevelopment.meteorclient.utils.render.ui.outline.outline360;

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

public final class Outline360Renderer implements AutoCloseable {
    
    private static final int MAX_OUTLINES = 256;
    private static final int MAX_RANGES = 1024;
    private static final int PARAMS_PER_OUTLINE = 4;
    private static final int PARAMS_PER_RANGE = 2;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int OUTLINE_UNIFORM_BYTES = MAX_OUTLINES * PARAMS_PER_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
    private static final int RANGE_UNIFORM_BYTES = MAX_RANGES * PARAMS_PER_RANGE * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile Outline360Renderer instance;
    
    public static final RenderPipeline OUTLINE_360_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/outline_360"))
        .withVertexShader(MeteorClient.identifier("ui/shared/outline_quad"))
        .withFragmentShader(MeteorClient.identifier("ui/outline_360/outline_360"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("Outline360ParamsArray", UniformType.UNIFORM_BUFFER)
        .withUniform("Outline360RangesArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<BuiltOutline360> preparedOutlines = new ArrayList<>(64);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private GpuBuffer rangesBuffer;
    private boolean paramsDirty = true;
    
    private Outline360Renderer() {
    }
    
    public static Outline360Renderer getInstance() {
        Outline360Renderer local = instance;
        if (local == null) {
            synchronized (Outline360Renderer.class) {
                local = instance;
                if (local == null) {
                    local = new Outline360Renderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        Outline360Renderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        activeContext = context;
    }
    
    public void draw(DrawContext context, BuiltOutline360 outline) {
        beginFrame(context);
        enqueue(outline);
        flush();
    }
    
    public void enqueue(BuiltOutline360 outline) {
        submit(activeContext, outline);
    }
    
    public void flush() {
        activeContext = null;
    }
    
    public void beginGuiFrame() {
        preparedOutlines.clear();
        paramsDirty = false;
    }
    
    public boolean isOutline360Pipeline(RenderPipeline pipeline) {
        return pipeline == OUTLINE_360_PIPELINE;
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedOutlines.isEmpty()) {
            return;
        }
        
        ensureBuffers();
        if (paramsBuffer != null) {
            renderPass.setUniform("Outline360ParamsArray", paramsBuffer);
        }
        if (rangesBuffer != null) {
            renderPass.setUniform("Outline360RangesArray", rangesBuffer);
        }
    }
    
    public void prepareBuffers() {
        if (preparedOutlines.isEmpty() || !paramsDirty) {
            return;
        }
        
        if (!ensureWritableBuffers()) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer paramsData = stack.calloc(outlineUniformBytes(preparedOutlines));
            ByteBuffer rangesData = stack.calloc(rangeUniformBytes(preparedOutlines));
            buildUniformData(paramsData, rangesData, preparedOutlines);
            
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(paramsBuffer.slice(0, paramsData.remaining()), paramsData);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(rangesBuffer.slice(0, rangesData.remaining()), rangesData);
            paramsDirty = false;
        } catch (RuntimeException ignored) {
            paramsDirty = true;
        }
    }
    
    int reserve(BuiltOutline360 outline) {
        int index = preparedOutlines.size();
        if (index == MAX_OUTLINES) {
            return -1;
        }
        
        preparedOutlines.add(outline);
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltOutline360 outline) {
        if (context == null || outline == null || !outline.visible()) {
            return;
        }
        
        try {
            BuiltOutline360 normalized = normalize(outline);
            Matrix3x2f pose = Render2D.pose(context);
            ((DrawContextAccessor) context)
                .meteor$getState()
                .addSimpleElement(new Outline360RenderState(pose, normalized));
        } catch (RuntimeException ignored) {
        }
    }
    
    private void ensureBuffers() {
        if (!paramsDirty && paramsBuffer != null && rangesBuffer != null) {
            return;
        }
        
        prepareBuffers();
        if (!paramsDirty && paramsBuffer != null && rangesBuffer != null) {
            return;
        }
        
        closeBuffers();
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer paramsData = stack.calloc(outlineUniformBytes(preparedOutlines));
            ByteBuffer rangesData = stack.calloc(rangeUniformBytes(preparedOutlines));
            buildUniformData(paramsData, rangesData, preparedOutlines);
            
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_outline_360_params",
                GpuBuffer.USAGE_UNIFORM,
                paramsData
            );
            rangesBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_outline_360_ranges",
                GpuBuffer.USAGE_UNIFORM,
                rangesData
            );
            paramsDirty = false;
        }
    }
    
    private boolean ensureWritableBuffers() {
        boolean paramsReady = paramsBuffer != null && !paramsBuffer.isClosed() && paramsBuffer.size() >= OUTLINE_UNIFORM_BYTES;
        boolean rangesReady = rangesBuffer != null && !rangesBuffer.isClosed() && rangesBuffer.size() >= RANGE_UNIFORM_BYTES;
        if (paramsReady && rangesReady) {
            return true;
        }
        
        closeBuffers();
        
        try {
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_outline_360_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                OUTLINE_UNIFORM_BYTES
            );
            rangesBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_outline_360_ranges",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                RANGE_UNIFORM_BYTES
            );
            return true;
        } catch (RuntimeException ignored) {
            closeBuffers();
            return false;
        }
    }
    
    private int outlineUniformBytes(List<BuiltOutline360> batch) {
        return Math.max(1, batch.size()) * PARAMS_PER_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
    }
    
    private int rangeUniformBytes(List<BuiltOutline360> batch) {
        int ranges = 0;
        for (BuiltOutline360 outline : batch) {
            ranges += Math.min(outline.ranges().size(), MAX_RANGES - ranges);
            if (ranges == MAX_RANGES) {
                break;
            }
        }
        return Math.max(1, ranges) * PARAMS_PER_RANGE * FLOATS_PER_PARAM * Float.BYTES;
    }
    
    private void buildUniformData(ByteBuffer paramsData, ByteBuffer rangesData, List<BuiltOutline360> batch) {
        int rangeOffset = 0;
        
        for (int i = 0; i < batch.size(); i++) {
            BuiltOutline360 outline = batch.get(i);
            int outlineOffset = i * PARAMS_PER_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
            int rangeCount = Math.min(outline.ranges().size(), MAX_RANGES - rangeOffset);
            
            paramsData.putFloat(outlineOffset, outline.radiusTopLeft());
            paramsData.putFloat(outlineOffset + 4, outline.radiusTopRight());
            paramsData.putFloat(outlineOffset + 8, outline.radiusBottomRight());
            paramsData.putFloat(outlineOffset + 12, outline.radiusBottomLeft());
            
            paramsData.putFloat(outlineOffset + 16, outline.width());
            paramsData.putFloat(outlineOffset + 20, outline.height());
            paramsData.putFloat(outlineOffset + 24, outline.thickness());
            paramsData.putFloat(outlineOffset + 28, outline.smoothness());
            
            putColor(paramsData, outlineOffset + 32, outline.defaultColor());
            
            paramsData.putFloat(outlineOffset + 48, rangeOffset);
            paramsData.putFloat(outlineOffset + 52, rangeCount);
            paramsData.putFloat(outlineOffset + 56, outline.blendDegrees());
            paramsData.putFloat(outlineOffset + 60, outline.angleOffsetDegrees());
            
            for (int rangeIndex = 0; rangeIndex < rangeCount; rangeIndex++) {
                Outline360Range range = outline.ranges().get(rangeIndex);
                int offset = (rangeOffset + rangeIndex) * PARAMS_PER_RANGE * FLOATS_PER_PARAM * Float.BYTES;
                rangesData.putFloat(offset, range.startDegrees());
                rangesData.putFloat(offset + 4, range.endDegrees());
                rangesData.putFloat(offset + 8, 0.0f);
                rangesData.putFloat(offset + 12, 0.0f);
                putColor(rangesData, offset + 16, range.color());
            }
            
            rangeOffset += rangeCount;
        }
        
        paramsData.position(0);
        rangesData.position(0);
    }
    
    private BuiltOutline360 normalize(BuiltOutline360 outline) {
        float maxRadius = Math.max(0.0f, Math.min(outline.width(), outline.height()) * 0.5f);
        float maxThickness = Math.max(0.0f, Math.min(outline.width(), outline.height()) * 0.5f);
        float radiusTopLeft = clamp(outline.radiusTopLeft(), 0.0f, maxRadius);
        float radiusTopRight = clamp(outline.radiusTopRight(), 0.0f, maxRadius);
        float radiusBottomRight = clamp(outline.radiusBottomRight(), 0.0f, maxRadius);
        float radiusBottomLeft = clamp(outline.radiusBottomLeft(), 0.0f, maxRadius);
        float thickness = clamp(outline.thickness(), 0.0f, maxThickness);
        float smoothness = Math.max(outline.smoothness(), 0.5f);
        float blendDegrees = Math.max(outline.blendDegrees(), 0.0f);
        
        if (radiusTopLeft == outline.radiusTopLeft()
            && radiusTopRight == outline.radiusTopRight()
            && radiusBottomRight == outline.radiusBottomRight()
            && radiusBottomLeft == outline.radiusBottomLeft()
            && thickness == outline.thickness()
            && smoothness == outline.smoothness()
            && blendDegrees == outline.blendDegrees()) {
            return outline;
        }
        
        return new BuiltOutline360(
            outline.x(),
            outline.y(),
            outline.width(),
            outline.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            outline.defaultColor(),
            smoothness,
            blendDegrees,
            outline.angleOffsetDegrees(),
            outline.ranges()
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
    
    private void closeBuffers() {
        if (paramsBuffer != null) {
            paramsBuffer.close();
            paramsBuffer = null;
        }
        if (rangesBuffer != null) {
            rangesBuffer.close();
            rangesBuffer = null;
        }
    }
    
    private static Identifier id(String path) {
        return Identifier.of("meteor", path);
    }
    
    @Override
    public void close() {
        preparedOutlines.clear();
        activeContext = null;
        closeBuffers();
    }
    
}
