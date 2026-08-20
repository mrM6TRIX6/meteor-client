package meteordevelopment.meteorclient.utils.render.ui.glow;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.DrawContextAccessor;
import meteordevelopment.meteorclient.utils.render.ScissorUtil;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.BufferAllocator;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class GlowRenderer implements AutoCloseable {
    
    private static final float TILE_SCALE = 1.0f;
    private static final int ATLAS_WIDTH = 2048;
    private static final int ATLAS_MAX_HEIGHT = 2048;
    private static final int ATLAS_HEIGHT_BUCKET = 256;
    private static final int TILE_GAP = 8;
    private static final int MAX_GLOWS = 224;
    private static final int PARAMS_PER_GLOW = 5;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 17920;
    private static final int KAWASE_UNIFORM_BYTES = 48;
    private static final int MAX_BLUR_ITERATIONS = 4;
    private static final int MAX_SPANS = 64;
    private static final int PALETTE_BASE_VEC4 = 70;
    private static final int PALETTE_VEC4 = 8;
    private static final int SHAPE_PARAMS_VEC4 = 82;
    private static final int CORNER_COLOR_BASE_VEC4 = 78;
    private static final int SHAPE_BYTES = 1312;
    
    public static final RenderPipeline GLOW_COMPOSITE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_composite"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_composite"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_composite"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    public static final RenderPipeline GLOW_SHAPE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_shape"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_shape"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_shape"))
        .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
        .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
        .build();
    
    public static final RenderPipeline GLOW_SOURCE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_source"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_source"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_shape"))
        .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
        .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
        .build();
    
    public static final RenderPipeline GLOW_BLUR_DOWN_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_blur_down"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_blur"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_blur_down"))
        .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
        .build();
    
    public static final RenderPipeline GLOW_BLUR_UP_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_blur_up"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_blur"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_blur_up"))
        .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
        .build();
    
    public static final RenderPipeline GLOW_CUTOUT_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_cutout"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_blur"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_cutout"))
        .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withSampler("Sampler1")
        .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final List<SimpleFramebuffer[]> downPool = new ArrayList<>(224);
    private final List<SimpleFramebuffer[]> upPool = new ArrayList<>(224);
    private final List<SimpleFramebuffer> shapeTargets = new ArrayList<>(224);
    private final List<SimpleFramebuffer> blurTargets = new ArrayList<>(224);
    private final List<SimpleFramebuffer> resultTargets = new ArrayList<>(224);
    private final List<PendingGlow> pendingGlows = new ArrayList<>(32);
    private final List<BuiltGlow> preparedGlows = new ArrayList<>(32);
    private final List<GlowCapture> preparedCaptures = new ArrayList<>(32);
    private final GlowCapture globalCapture = new GlowCapture();
    
    private static volatile GlowRenderer instance;
    
    private int[] tileX = new int[224];
    private int[] tileY = new int[224];
    private int[] tileW = new int[224];
    private int[] tileH = new int[224];
    
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private GpuBuffer kawaseParamsBuffer;
    private GpuBuffer fullscreenQuadBuffer;
    
    private boolean paramsDirty = true;
    
    private float effectiveGlowRadius;
    private float pendingMaxRadius;
    private float lastEffectiveGlowRadius;
    
    private GlowRenderer() {}
    
    public static GlowRenderer getInstance() {
        GlowRenderer local = instance;
        if (local == null) {
            synchronized (GlowRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new GlowRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        GlowRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }
    
    @Override
    public void close() {
        preparedGlows.clear();
        activeGraphics = null;
        closeParamsBuffer();
        closeKawaseParamsBuffer();
        
        if (fullscreenQuadBuffer != null) {
            fullscreenQuadBuffer.close();
            fullscreenQuadBuffer = null;
        }
        
        for (SimpleFramebuffer simpleFramebuffer : shapeTargets) {
            if (simpleFramebuffer == null) {
                continue;
            }
            simpleFramebuffer.delete();
        }
        
        for (SimpleFramebuffer simpleFramebuffer : blurTargets) {
            if (simpleFramebuffer == null) {
                continue;
            }
            simpleFramebuffer.delete();
        }
        
        for (SimpleFramebuffer simpleFramebuffer : resultTargets) {
            if (simpleFramebuffer == null) {
                continue;
            }
            simpleFramebuffer.delete();
        }
        
        shapeTargets.clear();
        blurTargets.clear();
        resultTargets.clear();
        
        GlowRenderer.destroyPool(downPool);
        GlowRenderer.destroyPool(upPool);
    }
    
    public void enqueue(BuiltGlow builtGlow) {
        submit(activeGraphics, builtGlow);
    }
    
    private void submit(DrawContext drawContext, BuiltGlow builtGlow) {
        if (drawContext == null || builtGlow == null || !builtGlow.visible()) {
            return;
        }
        
        try {
            Matrix3x2f matrix3x2f = Render2D.pose(drawContext);
            GlowCapture glowCapture = new GlowCapture();
            pendingMaxRadius = Math.max(pendingMaxRadius, builtGlow.glowRadius());
            
            if (pendingGlows.size() < 224) {
                glowCapture.index = pendingGlows.size();
                pendingGlows.add(new PendingGlow(builtGlow, glowCapture, matrix3x2f));
            }
            ((DrawContextAccessor) drawContext)
                .meteor$getState()
                .addSimpleElement(new GlowRenderState(matrix3x2f, builtGlow, ScissorUtil.current(), glowCapture));
        } catch (RuntimeException ignored) {
        }
    }
    
    public float effectiveGlowRadius() {
        return effectiveGlowRadius;
    }
    
    float submitRadiusHint(float ownRadius) {
        return Math.max(ownRadius, Math.max(pendingMaxRadius, lastEffectiveGlowRadius));
    }
    
    int reserve(BuiltGlow builtGlow, GlowCapture glowCapture) {
        int n = preparedGlows.size();
        if (n >= 224) {
            return -1;
        }
        preparedGlows.add(builtGlow);
        preparedCaptures.add(glowCapture);
        paramsDirty = true;
        return n;
    }
    
    public void beginGuiFrame() {
        preparedGlows.clear();
        preparedCaptures.clear();
        paramsDirty = false;
        pendingMaxRadius = 0.0f;
    }
    
    public void preparePending() {
        int n;
        int n2;
        
        if (pendingGlows.isEmpty()) {
            return;
        }
        
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.getWindow() == null) {
            pendingGlows.clear();
            return;
        }
        
        float effectiveRadius = 0.0f;
        
        for (n2 = 0; n2 < pendingGlows.size(); ++n2) {
            effectiveRadius = Math.max(effectiveRadius, pendingGlows.get(n2).glow().glowRadius());
        }
        
        effectiveGlowRadius = effectiveRadius;
        lastEffectiveGlowRadius = effectiveRadius;
        float effectivePad = BuiltGlow.padFor(effectiveRadius);
        int n3 = packTiles(effectivePad);
        int n4 = 2048;
        int n5 = Math.clamp((n3 + 256 - 1) / 256 * 256, 256, 2048);
        SimpleFramebuffer simpleFramebuffer = ensureShapeTarget(n4, n5);
        SimpleFramebuffer simpleFramebuffer2 = ensureBlurTarget(n4, n5);
        SimpleFramebuffer simpleFramebuffer3 = ensureResultTarget(n4, n5);
        
        GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        clearTarget(simpleFramebuffer);
        
        for (n2 = 0; n2 < pendingGlows.size(); ++n2) {
            PendingGlow pendingGlow = pendingGlows.get(n2);
            pendingGlow.capture().effectivePad = effectivePad;
            pendingGlow.capture().prepared = true;
            if (tileW[n2] > 0 && tileH[n2] > 0) {
                renderSourceShape(n2, pendingGlow.glow(), pendingGlow.capture(), simpleFramebuffer, tileX[n2], tileY[n2], tileW[n2], tileH[n2], n4, n5, effectivePad);
            } else {
                pendingGlow.capture().regionU0 = 0.0f;
                pendingGlow.capture().regionV0 = 0.0f;
                pendingGlow.capture().regionUW = 0.0f;
                pendingGlow.capture().regionVH = 0.0f;
            }
        }
        
        n2 = 3;
        // float f2 = MathHelper.clamp(effectiveRadius / 20.0f, 0.5f, 4.0f);
        float f2 = effectiveRadius / 20f;
        ensureScratchTargets(n4, n5, n2);
        SimpleFramebuffer[] simpleFramebufferArray = downPool.getFirst();
        SimpleFramebuffer[] simpleFramebufferArray2 = upPool.getFirst();
        GpuTextureView gpuTextureView = simpleFramebuffer.getColorAttachmentView();
        int n6 = simpleFramebuffer.textureWidth;
        int n7 = simpleFramebuffer.textureHeight;
        
        for (n = 0; n < n2; ++n) {
            renderKawasePass(GLOW_BLUR_DOWN_PIPELINE, gpuTextureView, n6, n7, simpleFramebufferArray[n], f2, gpuSampler);
            gpuTextureView = simpleFramebufferArray[n].getColorAttachmentView();
            n6 = simpleFramebufferArray[n].textureWidth;
            n7 = simpleFramebufferArray[n].textureHeight;
        }
        
        for (n = n2 - 2; n >= 0; --n) {
            renderKawasePass(GLOW_BLUR_UP_PIPELINE, gpuTextureView, n6, n7, simpleFramebufferArray2[n], f2, gpuSampler);
            gpuTextureView = simpleFramebufferArray2[n].getColorAttachmentView();
            n6 = simpleFramebufferArray2[n].textureWidth;
            n7 = simpleFramebufferArray2[n].textureHeight;
        }
        
        renderKawasePass(GLOW_BLUR_UP_PIPELINE, gpuTextureView, n6, n7, simpleFramebuffer2, f2, gpuSampler);
        renderCutoutPass(simpleFramebuffer2, simpleFramebuffer, simpleFramebuffer3, gpuSampler, n4, n5);
        TextureSetup textureSetup = TextureSetup.of(simpleFramebuffer3.getColorAttachmentView(), gpuSampler);
        
        for (PendingGlow pendingGlow : pendingGlows) {
            pendingGlow.capture().setup = textureSetup;
        }
        
        pendingGlows.clear();
        pendingMaxRadius = 0.0f;
    }
    
    public boolean isGlowPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == GLOW_COMPOSITE_PIPELINE;
    }
    
    public void flush() {
        activeGraphics = null;
    }
    
    public void prepareBuffers() {
        if (preparedGlows.isEmpty() || !paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();) {
            ByteBuffer byteBuffer = buildUniformData(memoryStack);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long) byteBuffer.remaining()), byteBuffer);
            paramsDirty = false;
        } catch (RuntimeException runtimeException) {
            paramsDirty = true;
        }
    }
    
    public void beginFrame(DrawContext drawContext) {
        activeGraphics = drawContext;
    }
    
    public void draw(DrawContext drawContext, BuiltGlow builtGlow) {
        beginFrame(drawContext);
        enqueue(builtGlow);
        flush();
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedGlows.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("GlowParamsArray", gpuBuffer);
        }
        if (!preparedCaptures.isEmpty()) {
            GlowCapture capture = preparedCaptures.getFirst();
            if (capture.setup.texure0() != null && capture.setup.sampler0() != null) {
                renderPass.bindTexture("Sampler0", capture.setup.texure0(), capture.setup.sampler0());
            }
        }
    }
    
    private GpuBuffer ensureWritableParamsBuffer() {
        if (paramsBuffer != null && !paramsBuffer.isClosed() && paramsBuffer.size() >= 17920L) {
            return paramsBuffer;
        }
        closeParamsBuffer();
        try {
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_glow_params", 136, 17920L);
            return paramsBuffer;
        } catch (RuntimeException runtimeException) {
            return null;
        }
    }
    
    private BuiltBuffer buildFullscreenQuad(BufferAllocator bufferAllocator) {
        BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(-1.0f, -1.0f, 0.0f);
        bufferBuilder.vertex(-1.0f, 1.0f, 0.0f);
        bufferBuilder.vertex(1.0f, 1.0f, 0.0f);
        bufferBuilder.vertex(1.0f, -1.0f, 0.0f);
        return bufferBuilder.end();
    }
    
    private GpuBuffer ensureKawaseParamsBuffer() {
        if (kawaseParamsBuffer != null && !kawaseParamsBuffer.isClosed() && kawaseParamsBuffer.size() >= 48L) {
            return kawaseParamsBuffer;
        }
        closeKawaseParamsBuffer();
        try {
            kawaseParamsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_glow_kawase_params", 136, 48L);
            return kawaseParamsBuffer;
        } catch (RuntimeException runtimeException) {
            return null;
        }
    }
    
    private void ensureScratchTargets(int n2, int n3, int n4) {
        while (downPool.size() <= 0) {
            downPool.add(new SimpleFramebuffer[4]);
            upPool.add(new SimpleFramebuffer[4]);
        }
        SimpleFramebuffer[] simpleFramebufferArray = downPool.getFirst();
        SimpleFramebuffer[] simpleFramebufferArray2 = upPool.getFirst();
        int n5 = Math.max(n2, 1);
        int n6 = Math.max(n3, 1);
        for (int i = 0; i < n4; ++i) {
            simpleFramebufferArray[i] = ensureTarget(simpleFramebufferArray[i], "meteor_glow_down_" + 0 + "_" + i, n5, n6);
            if (i < n4 - 1) {
                simpleFramebufferArray2[i] = ensureTarget(simpleFramebufferArray2[i], "meteor_glow_up_" + 0 + "_" + i, n5, n6);
            }
            n5 = Math.max(n5 / 2, 1);
            n6 = Math.max(n6 / 2, 1);
        }
    }
    
    private void closeKawaseParamsBuffer() {
        if (kawaseParamsBuffer != null) {
            kawaseParamsBuffer.close();
            kawaseParamsBuffer = null;
        }
    }
    
    private void closeParamsBuffer() {
        if (paramsBuffer != null) {
            paramsBuffer.close();
            paramsBuffer = null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack memoryStack) {
        int n = Math.max(1, preparedGlows.size());
        int n2 = n * 5 * 4 * 4;
        ByteBuffer byteBuffer = memoryStack.calloc(n2);
        for (int i = 0; i < preparedGlows.size(); ++i) {
            BuiltGlow builtGlow = preparedGlows.get(i);
            GlowCapture glowCapture = i < preparedCaptures.size() ? preparedCaptures.get(i) : null;
            float f = glowCapture != null ? glowCapture.regionU0 : 0.0f;
            float f2 = glowCapture != null ? glowCapture.regionV0 : 0.0f;
            float f3 = glowCapture != null ? glowCapture.regionUW : 1.0f;
            float f4 = glowCapture != null ? glowCapture.regionVH : 1.0f;
            float pad = glowCapture != null && glowCapture.prepared ? glowCapture.effectivePad : builtGlow.effectivePad();
            GlowRenderer.writeGlowParams(byteBuffer, i, builtGlow, f, f2, f3, f4, pad);
        }
        byteBuffer.position(0);
        return byteBuffer;
    }
    
    private SimpleFramebuffer ensureResultTarget(int n2, int n3) {
        while (resultTargets.size() <= 0) {
            resultTargets.add(null);
        }
        SimpleFramebuffer simpleFramebuffer = ensureTarget(resultTargets.getFirst(), "meteor_glow_result_" + 0, n2, n3);
        resultTargets.set(0, simpleFramebuffer);
        return simpleFramebuffer;
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
        try (MemoryStack memoryStack = MemoryStack.stackPush();) {
            ByteBuffer byteBuffer = buildUniformData(memoryStack);
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_glow_params", 128, byteBuffer);
            paramsDirty = false;
            return paramsBuffer;
        }
    }
    
    private SimpleFramebuffer ensureTarget(SimpleFramebuffer simpleFramebuffer, String string, int n, int n2) {
        if (simpleFramebuffer == null) {
            return new SimpleFramebuffer(string, n, n2, false);
        }
        if (simpleFramebuffer.textureWidth != n || simpleFramebuffer.textureHeight != n2) {
            simpleFramebuffer.resize(n, n2);
        }
        return simpleFramebuffer;
    }
    
    private GpuBuffer ensureFullscreenQuadBuffer() {
        if (fullscreenQuadBuffer != null) {
            return fullscreenQuadBuffer;
        }
        try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize());
            BuiltBuffer builtBuffer = buildFullscreenQuad(bufferAllocator)) {
            fullscreenQuadBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_glow_fullscreen_quad", 32, builtBuffer.getBuffer());
        }
        return fullscreenQuadBuffer;
    }
    
    private void renderKawasePass(RenderPipeline renderPipeline, GpuTextureView gpuTextureView, int n, int n2, SimpleFramebuffer simpleFramebuffer, float f, GpuSampler gpuSampler) {
        if (gpuTextureView == null || simpleFramebuffer == null) {
            return;
        }
        GpuBuffer gpuBuffer = ensureFullscreenQuadBuffer();
        if (gpuBuffer == null) {
            return;
        }
        GpuBuffer gpuBuffer2 = ensureKawaseParamsBuffer();
        if (gpuBuffer2 == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
        GpuBuffer gpuBuffer3 = shapeIndexBuffer.getIndexBuffer(6);
        try (MemoryStack memoryStack = MemoryStack.stackPush();) {
            ByteBuffer byteBuffer = memoryStack.calloc(48);
            byteBuffer.putFloat(0, (float) 0.0);
            byteBuffer.putFloat(4, (float) 0.0);
            byteBuffer.putFloat(8, (float) 1.0);
            byteBuffer.putFloat(12, (float) 1.0);
            byteBuffer.putFloat(16, f / (float) Math.max(n, 1));
            byteBuffer.putFloat(20, f / (float) Math.max(n2, 1));
            byteBuffer.putFloat(32, 0.0f);
            byteBuffer.putFloat(36, 0.0f);
            byteBuffer.putFloat(40, 0.0f);
            byteBuffer.putFloat(44, 0.0f);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 48L), byteBuffer);
        }
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_glow_kawase", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty())) {
            renderPass.setPipeline(renderPipeline);
            renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
            renderPass.setUniform("KawaseParams", gpuBuffer2);
            renderPass.setVertexBuffer(0, gpuBuffer);
            renderPass.setIndexBuffer(gpuBuffer3, shapeIndexBuffer.getIndexType());
            renderPass.drawIndexed(0, 0, 6, 1);
        }
    }
    
    private static void writeShapeSpans(ByteBuffer byteBuffer, BuiltGlow builtGlow, float effectivePad) {
        float[] fArray = builtGlow.spans();
        int n = builtGlow.spanCount();
        if (fArray == null || n <= 0) {
            return;
        }
        float f2 = Math.min(Math.min(builtGlow.radii()[0], builtGlow.radii()[1]), Math.min(builtGlow.radii()[2], builtGlow.radii()[3]));
        int n2 = 80;
        byteBuffer.putFloat(n2, Math.min(n, 64));
        byteBuffer.putFloat(n2 + 4, f2);
        byteBuffer.putFloat(n2 + 8, builtGlow.leftAligned() ? 1.0f : 0.0f);
        byteBuffer.putFloat(n2 + 12, builtGlow.bottomAnchored() ? 0.0f : 1.0f);
        int n3 = n2 + 16;
        int n4 = Math.min(n, 64);
        float f3 = builtGlow.height() + effectivePad * 2.0f;
        for (int i = 0; i < n4; ++i) {
            int n5 = i * 4;
            float f4 = fArray[n5] + effectivePad;
            float f5 = fArray[n5 + 1] + effectivePad;
            float f6 = fArray[n5 + 2] + effectivePad;
            float f7 = fArray[n5 + 3] + effectivePad;
            float f8 = f3 - f7;
            float f9 = f3 - f6;
            int n6 = n3 + i * 4 * 4;
            byteBuffer.putFloat(n6, f4);
            byteBuffer.putFloat(n6 + 4, f5);
            byteBuffer.putFloat(n6 + 8, f8);
            byteBuffer.putFloat(n6 + 12, f9);
        }
    }
    
    private SimpleFramebuffer ensureBlurTarget(int n2, int n3) {
        while (blurTargets.isEmpty()) {
            blurTargets.add(null);
        }
        SimpleFramebuffer simpleFramebuffer = ensureTarget(blurTargets.getFirst(), "meteor_glow_blur_" + 0, n2, n3);
        blurTargets.set(0, simpleFramebuffer);
        return simpleFramebuffer;
    }
    
    private void renderSourceShape(int n, BuiltGlow builtGlow, GlowCapture glowCapture, SimpleFramebuffer simpleFramebuffer, int n2, int n3, int n4, int n5, int n6, int n7, float effectivePad) {
        if (simpleFramebuffer == null) {
            return;
        }
        float f2 = Math.min((builtGlow.width() + effectivePad * 2.0f), (float) n4);
        float f3 = Math.min((builtGlow.height() + effectivePad * 2.0f), (float) n5);
        glowCapture.regionU0 = (float) n2 / (float) n6;
        glowCapture.regionV0 = (float) n3 / (float) n7;
        glowCapture.regionUW = Math.max(f2 / (float) n6, 1.0E-6f);
        glowCapture.regionVH = Math.max(f3 / (float) n7, 1.0E-6f);
        float f4 = (float) n2 / (float) n6 * 2.0f - 1.0f;
        float f5 = (float) n3 / (float) n7 * 2.0f - 1.0f;
        float f6 = ((float) n2 + f2) / (float) n6 * 2.0f - 1.0f;
        float f7 = ((float) n3 + f3) / (float) n7 * 2.0f - 1.0f;
        GpuBuffer gpuBuffer;
        try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
            bufferBuilder.vertex(f4, f5, 0.0f);
            bufferBuilder.vertex(f4, f7, 0.0f);
            bufferBuilder.vertex(f6, f7, 0.0f);
            bufferBuilder.vertex(f6, f5, 0.0f);
            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_glow_src_quad_" + n, 32, builtBuffer.getBuffer());
            }
        } catch (RuntimeException runtimeException) {
            return;
        }
        if (gpuBuffer == null) {
            return;
        }
        GpuBuffer paramsGpuBuffer;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.calloc(1312);
            GlowRenderer.writeGlowParams(byteBuffer, 0, builtGlow, 0.0f, 0.0f, 1.0f, 1.0f, effectivePad);
            byteBuffer.putFloat(48, builtGlow.splitIndex());
            GlowRenderer.writeShapeSpans(byteBuffer, builtGlow, effectivePad);
            GlowRenderer.writePalette(byteBuffer, builtGlow);
            GlowRenderer.writeCornerColors(byteBuffer, builtGlow);
            byteBuffer.position(0);
            paramsGpuBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_glow_src_params_" + n, 136, byteBuffer);
        } catch (RuntimeException runtimeException) {
            gpuBuffer.close();
            return;
        }
        try {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode) VertexFormat.DrawMode.QUADS);
            GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(6);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_glow_source_" + n, simpleFramebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty());) {
                renderPass.setPipeline(GLOW_SOURCE_PIPELINE);
                renderPass.setUniform("GlowParamsArray", paramsGpuBuffer);
                GpuBuffer gpuBuffer3 = Stubs.ClientSplits.buffer();
                if (gpuBuffer3 != null) {
                    renderPass.setUniform("SplitParams", gpuBuffer3);
                }
                renderPass.setVertexBuffer(0, gpuBuffer);
                renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                renderPass.drawIndexed(0, 0, 6, 1);
            }
        } finally {
            if (paramsGpuBuffer != null) {
                paramsGpuBuffer.close();
            }
            gpuBuffer.close();
        }
    }
    
    private void clearTarget(SimpleFramebuffer simpleFramebuffer) {
        if (simpleFramebuffer == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_glow_clear", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty());
        if (renderPass != null) {
            renderPass.close();
        }
    }
    
    private static void destroyPool(List<SimpleFramebuffer[]> list) {
        for (SimpleFramebuffer[] simpleFramebufferArray : list) {
            for (int i = 0; i < simpleFramebufferArray.length; ++i) {
                if (simpleFramebufferArray[i] == null) {
                    continue;
                }
                simpleFramebufferArray[i].delete();
                simpleFramebufferArray[i] = null;
            }
        }
        list.clear();
    }
    
    private void renderCutoutPass(SimpleFramebuffer simpleFramebuffer, SimpleFramebuffer simpleFramebuffer2, SimpleFramebuffer simpleFramebuffer3, GpuSampler gpuSampler, int atlasWidth, int atlasHeight) {
        if (simpleFramebuffer == null || simpleFramebuffer2 == null || simpleFramebuffer3 == null) {
            return;
        }
        GpuBuffer gpuBuffer = ensureFullscreenQuadBuffer();
        GpuBuffer gpuBuffer2 = ensureKawaseParamsBuffer();
        if (gpuBuffer == null || gpuBuffer2 == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode) VertexFormat.DrawMode.QUADS);
        GpuBuffer gpuBuffer3 = shapeIndexBuffer.getIndexBuffer(6);
        writeCutoutParams(commandEncoder, gpuBuffer2, 1.0f);
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_glow_cutout", simpleFramebuffer3.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty())) {
            renderPass.setPipeline(GLOW_CUTOUT_PIPELINE);
            renderPass.bindTexture("Sampler0", simpleFramebuffer.getColorAttachmentView(), gpuSampler);
            renderPass.bindTexture("Sampler1", simpleFramebuffer2.getColorAttachmentView(), gpuSampler);
            renderPass.setUniform("KawaseParams", gpuBuffer2);
            renderPass.setVertexBuffer(0, gpuBuffer);
            renderPass.setIndexBuffer(gpuBuffer3, shapeIndexBuffer.getIndexType());
            renderPass.drawIndexed(0, 0, 6, 1);
        }
        patchSolidTiles(commandEncoder, simpleFramebuffer, simpleFramebuffer2, simpleFramebuffer3, gpuSampler, gpuBuffer2, gpuBuffer3, shapeIndexBuffer, atlasWidth, atlasHeight);
    }
    
    private void patchSolidTiles(CommandEncoder commandEncoder, SimpleFramebuffer blurTarget, SimpleFramebuffer shapeTarget, SimpleFramebuffer resultTarget, GpuSampler gpuSampler, GpuBuffer paramsBuffer, GpuBuffer indexBuffer, RenderSystem.ShapeIndexBuffer shapeIndexBuffer, int atlasWidth, int atlasHeight) {
        List<GpuBuffer> quads = new ArrayList<>();
        try {
            for (int i = 0; i < pendingGlows.size(); ++i) {
                if (pendingGlows.get(i).glow().cutout() || tileW[i] <= 0 || tileH[i] <= 0) {
                    continue;
                }
                GpuBuffer quad = createTileQuad(i, tileX[i], tileY[i], tileW[i], tileH[i], atlasWidth, atlasHeight);
                if (quad != null) {
                    quads.add(quad);
                }
            }
            if (quads.isEmpty()) {
                return;
            }
            writeCutoutParams(commandEncoder, paramsBuffer, 0.0f);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_glow_cutout_patch", resultTarget.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
                renderPass.setPipeline(GLOW_CUTOUT_PIPELINE);
                renderPass.bindTexture("Sampler0", blurTarget.getColorAttachmentView(), gpuSampler);
                renderPass.bindTexture("Sampler1", shapeTarget.getColorAttachmentView(), gpuSampler);
                renderPass.setUniform("KawaseParams", paramsBuffer);
                renderPass.setIndexBuffer(indexBuffer, shapeIndexBuffer.getIndexType());
                for (GpuBuffer quad : quads) {
                    renderPass.setVertexBuffer(0, quad);
                    renderPass.drawIndexed(0, 0, 6, 1);
                }
            }
        } catch (RuntimeException ignored) {
        } finally {
            quads.forEach(GpuBuffer::close);
        }
    }
    
    private void writeCutoutParams(CommandEncoder commandEncoder, GpuBuffer paramsBuffer, float cutout) {
        try (MemoryStack memoryStack = MemoryStack.stackPush();) {
            ByteBuffer byteBuffer = memoryStack.calloc(48);
            byteBuffer.putFloat(0, 0.0f);
            byteBuffer.putFloat(4, 0.0f);
            byteBuffer.putFloat(8, 1.0f);
            byteBuffer.putFloat(12, 1.0f);
            byteBuffer.putFloat(32, cutout);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(paramsBuffer.slice(0L, 48L), byteBuffer);
        }
    }
    
    private GpuBuffer createTileQuad(int index, int tileX, int tileY, int tileW, int tileH, int atlasWidth, int atlasHeight) {
        float x0 = (float) tileX / (float) atlasWidth * 2.0f - 1.0f;
        float y0 = (float) tileY / (float) atlasHeight * 2.0f - 1.0f;
        float x1 = (float) (tileX + tileW) / (float) atlasWidth * 2.0f - 1.0f;
        float y1 = (float) (tileY + tileH) / (float) atlasHeight * 2.0f - 1.0f;
        try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
            bufferBuilder.vertex(x0, y0, 0.0f);
            bufferBuilder.vertex(x0, y1, 0.0f);
            bufferBuilder.vertex(x1, y1, 0.0f);
            bufferBuilder.vertex(x1, y0, 0.0f);
            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                return RenderSystem.getDevice().createBuffer(() -> "meteor_glow_cutout_patch_quad_" + index, 32, builtBuffer.getBuffer());
            }
        } catch (RuntimeException runtimeException) {
            return null;
        }
    }
    
    private static void writeGlowParams(ByteBuffer byteBuffer, int n, BuiltGlow builtGlow, float f, float f2, float f3, float f4, float effectivePad) {
        int n2 = n * 5 * 4 * 4;
        byteBuffer.putFloat(n2, builtGlow.radii()[0]);
        byteBuffer.putFloat(n2 + 4, builtGlow.radii()[1]);
        byteBuffer.putFloat(n2 + 8, builtGlow.radii()[2]);
        byteBuffer.putFloat(n2 + 12, builtGlow.radii()[3]);
        byteBuffer.putFloat(n2 + 16, builtGlow.width());
        byteBuffer.putFloat(n2 + 20, builtGlow.height());
        byteBuffer.putFloat(n2 + 24, effectivePad);
        byteBuffer.putFloat(n2 + 28, 2.0f);
        int n3 = builtGlow.color();
        byteBuffer.putFloat(n2 + 32, (float) (n3 >>> 16 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 36, (float) (n3 >>> 8 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 40, (float) (n3 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 44, builtGlow.intensity() * builtGlow.alpha());
        byteBuffer.putFloat(n2 + 48, f);
        byteBuffer.putFloat(n2 + 52, f2);
        byteBuffer.putFloat(n2 + 56, f3);
        byteBuffer.putFloat(n2 + 60, f4);
        int n4 = builtGlow.secondColor();
        byteBuffer.putFloat(n2 + 64, (float) (n4 >>> 16 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 68, (float) (n4 >>> 8 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 72, (float) (n4 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 76, builtGlow.colorOffset());
    }
    
    private SimpleFramebuffer ensureShapeTarget(int n2, int n3) {
        while (shapeTargets.isEmpty()) {
            shapeTargets.add(null);
        }
        SimpleFramebuffer simpleFramebuffer = ensureTarget(shapeTargets.getFirst(), "meteor_glow_shape_" + 0, n2, n3);
        shapeTargets.set(0, simpleFramebuffer);
        return simpleFramebuffer;
    }
    
    private void renderShape(int n, BuiltGlow builtGlow, SimpleFramebuffer simpleFramebuffer) {
        GpuBuffer gpuBuffer = ensureFullscreenQuadBuffer();
        if (gpuBuffer == null || simpleFramebuffer == null) {
            return;
        }
        GpuBuffer gpuBuffer2;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.calloc(1312);
            GlowRenderer.writeGlowParams(byteBuffer, 0, builtGlow, 0.0f, 0.0f, 1.0f, 1.0f, builtGlow.effectivePad());
            byteBuffer.putFloat(48, builtGlow.splitIndex());
            GlowRenderer.writeShapeSpans(byteBuffer, builtGlow, builtGlow.effectivePad());
            GlowRenderer.writePalette(byteBuffer, builtGlow);
            GlowRenderer.writeCornerColors(byteBuffer, builtGlow);
            byteBuffer.position(0);
            gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "meteor_glow_shape_params_" + n, 136, byteBuffer);
        } catch (RuntimeException runtimeException) {
            return;
        }
        try {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode) VertexFormat.DrawMode.QUADS);
            GpuBuffer gpuBuffer3 = shapeIndexBuffer.getIndexBuffer(6);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_glow_shape_" + n, simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty());) {
                renderPass.setPipeline(GLOW_SHAPE_PIPELINE);
                renderPass.setUniform("GlowParamsArray", gpuBuffer2);
                GpuBuffer gpuBuffer4 = Stubs.ClientSplits.buffer();
                if (gpuBuffer4 != null) {
                    renderPass.setUniform("SplitParams", gpuBuffer4);
                }
                renderPass.setVertexBuffer(0, gpuBuffer);
                renderPass.setIndexBuffer(gpuBuffer3, shapeIndexBuffer.getIndexType());
                renderPass.drawIndexed(0, 0, 6, 1);
            }
        } finally {
            if (gpuBuffer2 != null) {
                gpuBuffer2.close();
            }
        }
    }
    
    private static void writePalette(ByteBuffer byteBuffer, BuiltGlow builtGlow) {
        int n = 1120;
        int n6 = 1;
        int[] nArray = Stubs.ClientPalette.colors();
        byteBuffer.putFloat(n, n6);
        byteBuffer.putFloat(n + 4, Stubs.ClientPalette.phase());
        byteBuffer.putFloat(n + 8, Stubs.ClientPalette.styleId());
        byteBuffer.putFloat(n + 12, Stubs.GradientSweep.progress());
        int n7 = n + 112;
        byteBuffer.putFloat(n7, Stubs.ClientPalette.prevStyle());
        byteBuffer.putFloat(n7 + 4, Stubs.ClientPalette.closed());
        for (int i = 0; i < n6 && i < nArray.length; ++i) {
            int n8 = nArray[i] & 0xFFFFFF;
            int n9 = n + (1 + i) * 4 * 4;
            byteBuffer.putFloat(n9, (float) (n8 >>> 16 & 0xFF) / 255.0f);
            byteBuffer.putFloat(n9 + 4, (float) (n8 >>> 8 & 0xFF) / 255.0f);
            byteBuffer.putFloat(n9 + 8, (float) (n8 & 0xFF) / 255.0f);
            byteBuffer.putFloat(n9 + 12, 1.0f);
        }
    }
    
    private static void writeCornerColors(ByteBuffer byteBuffer, BuiltGlow builtGlow) {
        int n = CORNER_COLOR_BASE_VEC4 * 16;
        GlowRenderer.putColor(byteBuffer, n, builtGlow.colorTopLeft());
        GlowRenderer.putColor(byteBuffer, n + 16, builtGlow.colorTopRight());
        GlowRenderer.putColor(byteBuffer, n + 32, builtGlow.colorBottomRight());
        GlowRenderer.putColor(byteBuffer, n + 48, builtGlow.colorBottomLeft());
    }
    
    private static void putColor(ByteBuffer byteBuffer, int offset, int color) {
        byteBuffer.putFloat(offset, (float) (color >>> 16 & 0xFF) / 255.0f);
        byteBuffer.putFloat(offset + 4, (float) (color >>> 8 & 0xFF) / 255.0f);
        byteBuffer.putFloat(offset + 8, (float) (color & 0xFF) / 255.0f);
        byteBuffer.putFloat(offset + 12, (float) (color >>> 24 & 0xFF) / 255.0f);
    }
    
    private int packTiles(float effectivePad) {
        int n2;
        int n3 = pendingGlows.size();
        if (tileX.length < n3) {
            tileX = new int[n3];
            tileY = new int[n3];
            tileW = new int[n3];
            tileH = new int[n3];
        }
        
        float f = Math.max(8.0f, effectivePad);
        int n4 = n2 = Math.max(8, Math.round(f));
        int n5 = n2;
        int n6 = 0;
        for (int i = 0; i < n3; ++i) {
            BuiltGlow builtGlow = pendingGlows.get(i).glow();
            int n7 = Math.max(Math.round((builtGlow.width() + effectivePad * 2.0f)), 1);
            int n8 = Math.max(Math.round((builtGlow.height() + effectivePad * 2.0f)), 1);
            if (n4 + n7 + n2 > 2048) {
                n4 = n2;
                n5 += n6 + n2;
                n6 = 0;
            }
            if (n7 + n2 * 2 > 2048 || n5 + n8 + n2 > 2048) {
                tileW[i] = 0;
                tileH[i] = 0;
                continue;
            }
            tileX[i] = n4;
            tileY[i] = n5;
            tileW[i] = n7;
            tileH[i] = n8;
            n4 += n7 + n2;
            n6 = Math.max(n6, n8);
        }
        return n5 + n6 + n2;
    }
    
    public GlowCapture getGlobalCapture() {
        return globalCapture;
    }
    
    public record PendingGlow(BuiltGlow glow, GlowCapture capture, Matrix3x2f pose) {
    
    }
    
    // temporary
    public static final class Stubs {
        
        public static final class ClientPalette {
            
            private static final int[] COLORS = new int[] { 0xFFFFFFFF, 0xFFFF0000, 0xFFFF0000, 0xFFFFFFFF, 0xFFFF0000, 0xFFFFFFFF };
            
            public static int count() {
                return 1;
            }
            
            public static int[] colors() {
                return COLORS;
            }
            
            public static float phase() {
                return 0.0f;
            }
            
            public static float styleId() {
                return 0.0f;
            }
            
            public static float prevStyle() {
                return 0.0f;
            }
            
            public static float closed() {
                return 0.0f;
            }
            
        }
        
        public static final class ClientSplits {
            
            public static GpuBuffer buffer() {
                return null;
            }
            
        }
        
        public static final class GradientSweep {
            
            public static float progress() {
                return -1.0f;
            }
            
        }
        
    }
    
}
