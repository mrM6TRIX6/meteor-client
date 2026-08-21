package meteordevelopment.meteorclient.utils.render.ui.msdf;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.DrawContextAccessor;
import meteordevelopment.meteorclient.mixininterface.IGuiRenderStateLayer;
import meteordevelopment.meteorclient.utils.render.ScissorUtil;
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class MsdfRenderer implements AutoCloseable {
    
    private static final int MAX_MSDF = 1024;
    private static final int VEC4_PER_MSDF = 2;
    private static final int UNIFORM_BYTES = MAX_MSDF * VEC4_PER_MSDF * 4 * Float.BYTES;
    
    private static volatile MsdfRenderer instance;
    
    private static final VertexFormat MSDF_VERTEX_FORMAT = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("Color", VertexFormatElement.COLOR)
        .add("LineWidth", VertexFormatElement.LINE_WIDTH)
        .build();
    
    public static final RenderPipeline MSDF_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/msdf"))
        .withVertexShader(MeteorClient.identifier("ui/msdf/msdf"))
        .withFragmentShader(MeteorClient.identifier("ui/msdf/msdf"))
        .withVertexFormat(MSDF_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("MsdfParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final Map<FrameBatchKey, MsdfRenderState> frameBatches = new LinkedHashMap<>(32);
    private final List<MsdfEntry> preparedMsdf = new ArrayList<>(32);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    
    private MsdfRenderer() {}
    
    public static MsdfRenderer getInstance() {
        MsdfRenderer local = instance;
        if (local == null) {
            synchronized (MsdfRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new MsdfRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        MsdfRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    public void beginFrame(DrawContext context) {
        if (activeContext != context) {
            frameBatches.clear();
        }
        activeContext = context;
    }
    
    public void flush() {
        activeContext = null;
        frameBatches.clear();
    }
    
    public void barrier() {
        frameBatches.clear();
    }

    public void beginGuiFrame() {
        preparedMsdf.clear();
        paramsDirty = false;
    }
    
    public boolean isMsdfPipeline(RenderPipeline pipeline) {
        return pipeline == MSDF_PIPELINE;
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedMsdf.isEmpty()) {
            return;
        }
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("MsdfParamsArray", buffer);
        }
    }
    
    public void prepareBuffers() {
        if (preparedMsdf.isEmpty() || !paramsDirty) {
            return;
        }
        GpuBuffer buffer = ensureWritableParamsBuffer();
        if (buffer == null) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer uniformData = buildUniformData(stack, preparedMsdf);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(buffer.slice(0, uniformData.remaining()), uniformData);
            paramsDirty = false;
        } catch (RuntimeException ignored) {
            paramsDirty = true;
        }
    }
    
    public void enqueue(BuiltMsdf built) {
        if (activeContext == null || built == null || !built.visible()) {
            return;
        }
        submit(activeContext, built);
    }
    
    public void draw(DrawContext context, BuiltMsdf built) {
        beginFrame(context);
        enqueue(built);
        flush();
    }
    
    int reserve(float range, BuiltMsdf built) {
        int index = preparedMsdf.size();
        if (index >= MAX_MSDF) {
            return -1;
        }
        preparedMsdf.add(new MsdfEntry(range, built));
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltMsdf built) {
        if (context == null || built == null || !built.visible()) {
            return;
        }
        
        try {
            MsdfFont font = built.font();
            MsdfAtlas atlas = font.atlas();
            TextureSetup setup = font.textureSetup();
            if (setup == null) {
                return;
            }
            
            GuiRenderState guiState = ((DrawContextAccessor) context).meteor$getState();
            int layerSerial = ((IGuiRenderStateLayer) guiState).meteor$getLayerSerial();
            Matrix3x2f pose = Render2D.pose(context);
            ScreenRect scissorArea = ScissorUtil.current();
            
            FrameBatchKey key = new FrameBatchKey(
                guiState,
                layerSerial,
                PoseKey.of(pose),
                scissorArea,
                setup,
                built
            );
            
            MsdfRenderState state = frameBatches.get(key);
            if (state == null) {
                state = new MsdfRenderState(pose, setup, scissorArea, atlas.distanceRange(), built);
                frameBatches.put(key, state);
                guiState.addPreparedTextElement(state);
            }
            
            MsdfRenderState finalState = state;
            built.text().ifLeft(text -> {
                float cursorX = built.x();
                float scale = built.size() / atlas.fontSize();
                int index = 0;
                while (index < text.length()) {
                    int codePoint = text.codePointAt(index);
                    index += Character.charCount(codePoint);
                    MsdfGlyph glyph = font.glyph(codePoint);
                    if (glyph == null) {
                        continue;
                    }
                    if (glyph.drawable()) {
                        finalState.add(
                            cursorX + glyph.xOffset() * scale,
                            built.y() + glyph.yOffset() * scale,
                            glyph.width() * scale,
                            glyph.height() * scale,
                            glyph,
                            built.color(),
                            built.rotationDegrees(),
                            built.rotationOriginX(),
                            built.rotationOriginY(),
                            built.fadeLeft(),
                            built.fadeRight(),
                            built.fadeLeftX(),
                            built.fadeRightX(),
                            built.fadeWidth(),
                            built.fadeLeftStrength(),
                            built.fadeRightStrength()
                        );
                    }
                    cursorX += glyph.advance() * scale;
                }
            }).ifRight(text -> { // РАДИАЦИЯ ОПАСНО ОПАСНО!!!
                AtomicReference<Float> cursorX = new AtomicReference<>(built.x());
                float scale = built.size() / atlas.fontSize();
                
                text.asOrderedText().accept(
                    (index, style, codePoint) -> {
                        MsdfGlyph glyph = font.glyph(codePoint);
                        if (glyph == null) {
                            return true;
                        }
                        if (glyph.drawable()) {
                            int color = style.getColor() != null ? ColorUtil.withAlpha(style.getColor().getRgb(), 0xFF) : built.color();
                            finalState.add(
                                cursorX.get() + glyph.xOffset() * scale,
                                built.y() + glyph.yOffset() * scale,
                                glyph.width() * scale,
                                glyph.height() * scale,
                                glyph,
                                color,
                                built.rotationDegrees(),
                                built.rotationOriginX(),
                                built.rotationOriginY(),
                                built.fadeLeft(),
                                built.fadeRight(),
                                built.fadeLeftX(),
                                built.fadeRightX(),
                                built.fadeWidth(),
                                built.fadeLeftStrength(),
                                built.fadeRightStrength()
                            );
                        }
                        cursorX.updateAndGet(v -> v + glyph.advance() * scale);
                        return true;
                    }
                );
            });
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
            ByteBuffer uniformData = buildUniformData(stack, preparedMsdf);
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_msdf_params",
                GpuBuffer.USAGE_UNIFORM,
                uniformData
            );
            paramsDirty = false;
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private GpuBuffer ensureWritableParamsBuffer() {
        if (paramsBuffer != null && !paramsBuffer.isClosed() && paramsBuffer.size() >= UNIFORM_BYTES) {
            return paramsBuffer;
        }
        closeParamsBuffer();
        try {
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_msdf_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack, List<MsdfEntry> batch) {
        int usedBytes = Math.max(1, batch.size()) * VEC4_PER_MSDF * 4 * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        for (int i = 0; i < batch.size(); i++) {
            MsdfEntry entry = batch.get(i);
            BuiltMsdf built = entry.built();
            int off = i * VEC4_PER_MSDF * 16;
            
            data.putFloat(off, entry.range());
            data.putFloat(off + 4, built.thickness());
            data.putFloat(off + 8, built.smoothness());
            data.putFloat(off + 12, built.outline() ? built.outlineThickness() : 0.0f);
            
            int c = built.outlineColor();
            data.putFloat(off + 16, ((c >> 16) & 255) / 255.0f);
            data.putFloat(off + 20, ((c >> 8) & 255) / 255.0f);
            data.putFloat(off + 24, (c & 255) / 255.0f);
            data.putFloat(off + 28, ((c >>> 24) & 255) / 255.0f);
        }
        data.position(0);
        return data;
    }
    
    private void closeParamsBuffer() {
        if (paramsBuffer != null) {
            paramsBuffer.close();
            paramsBuffer = null;
        }
    }
    
    @Override
    public void close() {
        closeParamsBuffer();
        frameBatches.clear();
        preparedMsdf.clear();
        activeContext = null;
    }
    
    private record MsdfEntry(float range, BuiltMsdf built) {
    
    }
    
    private record FrameBatchKey(
        GuiRenderState state,
        int layerSerial,
        PoseKey pose,
        ScreenRect scissorArea,
        TextureSetup textureSetup,
        BuiltMsdf built
    ) {
    
    }
    
    private record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
        
        static PoseKey of(Matrix3x2f matrix) {
            return new PoseKey(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21());
        }
        
    }
    
}