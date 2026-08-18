package meteordevelopment.meteorclient.utils.render.ui.outline.outlineglass;

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
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.blur.BlurFramebuffer;
import meteordevelopment.meteorclient.utils.render.ui.blur.BuiltBlur;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class GlassOutlineRenderer implements AutoCloseable {
    
    private static final int MAX_OUTLINES = 512;
    private static final int PARAMS_PER_OUTLINE = 6;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = MAX_OUTLINES * PARAMS_PER_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile GlassOutlineRenderer instance;
    
    public static final RenderPipeline GLASS_OUTLINE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/outline_glass"))
        .withVertexShader(MeteorClient.identifier("ui/outline_glass/outline_glass"))
        .withFragmentShader(MeteorClient.identifier("ui/outline_glass/outline_glass"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("GlassOutlineParamsArray", UniformType.UNIFORM_BUFFER)
        .withUniform("BlurRegion", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<BuiltGlassOutline> preparedOutlines = new ArrayList<>(128);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    
    private GlassOutlineRenderer() {
    }
    
    public static GlassOutlineRenderer getInstance() {
        GlassOutlineRenderer local = instance;
        if (local == null) {
            synchronized (GlassOutlineRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new GlassOutlineRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        GlassOutlineRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        activeContext = context;
    }
    
    public void draw(DrawContext context, BuiltGlassOutline outline) {
        beginFrame(context);
        enqueue(outline);
        flush();
    }
    
    public void enqueue(BuiltGlassOutline outline) {
        submit(activeContext, outline);
    }
    
    public void flush() {
        activeContext = null;
    }
    
    public void beginGuiFrame() {
        preparedOutlines.clear();
        paramsDirty = false;
    }
    
    public boolean isGlassOutlinePipeline(RenderPipeline pipeline) {
        return pipeline == GLASS_OUTLINE_PIPELINE;
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedOutlines.isEmpty()) {
            return;
        }
        
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("GlassOutlineParamsArray", buffer);
        }
        BlurFramebuffer.getInstance().bindBlurRegion(renderPass);
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
    
    int reserve(BuiltGlassOutline outline) {
        int index = preparedOutlines.size();
        if (index == MAX_OUTLINES) {
            return -1;
        }
        
        preparedOutlines.add(outline);
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltGlassOutline outline) {
        if (context == null || outline == null || !outline.visible()) {
            return;
        }
        
        try {
            BuiltGlassOutline normalized = normalize(outline);
            Matrix3x2f pose = Render2D.pose(context);
            BlurFramebuffer.getInstance().requestCapture(
                context,
                new BuiltBlur(
                    normalized.x(),
                    normalized.y(),
                    normalized.width(),
                    normalized.height(),
                    normalized.radiusTopLeft(),
                    normalized.radiusTopRight(),
                    normalized.radiusBottomRight(),
                    normalized.radiusBottomLeft(),
                    1.0f,
                    30.0f,
                    ColorUtil.WHITE
                )
            );
            ((DrawContextAccessor) context)
                .meteor$getState()
                .addSimpleElement(new GlassOutlineRenderState(pose, normalized, ScissorUtil.current()));
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
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_outline_glass_params", GpuBuffer.USAGE_UNIFORM, uniformData);
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
                () -> "meteor_outline_glass_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltGlassOutline> batch) {
        int usedBytes = Math.max(1, batch.size()) * PARAMS_PER_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        
        for (int i = 0; i < batch.size(); i++) {
            BuiltGlassOutline outline = batch.get(i);
            int offset = i * PARAMS_PER_OUTLINE * FLOATS_PER_PARAM * Float.BYTES;
            
            data.putFloat(offset, outline.radiusTopLeft());
            data.putFloat(offset + 4, outline.radiusTopRight());
            data.putFloat(offset + 8, outline.radiusBottomRight());
            data.putFloat(offset + 12, outline.radiusBottomLeft());
            
            data.putFloat(offset + 16, outline.width());
            data.putFloat(offset + 20, outline.height());
            data.putFloat(offset + 24, outline.thickness());
            data.putFloat(offset + 28, outline.smoothness());
            
            data.putFloat(offset + 32, outline.globalAlpha());
            data.putFloat(offset + 36, outline.fresnelPower());
            data.putFloat(offset + 40, outline.baseAlpha());
            data.putFloat(offset + 44, outline.fresnelMix());
            
            putColor(data, offset + 48, outline.fresnelColor());
            
            data.putFloat(offset + 64, outline.fresnelInvert() ? 1.0f : 0.0f);
            data.putFloat(offset + 68, outline.distortStrength());
            data.putFloat(offset + 72, outline.z());
            data.putFloat(offset + 76, Math.max(outline.squirt(), 0.001f));
            
            putColor(data, offset + 80, outline.color());
        }
        
        data.position(0);
        return data;
    }
    
    private BuiltGlassOutline normalize(BuiltGlassOutline outline) {
        float maxRadius = Math.max(0.0f, Math.min(outline.width(), outline.height()) * 0.5f);
        float maxThickness = Math.max(0.0f, Math.min(outline.width(), outline.height()) * 0.5f);
        float radiusTopLeft = clamp(outline.radiusTopLeft(), 0.0f, maxRadius);
        float radiusTopRight = clamp(outline.radiusTopRight(), 0.0f, maxRadius);
        float radiusBottomRight = clamp(outline.radiusBottomRight(), 0.0f, maxRadius);
        float radiusBottomLeft = clamp(outline.radiusBottomLeft(), 0.0f, maxRadius);
        float thickness = clamp(outline.thickness(), 0.0f, maxThickness);
        float globalAlpha = clamp(outline.globalAlpha(), 0.0f, 1.0f);
        float fresnelPower = Math.max(outline.fresnelPower(), 0.001f);
        float baseAlpha = clamp(outline.baseAlpha(), 0.0f, 1.0f);
        float fresnelMix = clamp(outline.fresnelMix(), 0.0f, 1.0f);
        float squirt = Math.max(outline.squirt(), 0.001f);
        float smoothness = Math.max(outline.smoothness(), 0.5f);
        
        if (radiusTopLeft == outline.radiusTopLeft()
            && radiusTopRight == outline.radiusTopRight()
            && radiusBottomRight == outline.radiusBottomRight()
            && radiusBottomLeft == outline.radiusBottomLeft()
            && thickness == outline.thickness()
            && globalAlpha == outline.globalAlpha()
            && fresnelPower == outline.fresnelPower()
            && baseAlpha == outline.baseAlpha()
            && fresnelMix == outline.fresnelMix()
            && squirt == outline.squirt()
            && smoothness == outline.smoothness()) {
            return outline;
        }
        
        return new BuiltGlassOutline(
            outline.x(),
            outline.y(),
            outline.width(),
            outline.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            outline.color(),
            globalAlpha,
            fresnelPower,
            outline.fresnelColor(),
            baseAlpha,
            outline.fresnelInvert(),
            fresnelMix,
            outline.distortStrength(),
            squirt,
            smoothness,
            outline.z()
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
