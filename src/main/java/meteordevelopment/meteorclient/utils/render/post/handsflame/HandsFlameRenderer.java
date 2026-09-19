package meteordevelopment.meteorclient.utils.render.post.handsflame;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.IMinecraft;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import meteordevelopment.meteorclient.utils.render.post.shaderhands.ShaderHandsRenderer;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class HandsFlameRenderer implements IMinecraft {
    
    private static final float TRAIL_STEP_SECONDS = 0.008333334f;
    private static final int UNIFORM_BYTES = 144; // 9 * vec4 (includes flameExtra)
    private static final int IRIS_CAPTURE_SLOTS = 2;
    
    private static final Identifier TRAIL_PIPELINE_ID = MeteorClient.identifier("pipeline/effects/hands_flame_trail");
    private static final Identifier COMPOSITE_PIPELINE_ID = MeteorClient.identifier("pipeline/effects/hands_flame_composite");
    private static final Identifier RESTORE_PIPELINE_ID = MeteorClient.identifier("pipeline/effects/hands_flame_restore");
    private static final Identifier FULLSCREEN_VERTEX_SHADER = MeteorClient.identifier("effects/hands_flame/fullscreen");
    private static final Identifier TRAIL_FRAGMENT_SHADER = MeteorClient.identifier("effects/hands_flame/trail");
    private static final Identifier COMPOSITE_FRAGMENT_SHADER = MeteorClient.identifier("effects/hands_flame/composite");
    private static final Identifier RESTORE_FRAGMENT_SHADER = MeteorClient.identifier("effects/hands_flame/restore");
    
    private static boolean flameEnabled;
    private static float flameStrength;
    private static float flameRiseSpeed;
    private static float flameWobble;
    private static float flameLength;
    private static float flameBrightness;
    private static int flameColorMode;
    private static int flameColor;
    private static boolean flameItemsOnly;
    private static boolean flameClientGradient;
    
    private static RenderPipeline trailPipeline;
    private static RenderPipeline compositePipeline;
    private static RenderPipeline restorePipeline;
    
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer dataBuffer;
    private static GpuTexture beforeTexture;
    private static GpuTexture sceneTexture;
    private static GpuTexture handTexture;
    private static GpuTexture trailTextureA;
    private static GpuTexture trailTextureB;
    
    private static GpuTextureView beforeTextureView;
    private static GpuTextureView sceneTextureView;
    private static GpuTextureView handTextureView;
    private static GpuTextureView trailTextureViewA;
    private static GpuTextureView trailTextureViewB;
    
    private static final GpuTexture[] irisDepthBeforeTextures;
    private static final GpuTexture[] irisDepthAfterTextures;
    private static final GpuTextureView[] irisDepthBeforeTextureViews;
    private static final GpuTextureView[] irisDepthAfterTextureViews;
    
    private static boolean useTrailAAsHistory;
    private static boolean capturedBeforeHands;
    
    private static int irisCaptureCount;
    private static int irisOpenCaptureSlot;
    private static int lastWidth;
    private static int lastHeight;
    private static int lastIrisDepthWidth;
    private static int lastIrisDepthHeight;
    
    private static TextureFormat lastIrisDepthFormat;
    private static RenderRegion lastRenderRegion;
    private static boolean disabledAfterError;
    private static final float[] protectedTagBounds;
    private static GpuTexture blackTexture;
    private static GpuTextureView blackTextureView;
    private static long trailClockNs;
    private static float trailAccumulator;
    private static float pendingTrailStep;
    private static float smoothR;
    private static float smoothG;
    private static float smoothB;
    
    private HandsFlameRenderer() {}
    
    static {
        flameStrength = 0.85f;
        flameWobble = 0.65f;
        flameLength = 0.95f;
        flameBrightness = 0.9f;
        flameColor = ColorUtil.rgba(255, 255, 255, 230);
        irisDepthBeforeTextures = new GpuTexture[2];
        irisDepthAfterTextures = new GpuTexture[2];
        irisDepthBeforeTextureViews = new GpuTextureView[2];
        irisDepthAfterTextureViews = new GpuTextureView[2];
        useTrailAAsHistory = true;
        irisOpenCaptureSlot = -1;
        lastWidth = -1;
        lastHeight = -1;
        lastIrisDepthWidth = -1;
        lastIrisDepthHeight = -1;
        protectedTagBounds = new float[512];
        smoothR = -1.0f;
    }
    
    public static void shutdown() {
        closeTextures();
        closeBlackTexture();
        
        if (uniformBuffer != null) {
            uniformBuffer.close();
        }
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
        }
        if (dataBuffer != null) {
            MemoryUtil.memFree(dataBuffer);
        }
        
        uniformBuffer = null;
        dummyVertexBuffer = null;
        dataBuffer = null;
        trailPipeline = null;
        compositePipeline = null;
        restorePipeline = null;
        capturedBeforeHands = false;
    }
    
    public record RenderRegion(int x, int y, int width, int height) { }
    
    private static int channel(int n, int n2) {
        return n >>> n2 & 0xFF;
    }
    
    public static void configure(float f, float f2, float f3, float f4, float f5, int n, int n2, boolean bl, boolean bl2) {
        flameStrength = f;
        flameRiseSpeed = f2;
        flameWobble = f3;
        flameLength = f4;
        flameBrightness = f5;
        flameColorMode = n;
        flameColor = n2;
        flameItemsOnly = bl;
        flameClientGradient = bl2;
    }
    
    public static void renderIrisCapturedHandsFlame() {
        int n = irisCaptureCount;
        
        irisCaptureCount = 0;
        irisOpenCaptureSlot = -1;
        
        if (n <= 0 || !shouldRenderFlame()) {
            return;
        }
        
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!isUsable(framebuffer) || !ensureReady(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        
        try {
            RenderRegion renderRegion = flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            ensureRenderRegion(renderRegion);
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), handTexture, 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
            boolean bl2 = advanceTrailClock();
            writeUniforms(framebuffer.textureWidth, framebuffer.textureHeight, true, bl2 ? consumeTrailStep() : 0.0f);
            commandEncoder.writeToBuffer(uniformBuffer.slice(0L, (long) dataBuffer.remaining()), dataBuffer);
            
            int n2 = -1;
            
            for (int i = 0; i < Math.min(n, 2); ++i) {
                if (irisDepthBeforeTextureViews[i] == null || irisDepthAfterTextureViews[i] == null) {
                    continue;
                }
                
                if (bl2) {
                    renderTrail(commandEncoder, framebuffer, irisDepthAfterTextureViews[i], irisDepthBeforeTextureViews[i], handTextureView, handTextureView);
                    swapTrailHistory();
                }
                
                n2 = i;
            }
            
            if (n2 >= 0) {
                renderComposite(commandEncoder, framebuffer, irisDepthAfterTextureViews[n2], irisDepthBeforeTextureViews[n2], handTextureView, handTextureView, handTextureView);
                restoreProtectedNameTags(commandEncoder, framebuffer, handTextureView);
            }
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }
    
    private static void ensureRenderRegion(RenderRegion renderRegion) {
        if (renderRegion.equals(lastRenderRegion)) {
            return;
        }
        
        lastRenderRegion = renderRegion;
        resetTrail();
    }
    
    public static boolean shouldRenderFlame() {
        if (disabledAfterError) {
            return false;
        }
        
        if (!Utils.canUpdate()) {
            return false;
        }
        
        return flameEnabled;
    }
    
    private static boolean holdingFlameItem() {
        if (!Utils.canUpdate()) {
            return false;
        }
        return !mc.player.getMainHandStack().isEmpty() || !mc.player.getOffHandStack().isEmpty();
    }
    
    private static boolean shouldInjectFlameSource() {
        return !flameItemsOnly || holdingFlameItem();
    }
    
    private static boolean ensureReady(int n, int n2) {
        if (trailPipeline == null || compositePipeline == null || restorePipeline == null || uniformBuffer == null || dummyVertexBuffer == null || dataBuffer == null) {
            initPipelines();
        }
        ensureTextures(n, n2);
        return trailPipeline != null && compositePipeline != null && restorePipeline != null && uniformBuffer != null && dummyVertexBuffer != null && dataBuffer != null && beforeTexture != null && sceneTexture != null && trailTextureA != null && trailTextureB != null && beforeTextureView != null && sceneTextureView != null && trailTextureViewA != null && trailTextureViewB != null;
    }
    
    private static void closeBlackTexture() {
        if (blackTextureView != null) {
            blackTextureView.close();
            blackTextureView = null;
        }
        
        if (blackTexture != null) {
            blackTexture.close();
            blackTexture = null;
        }
    }
    
    private static void writeUniforms(int n, int n2, boolean bl, float f) {
        int n3 = flameColor;
        float f2 = (float) ColorUtil.getAlpha(n3) / 255.0f;
        float f3 = (float) channel(n3, 16) / 255.0f;
        float f4 = (float) channel(n3, 8) / 255.0f;
        float f5 = (float) channel(n3, 0) / 255.0f;
        
        if (smoothR < 0.0f) {
            smoothR = f3;
            smoothG = f4;
            smoothB = f5;
        } else {
            float f6 = 1.0f - (float) Math.exp(-f * 10.0f);
            smoothR += (f3 - smoothR) * f6;
            smoothG += (f4 - smoothG) * f6;
            smoothB += (f5 - smoothB) * f6;
        }
        
        dataBuffer.clear();
        dataBuffer.putFloat(smoothR);
        dataBuffer.putFloat(smoothG);
        dataBuffer.putFloat(smoothB);
        dataBuffer.putFloat(f);
        dataBuffer.putFloat(flameStrength);
        dataBuffer.putFloat(flameRiseSpeed);
        dataBuffer.putFloat(flameWobble);
        dataBuffer.putFloat(flameLength);
        dataBuffer.putFloat(flameBrightness);
        dataBuffer.putFloat(flameTime());
        dataBuffer.putFloat((float) flameColorMode + (flameItemsOnly ? 10.0f : 0.0f) + (bl ? 20.0f : 0.0f));
        dataBuffer.putFloat(f2);
        dataBuffer.putFloat(n);
        dataBuffer.putFloat(n2);
        dataBuffer.putFloat(1.0f / (float) Math.max(n, 1));
        dataBuffer.putFloat(1.0f / (float) Math.max(n2, 1));
        
        int packed = flameColor | 0xFF000000;
        putGradientColor(packed);
        putGradientColor(ColorUtil.lerpColor(packed, -1, 0.18f));
        putGradientColor(packed);
        putGradientColor(ColorUtil.lerpColor(packed, 0xFF000000, 0.35f));
        // flameExtra: x unused, y client-gradient flag, z source gate (OnlyWithItem fade), w reserved
        dataBuffer.putFloat(0.0f);
        dataBuffer.putFloat(flameClientGradient ? 1.0f : 0.0f);
        dataBuffer.putFloat(shouldInjectFlameSource() ? 1.0f : 0.0f);
        dataBuffer.putFloat(0.0f);
        dataBuffer.flip();
    }
    
    public static boolean hasCapturedHands() {
        return capturedBeforeHands;
    }
    
    private static float consumeTrailStep() {
        return pendingTrailStep;
    }
    
    private static void swapTrailHistory() {
        useTrailAAsHistory = !useTrailAAsHistory;
    }
    
    private static void restoreProtectedNameTags(CommandEncoder commandEncoder, Framebuffer framebuffer, GpuTextureView gpuTextureView) {
        int n = 0;
        float f = Render2D.width();
        float f2 = Render2D.height();
        
        if (n <= 0 || f <= 0.0f || f2 <= 0.0f || restorePipeline == null) {
            return;
        }
        
        float f3 = (float) framebuffer.textureWidth / f;
        float f4 = (float) framebuffer.textureHeight / f2;
        
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_hands_flame_restore_nametags", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty());) {
            renderPass.setPipeline(restorePipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("SceneSampler", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            for (int i = 0; i < n; ++i) {
                int n2 = i * 4;
                int n3 = Math.max(0, (int) Math.floor(protectedTagBounds[n2] * f3));
                int n4 = Math.max(0, (int) Math.floor(protectedTagBounds[n2 + 1] * f4));
                int n5 = Math.min(framebuffer.textureWidth, (int) Math.ceil((protectedTagBounds[n2] + protectedTagBounds[n2 + 2]) * f3));
                int n6 = Math.min(framebuffer.textureHeight, (int) Math.ceil((protectedTagBounds[n2 + 1] + protectedTagBounds[n2 + 3]) * f4));
                if (n5 <= n3 || n6 <= n4) {
                    continue;
                }
                renderPass.enableScissor(n3, framebuffer.textureHeight - n6, n5 - n3, n6 - n4);
                renderPass.draw(0, 6);
            }
        }
    }
    
    private static void ensureTextures(int n, int n2) {
        if (beforeTexture != null && n == lastWidth && n2 == lastHeight) {
            return;
        }
        
        closeTextures();
        int n3 = Math.max(1, n / 3);
        int n4 = Math.max(1, n2 / 3);
        
        beforeTexture = createTexture("meteor_hands_flame_before", n, n2, 7);
        sceneTexture = createTexture("meteor_hands_flame_scene", n, n2, 5);
        handTexture = createTexture("meteor_hands_flame_hand_temp", n, n2, 5);
        trailTextureA = createTexture("meteor_hands_flame_trail_a", n3, n4, 13);
        trailTextureB = createTexture("meteor_hands_flame_trail_b", n3, n4, 13);
        beforeTextureView = RenderSystem.getDevice().createTextureView(beforeTexture);
        sceneTextureView = RenderSystem.getDevice().createTextureView(sceneTexture);
        handTextureView = RenderSystem.getDevice().createTextureView(handTexture);
        trailTextureViewA = RenderSystem.getDevice().createTextureView(trailTextureA);
        trailTextureViewB = RenderSystem.getDevice().createTextureView(trailTextureB);
        
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.clearColorTexture(trailTextureA, 0);
        commandEncoder.clearColorTexture(trailTextureB, 0);
        useTrailAAsHistory = true;
        lastWidth = n;
        lastHeight = n2;
    }
    
    private static boolean ensureBlackTexture() {
        if (blackTexture != null && blackTextureView != null) {
            return true;
        }
        
        blackTexture = RenderSystem.getDevice().createTexture(() -> "meteor_hands_flame_black", 13, TextureFormat.RGBA8, 1, 1, 1, 1);
        blackTextureView = RenderSystem.getDevice().createTextureView(blackTexture);
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(blackTexture, 0);
        
        return blackTextureView != null;
    }
    
    private static void ensureIrisDepthTextures(Framebuffer framebuffer) {
        GpuTexture gpuTexture = framebuffer.getDepthAttachment();
        TextureFormat textureFormat = gpuTexture.getFormat();
        
        if (irisDepthBeforeTextures[0] != null && framebuffer.textureWidth == lastIrisDepthWidth && framebuffer.textureHeight == lastIrisDepthHeight && textureFormat == lastIrisDepthFormat) {
            return;
        }
        
        closeIrisDepthTextures();
        
        for (int i = 0; i < 2; ++i) {
            irisDepthBeforeTextures[i] = RenderSystem.getDevice().createTexture(() -> "meteor_hands_flame_iris_depth_before", 5, textureFormat, framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
            irisDepthAfterTextures[i] = RenderSystem.getDevice().createTexture(() -> "meteor_hands_flame_iris_depth_after", 5, textureFormat, framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
            irisDepthBeforeTextureViews[i] = RenderSystem.getDevice().createTextureView(irisDepthBeforeTextures[i]);
            irisDepthAfterTextureViews[i] = RenderSystem.getDevice().createTextureView(irisDepthAfterTextures[i]);
        }
        
        lastIrisDepthWidth = framebuffer.textureWidth;
        lastIrisDepthHeight = framebuffer.textureHeight;
        lastIrisDepthFormat = textureFormat;
    }
    
    private static void renderTrail(CommandEncoder commandEncoder, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuTextureView gpuTextureView4) {
        GpuTextureView gpuTextureView5 = ShaderHandsRenderer.handBoundsLeftView();
        GpuTextureView gpuTextureView6 = ShaderHandsRenderer.handBoundsRightView();
        
        if (gpuTextureView5 == null || gpuTextureView6 == null) {
            if (!ensureBlackTexture()) {
                return;
            }
            gpuTextureView5 = blackTextureView;
            gpuTextureView6 = blackTextureView;
        }
        
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_hands_flame_trail", nextTrailView(), OptionalInt.empty(), null, OptionalDouble.empty());) {
            renderPass.setPipeline(trailPipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("BeforeSampler", gpuTextureView3, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("AfterSampler", gpuTextureView4, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("PrevTrailSampler", historyTrailView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("DepthSampler", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.bindTexture("NoHandDepthSampler", gpuTextureView2, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.bindTexture("BoundsTexL", gpuTextureView5, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.bindTexture("BoundsTexR", gpuTextureView6, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.setUniform("HandsFlameData", uniformBuffer.slice());
            renderPass.draw(0, 6);
        }
    }
    
    private static RenderRegion flameRegion(int n, int n2) {
        return new RenderRegion(0, 0, n, n2);
    }
    
    private static boolean advanceTrailClock() {
        long l = System.nanoTime();
        
        if (trailClockNs != 0L) {
            trailAccumulator += Math.clamp((float) (l - trailClockNs) / 1.0E9f, 0.0f, 0.1f);
        }
        
        trailClockNs = l;
        
        if (trailAccumulator >= TRAIL_STEP_SECONDS) {
            pendingTrailStep = Math.min(trailAccumulator, 0.05f);
            trailAccumulator = 0.0f;
            return true;
        }
        
        return false;
    }
    
    private static void closeTextures() {
        if (beforeTextureView != null) {
            beforeTextureView.close();
        }
        if (sceneTextureView != null) {
            sceneTextureView.close();
        }
        if (handTextureView != null) {
            handTextureView.close();
        }
        if (trailTextureViewA != null) {
            trailTextureViewA.close();
        }
        if (trailTextureViewB != null) {
            trailTextureViewB.close();
        }
        if (beforeTexture != null) {
            beforeTexture.close();
        }
        if (sceneTexture != null) {
            sceneTexture.close();
        }
        if (handTexture != null) {
            handTexture.close();
        }
        if (trailTextureA != null) {
            trailTextureA.close();
        }
        if (trailTextureB != null) {
            trailTextureB.close();
        }
        
        closeIrisDepthTextures();
        
        beforeTextureView = null;
        sceneTextureView = null;
        handTextureView = null;
        trailTextureViewA = null;
        trailTextureViewB = null;
        beforeTexture = null;
        sceneTexture = null;
        handTexture = null;
        trailTextureA = null;
        trailTextureB = null;
        lastWidth = -1;
        lastHeight = -1;
        lastRenderRegion = null;
    }
    
    private static void putGradientColor(int n) {
        dataBuffer.putFloat((float) (n >> 16 & 0xFF) / 255.0f);
        dataBuffer.putFloat((float) (n >> 8 & 0xFF) / 255.0f);
        dataBuffer.putFloat((float) (n & 0xFF) / 255.0f);
        dataBuffer.putFloat(1.0f);
    }
    
    private static void initPipelines() {
        try {
            trailPipeline = RenderPipelines.register(
                RenderPipeline.builder()
                    .withLocation(TRAIL_PIPELINE_ID)
                    .withVertexShader(FULLSCREEN_VERTEX_SHADER)
                    .withFragmentShader(TRAIL_FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("HandsFlameData", UniformType.UNIFORM_BUFFER)
                    .withSampler("BeforeSampler")
                    .withSampler("AfterSampler")
                    .withSampler("PrevTrailSampler")
                    .withSampler("DepthSampler")
                    .withSampler("NoHandDepthSampler")
                    .withSampler("BoundsTexL")
                    .withSampler("BoundsTexR")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
            );
            
            compositePipeline = RenderPipelines.register(
                RenderPipeline.builder()
                    .withLocation(COMPOSITE_PIPELINE_ID)
                    .withVertexShader(FULLSCREEN_VERTEX_SHADER)
                    .withFragmentShader(COMPOSITE_FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("HandsFlameData", UniformType.UNIFORM_BUFFER)
                    .withSampler("SceneSampler")
                    .withSampler("BeforeSampler")
                    .withSampler("AfterSampler")
                    .withSampler("TrailSampler")
                    .withSampler("DepthSampler")
                    .withSampler("NoHandDepthSampler")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
            );
            
            restorePipeline = RenderPipelines.register(
                RenderPipeline.builder()
                    .withLocation(RESTORE_PIPELINE_ID)
                    .withVertexShader(FULLSCREEN_VERTEX_SHADER)
                    .withFragmentShader(RESTORE_FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withSampler("SceneSampler")
                    .withoutBlend()
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
            );
            
            dataBuffer = MemoryUtil.memAlloc(UNIFORM_BYTES);
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_hands_flame_uniform",
                136,
                UNIFORM_BYTES
            );
            
            ByteBuffer byteBuffer = MemoryUtil.memAlloc(4);
            byteBuffer.putInt(0);
            byteBuffer.flip();
            
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_hands_flame_dummy_vertex",
                40,
                byteBuffer
            );
            
            MemoryUtil.memFree(byteBuffer);
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }
    
    private static void closeIrisDepthTextures() {
        for (int i = 0; i < 2; ++i) {
            if (irisDepthBeforeTextureViews[i] != null) {
                irisDepthBeforeTextureViews[i].close();
            }
            if (irisDepthAfterTextureViews[i] != null) {
                irisDepthAfterTextureViews[i].close();
            }
            if (irisDepthBeforeTextures[i] != null) {
                irisDepthBeforeTextures[i].close();
            }
            if (irisDepthAfterTextures[i] != null) {
                irisDepthAfterTextures[i].close();
            }
            
            irisDepthBeforeTextureViews[i] = null;
            irisDepthAfterTextureViews[i] = null;
            irisDepthBeforeTextures[i] = null;
            irisDepthAfterTextures[i] = null;
        }
        
        irisCaptureCount = 0;
        irisOpenCaptureSlot = -1;
        lastIrisDepthWidth = -1;
        lastIrisDepthHeight = -1;
        lastIrisDepthFormat = null;
    }
    
    private static void renderComposite(CommandEncoder commandEncoder, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuTextureView gpuTextureView4, GpuTextureView gpuTextureView5) {
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_hands_flame_composite", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
            renderPass.setPipeline(compositePipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("SceneSampler", gpuTextureView5, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("BeforeSampler", gpuTextureView3, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("AfterSampler", gpuTextureView4, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("TrailSampler", historyTrailView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("DepthSampler", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.bindTexture("NoHandDepthSampler", gpuTextureView2, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.setUniform("HandsFlameData", uniformBuffer.slice());
            renderPass.draw(0, 6);
        }
    }
    
    private static GpuTextureView nextTrailView() {
        return useTrailAAsHistory ? trailTextureViewB : trailTextureViewA;
    }
    
    private static GpuTextureView historyTrailView() {
        return useTrailAAsHistory ? trailTextureViewA : trailTextureViewB;
    }
    
    public static void captureAfterHandRender() {
        if (!shouldRenderFlame()) {
            return;
        }
        
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!isUsable(framebuffer) || !ensureReady(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        
        try {
            RenderRegion renderRegion = flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            ensureRenderRegion(renderRegion);
            if (!ShaderHandsRenderer.wasHandCapturedThisFrame()) {
                RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getColorAttachment(), handTexture, 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
            }
            capturedBeforeHands = true;
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }
    
    public static void captureBeforeHandRender() {
        capturedBeforeHands = false;
        if (!shouldRenderFlame()) {
            return;
        }
        
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!isUsable(framebuffer)) {
            return;
        }
        
        if (!ensureReady(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        
        try {
            RenderRegion renderRegion = flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            ensureRenderRegion(renderRegion);
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getColorAttachment(), beforeTexture, 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }
    
    public static void renderCapturedHandsFlame() {
        if (!capturedBeforeHands) {
            return;
        }
        
        capturedBeforeHands = false;
        if (!shouldRenderFlame()) {
            return;
        }
        
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!isUsable(framebuffer) || !ensureReady(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        
        try {
            RenderRegion renderRegion = flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            
            ensureRenderRegion(renderRegion);
            
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            GpuTextureView gpuTextureView = ShaderHandsRenderer.capturedHandColorView();
            GpuTextureView gpuTextureView2 = ShaderHandsRenderer.capturedHandDepthView();
            
            boolean bl2 = gpuTextureView != null && gpuTextureView2 != null && ensureBlackTexture();
            GpuTextureView gpuTextureView3 = bl2 ? blackTextureView : beforeTextureView;
            GpuTextureView gpuTextureView4 = bl2 ? gpuTextureView : handTextureView;
            GpuTextureView gpuTextureView5 = bl2 ? gpuTextureView2 : framebuffer.getDepthAttachmentView();
            
            boolean bl3 = advanceTrailClock();
            writeUniforms(framebuffer.textureWidth, framebuffer.textureHeight, false, bl3 ? consumeTrailStep() : 0.0f);
            commandEncoder.writeToBuffer(uniformBuffer.slice(0L, (long) dataBuffer.remaining()), dataBuffer);
            
            if (bl3) {
                renderTrail(commandEncoder, framebuffer, gpuTextureView5, gpuTextureView5, gpuTextureView3, gpuTextureView4);
                swapTrailHistory();
            }
            
            commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), sceneTexture, 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
            renderComposite(commandEncoder, framebuffer, gpuTextureView5, gpuTextureView5, gpuTextureView3, gpuTextureView4, sceneTextureView);
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }
    
    private static GpuTexture createTexture(String string, int n, int n2, int n3) {
        return RenderSystem.getDevice().createTexture(() -> string, n3, TextureFormat.RGBA8, n, n2, 1, 1);
    }
    
    public static void setFlameEnabled(boolean bl) {
        if (flameEnabled == bl) {
            return;
        }
        flameEnabled = bl;
        if (!bl) {
            resetTrail();
            irisCaptureCount = 0;
            irisOpenCaptureSlot = -1;
        } else {
            // Allow recovery after a previous pipeline/shader failure (e.g. fixed shaders hot-reloaded)
            disabledAfterError = false;
        }
    }
    
    private static void disableAfterError(Throwable throwable) {
        disabledAfterError = true;
        MeteorClient.LOGGER.error("Hands flame renderer failed; disabling effect", throwable);
        shutdown();
    }
    
    public static void beginIrisHandDepthCapture() {
        irisOpenCaptureSlot = -1;
        if (!shouldRenderFlame()) {
            return;
        }
        
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!isUsable(framebuffer) || !ensureReady(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        
        try {
            RenderRegion renderRegion = flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            ensureRenderRegion(renderRegion);
            ensureIrisDepthTextures(framebuffer);
            
            if (irisCaptureCount >= 2) {
                return;
            }
            
            irisOpenCaptureSlot = irisCaptureCount;
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getDepthAttachment(), irisDepthBeforeTextures[irisOpenCaptureSlot], 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
        } catch (Throwable throwable) {
            irisOpenCaptureSlot = -1;
            disableAfterError(throwable);
        }
    }
    
    public static void endIrisHandDepthCapture() {
        int n = irisOpenCaptureSlot;
        irisOpenCaptureSlot = -1;
        
        if (n < 0 || n >= 2) {
            return;
        }
        
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!isUsable(framebuffer) || irisDepthAfterTextures[n] == null) {
            return;
        }
        
        try {
            RenderRegion renderRegion = flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getDepthAttachment(), irisDepthAfterTextures[n], 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
            irisCaptureCount = Math.max(irisCaptureCount, n + 1);
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }
    
    public static void resetTrail() {
        capturedBeforeHands = false;
        
        if (trailTextureA == null || trailTextureB == null) {
            return;
        }
        
        try {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.clearColorTexture(trailTextureA, 0);
            commandEncoder.clearColorTexture(trailTextureB, 0);
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }
    
    private static float flameTime() {
        return (float) (System.nanoTime() % 180000000000L) / 1.0E9f;
    }
    
    private static boolean isUsable(Framebuffer framebuffer) {
        return framebuffer != null && framebuffer.getColorAttachment() != null && framebuffer.getColorAttachmentView() != null && framebuffer.getDepthAttachmentView() != null && framebuffer.textureWidth > 0 && framebuffer.textureHeight > 0;
    }
    
}
