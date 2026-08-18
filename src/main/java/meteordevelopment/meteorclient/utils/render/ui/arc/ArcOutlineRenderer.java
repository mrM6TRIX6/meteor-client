package meteordevelopment.meteorclient.utils.render.ui.arc;

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

public final class ArcOutlineRenderer implements AutoCloseable {
    
    private static final int MAX_ARC_OUTLINES = 256;
    private static final int PARAMS_PER_ARC_OUTLINE = 4;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = MAX_ARC_OUTLINES * PARAMS_PER_ARC_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile ArcOutlineRenderer instance;
    
    public static final RenderPipeline ARC_OUTLINE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/arc_outline"))
        .withVertexShader(MeteorClient.identifier("ui/shared/outline_quad"))
        .withFragmentShader(MeteorClient.identifier("ui/arc/arc_outline"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("ArcOutlineParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<BuiltArcOutline> preparedOutlines = new ArrayList<>(64);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    
    private ArcOutlineRenderer() {}
    
    public static ArcOutlineRenderer getInstance() {
        ArcOutlineRenderer local = instance;
        if (local == null) {
            synchronized (ArcOutlineRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new ArcOutlineRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        ArcOutlineRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        activeContext = context;
    }
    
    public void draw(DrawContext context, BuiltArcOutline outline) {
        beginFrame(context);
        enqueue(outline);
        flush();
    }
    
    public void enqueue(BuiltArcOutline outline) {
        submit(activeContext, outline);
    }
    
    public void flush() {
        activeContext = null;
    }
    
    public void beginGuiFrame() {
        preparedOutlines.clear();
        paramsDirty = false;
    }
    
    public boolean isArcOutlinePipeline(RenderPipeline pipeline) {
        return pipeline == ARC_OUTLINE_PIPELINE;
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedOutlines.isEmpty()) {
            return;
        }
        
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("ArcOutlineParamsArray", buffer);
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
    
    int reserve(BuiltArcOutline outline) {
        int index = preparedOutlines.size();
        if (index == MAX_ARC_OUTLINES) {
            return -1;
        }
        
        preparedOutlines.add(outline);
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltArcOutline outline) {
        if (context == null || outline == null || !outline.visible()) {
            return;
        }
        
        try {
            BuiltArcOutline normalized = normalize(outline);
            Matrix3x2f pose = Render2D.pose(context);
            ((DrawContextAccessor) context)
                .meteor$getState()
                .addSimpleElement(new ArcOutlineRenderState(pose, normalized, ScissorUtil.current()));
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
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_arc_outline_params", GpuBuffer.USAGE_UNIFORM, uniformData);
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
                () -> "meteor_arc_outline_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltArcOutline> batch) {
        int usedBytes = Math.max(1, batch.size()) * PARAMS_PER_ARC_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        
        for (int i = 0; i < batch.size(); i++) {
            BuiltArcOutline outline = batch.get(i);
            int offset = i * PARAMS_PER_ARC_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
            
            data.putFloat(offset, outline.size());
            data.putFloat(offset + 4, outline.arcThickness());
            data.putFloat(offset + 8, outline.degree());
            data.putFloat(offset + 12, outline.rotation());
            
            data.putFloat(offset + 16, outline.outlineThickness());
            data.putFloat(offset + 20, 0.0f);
            data.putFloat(offset + 24, 0.0f);
            data.putFloat(offset + 28, 0.0f);
            
            putColor(data, offset + 32, outline.fillColor());
            putColor(data, offset + 48, outline.outlineColor());
        }
        
        data.position(0);
        return data;
    }
    
    private BuiltArcOutline normalize(BuiltArcOutline outline) {
        float size = Math.max(0.0f, outline.size());
        float arcThickness = Math.max(0.0f, Math.min(outline.arcThickness(), size));
        float degree = Math.max(0.0f, Math.min(outline.degree(), 360.0f));
        float outlineThickness = Math.max(0.0f, Math.min(outline.outlineThickness(), arcThickness * 0.5f));
        return new BuiltArcOutline(
            outline.x(),
            outline.y(),
            size,
            arcThickness,
            degree,
            outline.rotation(),
            outlineThickness,
            outline.fillColor(),
            outline.outlineColor()
        );
    }
    
    private void putColor(ByteBuffer data, int offset, int color) {
        data.putFloat(offset, ((color >>> 16) & 0xFF) / 255.0f);
        data.putFloat(offset + 4, ((color >>> 8) & 0xFF) / 255.0f);
        data.putFloat(offset + 8, (color & 0xFF) / 255.0f);
        data.putFloat(offset + 12, ((color >>> 24) & 0xFF) / 255.0f);
    }
    
    private static Identifier id(String path) {
        return Identifier.of("meteor", path);
    }
    
    @Override
    public void close() {
        closeParamsBuffer();
    }
    
    private void closeParamsBuffer() {
        if (paramsBuffer != null) {
            paramsBuffer.close();
            paramsBuffer = null;
        }
    }
    
}
