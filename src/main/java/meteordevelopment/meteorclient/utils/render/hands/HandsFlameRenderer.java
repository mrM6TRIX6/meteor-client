package meteordevelopment.meteorclient.utils.render.hands;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.RenderSampler;
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class HandsFlameRenderer {
    
    private static final Identifier TRAIL_PIPELINE_ID = MeteorClient.identifier("pipeline/effects/hands_flame_trail");
    private static final Identifier COMPOSITE_PIPELINE_ID = MeteorClient.identifier("pipeline/effects/hands_flame_composite");
    private static final Identifier FULLSCREEN_VERTEX_SHADER = MeteorClient.identifier("effects/hands_flame/fullscreen");
    private static final Identifier TRAIL_FRAGMENT_SHADER = MeteorClient.identifier("effects/hands_flame/trail");
    private static final Identifier COMPOSITE_FRAGMENT_SHADER = MeteorClient.identifier("effects/hands_flame/composite");
    
    private static final int UNIFORM_BYTES = 64;
    
    private static boolean flameEnabled;
    private static float flameStrength = 0.85f;
    private static float flameRiseSpeed;
    private static float flameWobble = 0.65f;
    private static float flameLength = 0.95f;
    private static float flameBrightness = 0.9f;
    private static int flameColorMode;
    private static int flameColor = ColorUtil.rgba(255, 255, 255, 230);
    private static boolean flameItemsOnly;
    
    private static RenderPipeline trailPipeline;
    private static RenderPipeline compositePipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer dataBuffer;
    private static GpuTexture beforeTexture;
    private static GpuTexture sceneTexture;
    private static GpuTexture trailTextureA;
    private static GpuTexture trailTextureB;
    private static GpuTextureView beforeTextureView;
    private static GpuTextureView sceneTextureView;
    private static GpuTextureView trailTextureViewA;
    private static GpuTextureView trailTextureViewB;
    private static boolean useTrailAAsHistory = true;
    private static boolean capturedBeforeHands;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    private static boolean disabledAfterError;
    
    private HandsFlameRenderer() {
    }
    
    public static void captureBeforeHandRender() {
        capturedBeforeHands = false;
        if (!shouldRenderFlame()) {
            return;
        }
        
        Framebuffer target = MinecraftClient.getInstance().getFramebuffer();
        if (!isUsable(target)) {
            return;
        }
        
        if (!ensureReady(target.textureWidth, target.textureHeight)) {
            return;
        }
        
        try {
            RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToTexture(target.getColorAttachment(), beforeTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
            capturedBeforeHands = true;
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
        
        Framebuffer target = MinecraftClient.getInstance().getFramebuffer();
        if (!isUsable(target) || !ensureReady(target.textureWidth, target.textureHeight)) {
            return;
        }
        
        try {
            writeUniforms(target.textureWidth, target.textureHeight);
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(0, dataBuffer.remaining()), dataBuffer);
            renderTrail(encoder, target);
            swapTrailHistory();
            encoder.copyTextureToTexture(target.getColorAttachment(), sceneTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
            renderComposite(encoder, target);
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }
    
    public static boolean hasCapturedHands() {
        return capturedBeforeHands;
    }
    
    public static void resetTrail() {
        capturedBeforeHands = false;
        if (trailTextureA == null || trailTextureB == null) {
            return;
        }
        
        try {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.clearColorTexture(trailTextureA, 0);
            encoder.clearColorTexture(trailTextureB, 0);
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }
    
    public static void shutdown() {
        closeTextures();
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
        capturedBeforeHands = false;
    }
    
    public static void setFlameEnabled(boolean enabled) {
        flameEnabled = enabled;
        if (!enabled) {
            resetTrail();
        }
    }
    
    public static void configure(float strength, float riseSpeed, float wobble, float length, float brightness, int colorMode, int color, boolean itemsOnly) {
        flameStrength = clamp(strength, 0.0f, 2.0f);
        flameRiseSpeed = clamp(riseSpeed, 0.0f, 2.0f);
        flameWobble = clamp(wobble, 0.0f, 2.0f);
        flameLength = clamp(length, 0.1f, 2.5f);
        flameBrightness = clamp(brightness, 0.0f, 2.0f);
        flameColorMode = Math.max(0, Math.min(2, colorMode));
        flameColor = color;
        flameItemsOnly = itemsOnly;
    }
    
    public static boolean shouldRenderFlame() {
        if (!flameEnabled || disabledAfterError) {
            return false;
        }
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.world == null) {
            return false;
        }
        return !flameItemsOnly || !minecraft.player.getMainHandStack().isEmpty() || !minecraft.player.getOffHandStack().isEmpty();
    }
    
    private static boolean ensureReady(int width, int height) {
        if (trailPipeline == null || compositePipeline == null || uniformBuffer == null || dummyVertexBuffer == null || dataBuffer == null) {
            initPipelines();
        }
        ensureTextures(width, height);
        return trailPipeline != null
            && compositePipeline != null
            && uniformBuffer != null
            && dummyVertexBuffer != null
            && dataBuffer != null
            && beforeTexture != null
            && sceneTexture != null
            && trailTextureA != null
            && trailTextureB != null
            && beforeTextureView != null
            && sceneTextureView != null
            && trailTextureViewA != null
            && trailTextureViewB != null;
    }
    
    private static void initPipelines() {
        try {
            trailPipeline = RenderPipeline.builder()
                .withLocation(TRAIL_PIPELINE_ID)
                .withVertexShader(FULLSCREEN_VERTEX_SHADER)
                .withFragmentShader(TRAIL_FRAGMENT_SHADER)
                .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                .withUniform("HandsFlameData", UniformType.UNIFORM_BUFFER)
                .withSampler("BeforeSampler")
                .withSampler("AfterSampler")
                .withSampler("PrevTrailSampler")
                .withSampler("DepthSampler")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();
            compositePipeline = RenderPipeline.builder()
                .withLocation(COMPOSITE_PIPELINE_ID)
                .withVertexShader(FULLSCREEN_VERTEX_SHADER)
                .withFragmentShader(COMPOSITE_FRAGMENT_SHADER)
                .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                .withUniform("HandsFlameData", UniformType.UNIFORM_BUFFER)
                .withSampler("SceneSampler")
                .withSampler("BeforeSampler")
                .withSampler("TrailSampler")
                .withSampler("DepthSampler")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();
            
            dataBuffer = MemoryUtil.memAlloc(UNIFORM_BYTES);
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor-client:hands_flame_uniform",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            ByteBuffer dummyData = MemoryUtil.memAlloc(4);
            dummyData.putInt(0);
            dummyData.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor-client:hands_flame_dummy_vertex",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                dummyData
            );
            MemoryUtil.memFree(dummyData);
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }
    
    private static void ensureTextures(int width, int height) {
        if (beforeTexture != null && width == lastWidth && height == lastHeight) {
            return;
        }
        
        closeTextures();
        int trailWidth = width;
        int trailHeight = height;
        beforeTexture = createTexture("meteor-client:hands_flame_before", width, height, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING);
        sceneTexture = createTexture("meteor-client:hands_flame_scene", width, height, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING);
        trailTextureA = createTexture("meteor-client:hands_flame_trail_a", trailWidth, trailHeight, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT);
        trailTextureB = createTexture("meteor-client:hands_flame_trail_b", trailWidth, trailHeight, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT);
        beforeTextureView = RenderSystem.getDevice().createTextureView(beforeTexture);
        sceneTextureView = RenderSystem.getDevice().createTextureView(sceneTexture);
        trailTextureViewA = RenderSystem.getDevice().createTextureView(trailTextureA);
        trailTextureViewB = RenderSystem.getDevice().createTextureView(trailTextureB);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(trailTextureA, 0);
        encoder.clearColorTexture(trailTextureB, 0);
        useTrailAAsHistory = true;
        lastWidth = width;
        lastHeight = height;
    }
    
    private static GpuTexture createTexture(String label, int width, int height, int usage) {
        return RenderSystem.getDevice().createTexture(
            () -> label,
            usage,
            TextureFormat.RGBA8,
            width,
            height,
            1,
            1
        );
    }
    
    private static void writeUniforms(int width, int height) {
        int color = flameColor;
        float alpha = ColorUtil.getAlpha(color) / 255.0f;
        dataBuffer.clear();
        dataBuffer.putFloat(ColorUtil.getRed(color) / 255.0f);
        dataBuffer.putFloat(ColorUtil.getGreen(color) / 255.0f);
        dataBuffer.putFloat(ColorUtil.getBlue(color) / 255.0f);
        dataBuffer.putFloat(alpha);
        dataBuffer.putFloat(flameStrength);
        dataBuffer.putFloat(flameRiseSpeed);
        dataBuffer.putFloat(flameWobble);
        dataBuffer.putFloat(flameLength);
        dataBuffer.putFloat(flameBrightness);
        dataBuffer.putFloat(flameTime());
        dataBuffer.putFloat(flameColorMode + (flameItemsOnly ? 10.0f : 0.0f));
        dataBuffer.putFloat(alpha);
        dataBuffer.putFloat(width);
        dataBuffer.putFloat(height);
        dataBuffer.putFloat(1.0f / Math.max(width, 1));
        dataBuffer.putFloat(1.0f / Math.max(height, 1));
        dataBuffer.flip();
    }
    
    private static void renderTrail(CommandEncoder encoder, Framebuffer target) {
        try (var renderPass = encoder.createRenderPass(
            () -> "meteor-client:hands_flame_trail",
            nextTrailView(),
            OptionalInt.empty(),
            null,
            OptionalDouble.empty()
        )) {
            renderPass.setPipeline(trailPipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("BeforeSampler", beforeTextureView, RenderSampler.linear());
            renderPass.bindTexture("AfterSampler", target.getColorAttachmentView(), RenderSampler.linear());
            renderPass.bindTexture("PrevTrailSampler", historyTrailView(), RenderSampler.linear());
            renderPass.bindTexture("DepthSampler", target.getDepthAttachmentView(), RenderSampler.nearest());
            renderPass.setUniform("HandsFlameData", uniformBuffer.slice());
            renderPass.draw(0, 6);
        }
    }
    
    private static void renderComposite(CommandEncoder encoder, Framebuffer target) {
        try (var renderPass = encoder.createRenderPass(
            () -> "meteor-client:hands_flame_composite",
            target.getColorAttachmentView(),
            OptionalInt.empty(),
            null,
            OptionalDouble.empty()
        )) {
            renderPass.setPipeline(compositePipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("SceneSampler", sceneTextureView, RenderSampler.linear());
            renderPass.bindTexture("BeforeSampler", beforeTextureView, RenderSampler.linear());
            renderPass.bindTexture("TrailSampler", historyTrailView(), RenderSampler.linear());
            renderPass.bindTexture("DepthSampler", target.getDepthAttachmentView(), RenderSampler.nearest());
            renderPass.setUniform("HandsFlameData", uniformBuffer.slice());
            renderPass.draw(0, 6);
        }
    }
    
    private static GpuTextureView historyTrailView() {
        return useTrailAAsHistory ? trailTextureViewA : trailTextureViewB;
    }
    
    private static GpuTextureView nextTrailView() {
        return useTrailAAsHistory ? trailTextureViewB : trailTextureViewA;
    }
    
    private static void swapTrailHistory() {
        useTrailAAsHistory = !useTrailAAsHistory;
    }
    
    private static boolean isUsable(Framebuffer target) {
        return target != null
            && target.getColorAttachment() != null
            && target.getColorAttachmentView() != null
            && target.getDepthAttachmentView() != null
            && target.textureWidth > 0
            && target.textureHeight > 0;
    }
    
    private static float flameTime() {
        return (System.nanoTime() % 180_000_000_000L) / 1_000_000_000.0f;
    }
    
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    private static void disableAfterError(Throwable throwable) {
        disabledAfterError = true;
        throwable.printStackTrace();
        shutdown();
    }
    
    private static void closeTextures() {
        if (beforeTextureView != null) {
            beforeTextureView.close();
        }
        if (sceneTextureView != null) {
            sceneTextureView.close();
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
        if (trailTextureA != null) {
            trailTextureA.close();
        }
        if (trailTextureB != null) {
            trailTextureB.close();
        }
        beforeTextureView = null;
        sceneTextureView = null;
        trailTextureViewA = null;
        trailTextureViewB = null;
        beforeTexture = null;
        sceneTexture = null;
        trailTextureA = null;
        trailTextureB = null;
        lastWidth = -1;
        lastHeight = -1;
    }
    
}
