package meteordevelopment.meteorclient.utils.render.post.shaderhands;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.RenderSampler;
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.*;

public final class ShaderHandsRenderer {
    
    private static final int MASK_W = 256;
    private static final int MASK_H = 144;
    private static final int MASK_ALPHA_THRESHOLD = 110;
    private static final float MASK_DEPTH_MIN = 0.2f;
    private static final float MASK_DEPTH_MAX = 2.0f;
    private static final int KAWASE_SIZE = 16;
    private static final int GLASS_SIZE = 32;
    private static final int GLOW_SIZE = 96;
    private static final int GAUSS_SIZE = 224;
    private static final int OUTLINE_SIZE = 96;
    private static final int REDUCE_SIZE = 16;
    private static final int INIT_SIZE = 16;
    private static final int BOUNDS_WRITE_SIZE = 16;
    private static final int PALETTE_BYTES = 1024;
    private static final float KAWASE_OFFSET = 2.6f;
    private static final int KAWASE_STEPS = 4;
    
    private static final Identifier VERTEX = MeteorClient.identifier("post/shaderhands/shaderhands");
    private static final Identifier KAWASE_DOWN_SHADER = MeteorClient.identifier("post/shaderhands/light_kawase_down");
    private static final Identifier KAWASE_UP_SHADER = MeteorClient.identifier("post/shaderhands/light_kawase_up");
    private static final Identifier GLASS_SHADER = MeteorClient.identifier("post/shaderhands/glass");
    private static final Identifier GLOW_DILATE_SHADER = MeteorClient.identifier("post/shaderhands/glow_dilate");
    private static final Identifier GLOW_GAUSS_SHADER = MeteorClient.identifier("post/shaderhands/glow_gauss");
    private static final Identifier OUTLINE_SHADER = MeteorClient.identifier("post/shaderhands/outline");
    private static final Identifier BOUNDS_INIT_SHADER = MeteorClient.identifier("post/shaderhands/bounds_init");
    private static final Identifier BOUNDS_REDUCE_SHADER = MeteorClient.identifier("post/shaderhands/bounds_reduce");
    private static final Identifier BOUNDS_WRITE_SHADER = MeteorClient.identifier("post/shaderhands/bounds_write");
    private static final Identifier PASSTHROUGH_SHADER = MeteorClient.identifier("post/shaderhands/passthrough");
    private static final Identifier HANDMASK_SHADER = MeteorClient.identifier("post/shaderhands/handmask");
    private static final Identifier KAWASE_DOWN_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/kawase_down");
    private static final Identifier KAWASE_UP_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/kawase_up");
    private static final Identifier GLASS_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/glass");
    private static final Identifier GLOW_DILATE_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/glow_dilate");
    private static final Identifier GLOW_GAUSS_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/glow_gauss");
    private static final Identifier OUTLINE_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/outline");
    private static final Identifier OUTLINE_ADD_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/outline_add");
    private static final Identifier BOUNDS_INIT_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/bounds_init");
    private static final Identifier BOUNDS_REDUCE_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/bounds_reduce");
    private static final Identifier BOUNDS_WRITE_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/bounds_write");
    private static final Identifier PASSTHROUGH_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/passthrough");
    private static final Identifier HANDMASK_PIPELINE_ID = MeteorClient.identifier("pipeline/post/shaderhands/handmask");
    
    private static RenderPipeline kawaseDownPipeline;
    private static RenderPipeline kawaseUpPipeline;
    private static RenderPipeline glassPipeline;
    private static RenderPipeline glowDilatePipeline;
    private static RenderPipeline glowGaussPipeline;
    private static RenderPipeline outlinePipeline;
    private static RenderPipeline outlineAddPipeline;
    private static RenderPipeline boundsInitPipeline;
    private static RenderPipeline boundsReducePipeline;
    private static RenderPipeline boundsWritePipeline;
    private static RenderPipeline passthroughPipeline;
    private static RenderPipeline handMaskPipeline;
    
    private static GpuBuffer kawaseBuffer;
    private static GpuBuffer glassBuffer;
    private static GpuBuffer glowBuffer;
    private static GpuBuffer gaussBuffer;
    private static GpuBuffer outlineBuffer;
    private static GpuBuffer reduceBuffer;
    private static GpuBuffer initBuffer;
    private static GpuBuffer boundsWriteBuffer;
    private static GpuBuffer paletteBuffer;
    
    private static final float[] boundsAData = { 1.0f, 1.0f, 0.0f, 0.0f };
    private static final float[] boundsBData = { 1.0f, 1.0f, 0.0f, 0.0f };
    
    private static SimpleFramebuffer maskGrid;
    private static GpuBuffer maskReadBuffer;
    
    private static volatile int[] handMaskCpu;
    private static volatile boolean handMaskValid;
    
    private static final List<SimpleFramebuffer> boundsChain = new ArrayList<>();
    
    private static SimpleFramebuffer boundsUnion;
    private static SimpleFramebuffer boundsLeft;
    private static SimpleFramebuffer boundsRight;
    
    private static GpuTexture sceneCopyTexture;
    private static GpuTextureView sceneCopyTextureView;
    
    private static SimpleFramebuffer sceneA;
    private static SimpleFramebuffer sceneB;
    private static SimpleFramebuffer handFbo;
    private static SimpleFramebuffer glow;
    private static SimpleFramebuffer glowSwap;
    
    private static int targetWidth = -1;
    private static int targetHeight = -1;
    
    private static GpuTextureView prevColorOverride;
    private static GpuTextureView prevDepthOverride;
    
    private static boolean capturing;
    private static boolean sceneReady;
    private static boolean handReady;
    private static boolean disabledAfterError;
    
    private static int[] floodStamp;
    private static int[] floodQueue;
    private static int floodGen;
    private static float[] floodDist;
    private static float[] marchBuf;
    private static final float[] marchPts = new float[8];
    private static boolean handCapturedFrame;
    
    private ShaderHandsRenderer() {}
    
    private static void reset() {
        sceneReady = false;
        handReady = false;
    }
    
    public static void clear() {
        ShaderHandsRenderer.closeTargets();
        ShaderHandsRenderer.closeBuffers();
        ShaderHandsRenderer.reset();
        disabledAfterError = false;
    }
    
    private static void init() {
        if (disabledAfterError) {
            return;
        }
        
        try {
            if (kawaseDownPipeline == null) {
                kawaseDownPipeline = ShaderHandsRenderer.registerKawase(
                    KAWASE_DOWN_PIPELINE_ID,
                    KAWASE_DOWN_SHADER
                );
            }
            
            if (kawaseUpPipeline == null) {
                kawaseUpPipeline = ShaderHandsRenderer.registerKawase(
                    KAWASE_UP_PIPELINE_ID,
                    KAWASE_UP_SHADER
                );
            }
            
            if (glassPipeline == null) {
                glassPipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                        .withLocation(GLASS_PIPELINE_ID)
                        .withVertexShader(VERTEX)
                        .withFragmentShader(GLASS_SHADER)
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withUniform("GlassConfig", UniformType.UNIFORM_BUFFER)
                        .withUniform("PaletteParams", UniformType.UNIFORM_BUFFER)
                        .withSampler("SceneTex")
                        .withSampler("HandTex")
                        .withSampler("BoundsTexL")
                        .withSampler("BoundsTexR")
                        .withBlend(BlendFunction.TRANSLUCENT)
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build()
                );
            }
            
            if (glowDilatePipeline == null) {
                glowDilatePipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                        .withLocation(GLOW_DILATE_PIPELINE_ID)
                        .withVertexShader(VERTEX)
                        .withFragmentShader(GLOW_DILATE_SHADER)
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withUniform("GlowConfig", UniformType.UNIFORM_BUFFER)
                        .withSampler("MaskTex")
                        .withSampler("BoundsTexL")
                        .withSampler("BoundsTexR")
                        .withBlend(BlendFunction.TRANSLUCENT)
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build()
                );
            }
            
            if (glowGaussPipeline == null) {
                glowGaussPipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                        .withLocation(GLOW_GAUSS_PIPELINE_ID)
                        .withVertexShader(VERTEX)
                        .withFragmentShader(GLOW_GAUSS_SHADER)
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withUniform("GaussConfig", UniformType.UNIFORM_BUFFER)
                        .withSampler("TextureIn")
                        .withSampler("BoundsTexL")
                        .withSampler("BoundsTexR")
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build()
                );
            }
            
            if (outlinePipeline == null) {
                outlinePipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                        .withLocation(OUTLINE_PIPELINE_ID)
                        .withVertexShader(VERTEX)
                        .withFragmentShader(OUTLINE_SHADER)
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withUniform("OutlineConfig", UniformType.UNIFORM_BUFFER)
                        .withSampler("BaseMaskTex")
                        .withSampler("OutlineMaskTex")
                        .withSampler("GlowTex")
                        .withSampler("BoundsTexL")
                        .withSampler("BoundsTexR")
                        .withBlend(BlendFunction.TRANSLUCENT)
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build()
                );
            }
            
            if (outlineAddPipeline == null) {
                outlineAddPipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                        .withLocation(OUTLINE_ADD_PIPELINE_ID)
                        .withVertexShader(VERTEX)
                        .withFragmentShader(OUTLINE_SHADER)
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withUniform("OutlineConfig", UniformType.UNIFORM_BUFFER)
                        .withSampler("BaseMaskTex")
                        .withSampler("OutlineMaskTex")
                        .withSampler("GlowTex")
                        .withSampler("BoundsTexL")
                        .withSampler("BoundsTexR")
                        .withBlend(BlendFunction.LIGHTNING)
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build()
                );
            }
            
            if (boundsInitPipeline == null) {
                boundsInitPipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                        .withLocation(BOUNDS_INIT_PIPELINE_ID)
                        .withVertexShader(VERTEX)
                        .withFragmentShader(BOUNDS_INIT_SHADER)
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withUniform("InitConfig", UniformType.UNIFORM_BUFFER)
                        .withSampler("MaskTex")
                        .withSampler("SplitTex")
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build()
                );
            }
            
            if (boundsReducePipeline == null) {
                boundsReducePipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                        .withLocation(BOUNDS_REDUCE_PIPELINE_ID)
                        .withVertexShader(VERTEX)
                        .withFragmentShader(BOUNDS_REDUCE_SHADER)
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withUniform("ReduceConfig", UniformType.UNIFORM_BUFFER)
                        .withSampler("BoundsTex")
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build()
                );
            }
            
            if (boundsWritePipeline == null) {
                boundsWritePipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                        .withLocation(BOUNDS_WRITE_PIPELINE_ID)
                        .withVertexShader(VERTEX)
                        .withFragmentShader(BOUNDS_WRITE_SHADER)
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withUniform("BoundsWrite", UniformType.UNIFORM_BUFFER)
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build()
                );
            }
            
            if (passthroughPipeline == null) {
                passthroughPipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                        .withLocation(PASSTHROUGH_PIPELINE_ID)
                        .withVertexShader(VERTEX)
                        .withFragmentShader(PASSTHROUGH_SHADER)
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withSampler("HandTex")
                        .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build()
                );
            }
            
            if (handMaskPipeline == null) {
                handMaskPipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                        .withLocation(HANDMASK_PIPELINE_ID)
                        .withVertexShader(VERTEX)
                        .withFragmentShader(HANDMASK_SHADER)
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withSampler("HandTex")
                        .withSampler("HandDepth")
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build()
                );
            }
            
            kawaseBuffer = ShaderHandsRenderer.ensureBuffer(
                kawaseBuffer, 16, "meteor_shader_hands_kawase"
            );
            glassBuffer = ShaderHandsRenderer.ensureBuffer(
                glassBuffer, 48, "meteor_shader_hands_glass"
            );
            glowBuffer = ShaderHandsRenderer.ensureBuffer(
                glowBuffer, 96, "meteor_shader_hands_glow"
            );
            gaussBuffer = ShaderHandsRenderer.ensureBuffer(
                gaussBuffer, 224, "meteor_shader_hands_gauss"
            );
            outlineBuffer = ShaderHandsRenderer.ensureBuffer(
                outlineBuffer, 96, "meteor_shader_hands_outline"
            );
            reduceBuffer = ShaderHandsRenderer.ensureBuffer(
                reduceBuffer, 16, "meteor_shader_hands_reduce"
            );
            initBuffer = ShaderHandsRenderer.ensureBuffer(
                initBuffer, 16, "meteor_shader_hands_bounds_initcfg"
            );
            boundsWriteBuffer = ShaderHandsRenderer.ensureBuffer(
                boundsWriteBuffer, 16, "meteor_shader_hands_bounds_write"
            );
            paletteBuffer = ShaderHandsRenderer.ensureBuffer(
                paletteBuffer, PALETTE_BYTES, "meteor_shader_hands_palette"
            );
        } catch (Throwable throwable) {
            ShaderHandsRenderer.fail(throwable);
        }
    }
    
    private static SimpleFramebuffer destroy(SimpleFramebuffer simpleFramebuffer) {
        if (simpleFramebuffer != null) {
            simpleFramebuffer.delete();
        }
        return null;
    }
    
    private static void fail() {
        ShaderHandsRenderer.fail(null);
    }

    private static void fail(Throwable throwable) {
        disabledAfterError = true;
        
        if (throwable != null) {
            MeteorClient.LOGGER.error("ShaderHands renderer failed; disabling effect", throwable);
        } else {
            MeteorClient.LOGGER.error("ShaderHands renderer failed; disabling effect");
        }
        
        if (capturing) {
            RenderSystem.outputColorTextureOverride = prevColorOverride;
            RenderSystem.outputDepthTextureOverride = prevDepthOverride;
            capturing = false;
        }
        
        ShaderHandsRenderer.closeTargets();
        ShaderHandsRenderer.closeBuffers();
        ShaderHandsRenderer.reset();
    }
    
    private static void putColor(ByteBuffer byteBuffer, int n, int n2) {
        byteBuffer.putFloat(n, (float) (n2 >> 16 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 4, (float) (n2 >> 8 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 8, (float) (n2 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 12, (float) (n2 >>> 24 & 0xFF) / 255.0f);
    }
    
    private static GpuBuffer closeBuffer(GpuBuffer gpuBuffer) {
        if (gpuBuffer != null) {
            gpuBuffer.close();
        }
        return null;
    }
    
    private static void setBounds(float[] fArray, float f, float f2, float f3, float f4) {
        fArray[0] = f;
        fArray[1] = f2;
        fArray[2] = f3;
        fArray[3] = f4;
    }
    
    private static float syCell(float f, float f2) {
        return (143.0f - f + 0.5f) * f2;
    }
    
    private static void glowDilate(GpuTextureView gpuTextureView, SimpleFramebuffer simpleFramebuffer, float f, float f2, float f3, int[] nArray, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuSampler gpuSampler, boolean bl) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.calloc(96);
            byteBuffer.putFloat(0, f);
            byteBuffer.putFloat(4, f2);
            byteBuffer.putFloat(8, 1.0f / (float) simpleFramebuffer.textureWidth);
            byteBuffer.putFloat(12, 1.0f / (float) simpleFramebuffer.textureHeight);
            byteBuffer.putFloat(16, f3);
            byteBuffer.putFloat(20, 0.5f);
            byteBuffer.putFloat(24, 0.5f);
            byteBuffer.putFloat(28, 0.5f);
            ShaderHandsRenderer.putGradient(byteBuffer, 32, nArray);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(glowBuffer.slice(0L, 96L), byteBuffer);
        }
        
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_glow_dilate", simpleFramebuffer.getColorAttachmentView(), bl ? OptionalInt.of(0) : OptionalInt.empty())) {
            renderPass.setPipeline(glowDilatePipeline);
            renderPass.bindTexture("MaskTex", gpuTextureView, gpuSampler);
            renderPass.bindTexture("BoundsTexL", gpuTextureView2, RenderSampler.nearest());
            renderPass.bindTexture("BoundsTexR", gpuTextureView3, RenderSampler.nearest());
            renderPass.setUniform("GlowConfig", glowBuffer);
            renderPass.draw(0, 6);
        }
    }
    
    private static void glowGauss(GpuTextureView gpuTextureView, SimpleFramebuffer simpleFramebuffer, float f, float f2, float f3, int[] nArray, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuSampler gpuSampler) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.calloc(224);
            int n = Math.max(1, (int) Math.ceil(f3 * 2.0f));
            float f4 = Math.max(0.75f, f3 * 0.5f);
            float f5 = (float) Math.exp(-0.5f / (f4 * f4));
            float f6 = 1.0f / (2.5066283f * f4);
            byteBuffer.putFloat(0, f);
            byteBuffer.putFloat(4, f2);
            byteBuffer.putFloat(8, 1.0f / (float) simpleFramebuffer.textureWidth);
            byteBuffer.putFloat(12, 1.0f / (float) simpleFramebuffer.textureHeight);
            byteBuffer.putFloat(16, f6);
            byteBuffer.putFloat(20, f5);
            byteBuffer.putFloat(24, f5 * f5);
            byteBuffer.putFloat(28, n);
            byteBuffer.putFloat(32, 0.5f);
            byteBuffer.putFloat(36, 0.5f);
            byteBuffer.putFloat(40, 0.5f);
            byteBuffer.putFloat(44, 1.0f);
            ShaderHandsRenderer.putGradient(byteBuffer, 48, nArray);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(gaussBuffer.slice(0L, 224L), byteBuffer);
        }
        
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_glow_gauss", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0))) {
            renderPass.setPipeline(glowGaussPipeline);
            renderPass.bindTexture("TextureIn", gpuTextureView, gpuSampler);
            renderPass.bindTexture("BoundsTexL", gpuTextureView2, RenderSampler.nearest());
            renderPass.bindTexture("BoundsTexR", gpuTextureView3, RenderSampler.nearest());
            renderPass.setUniform("GaussConfig", gaussBuffer);
            renderPass.draw(0, 6);
        }
    }
    
    private static void kawaseBlur(int n, int n2, GpuSampler gpuSampler) {
        float f = 1.0f / (float) n;
        float f2 = 1.0f / (float) n2;
        
        ShaderHandsRenderer.kawasePass(kawaseDownPipeline, sceneCopyTextureView, sceneA, f, f2, gpuSampler);
        ShaderHandsRenderer.kawasePass(kawaseDownPipeline, sceneA.getColorAttachmentView(), sceneB, f, f2, gpuSampler);
        ShaderHandsRenderer.kawasePass(kawaseDownPipeline, sceneB.getColorAttachmentView(), sceneA, f, f2, gpuSampler);
        ShaderHandsRenderer.kawasePass(kawaseDownPipeline, sceneA.getColorAttachmentView(), sceneB, f, f2, gpuSampler);
        ShaderHandsRenderer.kawasePass(kawaseUpPipeline, sceneA.getColorAttachmentView(), sceneB, f, f2, gpuSampler);
        ShaderHandsRenderer.kawasePass(kawaseUpPipeline, sceneB.getColorAttachmentView(), sceneA, f, f2, gpuSampler);
        ShaderHandsRenderer.kawasePass(kawaseUpPipeline, sceneA.getColorAttachmentView(), sceneB, f, f2, gpuSampler);
        ShaderHandsRenderer.kawasePass(kawaseUpPipeline, sceneB.getColorAttachmentView(), sceneA, f, f2, gpuSampler);
    }
    
    private static float gaussian(float f, float f2) {
        float f3 = Math.max(0.1f, f2);
        return (float) (1.0 / Math.sqrt(Math.PI * 2 * (double) f3 * (double) f3) * Math.exp((double) (-(f * f)) / (2.0 * (double) f3 * (double) f3)));
    }
    
    private static void boundsInit(GpuTextureView gpuTextureView, SimpleFramebuffer simpleFramebuffer, float f, float f2, int n, GpuTextureView gpuTextureView2) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        
        try (MemoryStack memoryStack = MemoryStack.stackPush();) {
            ByteBuffer byteBuffer = memoryStack.calloc(16);
            byteBuffer.putFloat(0, f);
            byteBuffer.putFloat(4, f2);
            byteBuffer.putFloat(8, n);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(initBuffer.slice(0L, 16L), byteBuffer);
        }
        
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_bounds_init", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0))) {
            renderPass.setPipeline(boundsInitPipeline);
            renderPass.bindTexture("MaskTex", gpuTextureView, RenderSampler.nearest());
            renderPass.bindTexture("SplitTex", gpuTextureView2, RenderSampler.nearest());
            renderPass.setUniform("InitConfig", initBuffer);
            renderPass.draw(0, 6);
        }
    }
    
    private static void kawasePass(RenderPipeline renderPipeline, GpuTextureView gpuTextureView, SimpleFramebuffer simpleFramebuffer, float f, float f2, GpuSampler gpuSampler) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.calloc(16);
            byteBuffer.putFloat(0, f);
            byteBuffer.putFloat(4, f2);
            byteBuffer.putFloat(8, 2.6f);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(kawaseBuffer.slice(0L, 16L), byteBuffer);
        }
        
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_kawase", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0))) {
            renderPass.setPipeline(renderPipeline);
            renderPass.bindTexture("Image", gpuTextureView, gpuSampler);
            renderPass.setUniform("KawaseParams", kawaseBuffer);
            renderPass.draw(0, 6);
        }
    }
    
    private static float sxCell(float f, float f2) {
        return (f + 0.5f) * f2;
    }
    
    private static void glass(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuTextureView gpuTextureView4, GpuTextureView gpuTextureView5, int n, float f, float f2, float f3, float f4, boolean flip, GpuSampler gpuSampler) {
        ByteBuffer byteBuffer;
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            byteBuffer = memoryStack.calloc(48);
            ShaderHandsRenderer.putColor(byteBuffer, 0, n);
            byteBuffer.putFloat(16, f);
            byteBuffer.putFloat(20, f2);
            byteBuffer.putFloat(24, f3);
            byteBuffer.putFloat(28, f4);
            byteBuffer.putFloat(32, flip ? 1.0f : 0.0f);
            byteBuffer.putFloat(36, 0.0f);
            byteBuffer.putFloat(40, 0.0f);
            byteBuffer.putFloat(44, 0.0f);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(glassBuffer.slice(0L, 48L), byteBuffer);
            ShaderHandsRenderer.uploadPalette(commandEncoder, n);
        }
        
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_glass", gpuTextureView, OptionalInt.empty())) {
            renderPass.setPipeline(glassPipeline);
            renderPass.bindTexture("SceneTex", gpuTextureView2, gpuSampler);
            renderPass.bindTexture("HandTex", gpuTextureView3, gpuSampler);
            renderPass.bindTexture("BoundsTexL", gpuTextureView4, RenderSampler.nearest());
            renderPass.bindTexture("BoundsTexR", gpuTextureView5, RenderSampler.nearest());
            renderPass.setUniform("GlassConfig", glassBuffer);
            if (paletteBuffer != null) {
                renderPass.setUniform("PaletteParams", paletteBuffer);
            }
            renderPass.draw(0, 6);
        }
    }
    
    public static void composite(int n, int[] nArray, boolean bl, boolean bl2, int n2, float f, float f2, float f3, boolean bl3, float f4, float f5, float f6, float f7, boolean glassFlip) {
        Framebuffer framebuffer;
        if (disabledAfterError || !sceneReady || !handReady) {
            handReady = false;
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient != null ? minecraftClient.getFramebuffer() : null;
        
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null || glassPipeline == null || boundsWritePipeline == null || boundsLeft == null || boundsRight == null) {
            handReady = false;
            return;
        }
        
        try {
            GpuSampler gpuSampler = RenderSampler.linear();
            GpuTextureView gpuTextureView = handFbo.getColorAttachmentView();
            ShaderHandsRenderer.uploadHandBounds();
            GpuTextureView gpuTextureView2 = boundsLeft.getColorAttachmentView();
            GpuTextureView gpuTextureView3 = boundsRight.getColorAttachmentView();
            
            if (bl) {
                ShaderHandsRenderer.glass(framebuffer.getColorAttachmentView(), sceneB.getColorAttachmentView(), gpuTextureView, gpuTextureView2, gpuTextureView3, n, f4, f5, f6, f7, glassFlip, gpuSampler);
            } else if (passthroughPipeline != null) {
                CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_plain", framebuffer.getColorAttachmentView(), OptionalInt.empty());) {
                    renderPass.setPipeline(passthroughPipeline);
                    renderPass.bindTexture("HandTex", gpuTextureView, RenderSampler.linear());
                    renderPass.draw(0, 6);
                }
            }
            
            if (bl2 && glowDilatePipeline != null && glowGaussPipeline != null && outlinePipeline != null && outlineAddPipeline != null) {
                float f8;
                boolean bl4 = n2 != 1;
                boolean bl5 = n2 != 0;
                int[] nArray2 = nArray != null && nArray.length >= 4 ? nArray : ShaderHandsRenderer.gradientColors(n);
                float f9 = Math.clamp(f + 3.0f, 1.0f, 31.0f);
                float f10 = bl4 ? f3 : 0.0f;
                float f11 = f8 = bl5 ? f2 : 0.0f;
                if (bl4) {
                    ShaderHandsRenderer.glowDilate(gpuTextureView, glow, 0.0f, 0.0f, 1.0f, nArray2, gpuTextureView2, gpuTextureView3, gpuSampler, true);
                    ShaderHandsRenderer.glowGauss(glow.getColorAttachmentView(), glowSwap, 1.0f, 0.0f, f9, nArray2, gpuTextureView2, gpuTextureView3, gpuSampler);
                    ShaderHandsRenderer.glowGauss(glowSwap.getColorAttachmentView(), glow, 0.0f, 1.0f, f9, nArray2, gpuTextureView2, gpuTextureView3, gpuSampler);
                    ShaderHandsRenderer.glowGauss(glow.getColorAttachmentView(), glowSwap, 1.0f, 0.0f, f9, nArray2, gpuTextureView2, gpuTextureView3, gpuSampler);
                    ShaderHandsRenderer.glowGauss(glowSwap.getColorAttachmentView(), glow, 0.0f, 1.0f, f9, nArray2, gpuTextureView2, gpuTextureView3, gpuSampler);
                }
                ShaderHandsRenderer.outline(framebuffer.getColorAttachmentView(), gpuTextureView, glow.getColorAttachmentView(), nArray2, gpuTextureView2, gpuTextureView3, f9, f10, f8, bl3, gpuSampler);
            }
        } catch (Throwable throwable) {
            ShaderHandsRenderer.fail(throwable);
        } finally {
            handReady = false;
        }
    }

    private static void outline(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, int[] nArray, GpuTextureView gpuTextureView4, GpuTextureView gpuTextureView5, float f, float f2, float f3, boolean bl, GpuSampler gpuSampler) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.calloc(96);
            byteBuffer.putFloat(0, 1.0f / (float) targetWidth);
            byteBuffer.putFloat(4, 1.0f / (float) targetHeight);
            byteBuffer.putFloat(8, f3);
            byteBuffer.putFloat(12, f2);
            byteBuffer.putFloat(16, 0.5f);
            byteBuffer.putFloat(20, 0.5f);
            byteBuffer.putFloat(24, 0.5f);
            byteBuffer.putFloat(28, f);
            ShaderHandsRenderer.putGradient(byteBuffer, 32, nArray);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(outlineBuffer.slice(0L, 96L), byteBuffer);
        }
        
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_outline", gpuTextureView, OptionalInt.empty())) {
            renderPass.setPipeline(bl ? outlineAddPipeline : outlinePipeline);
            renderPass.bindTexture("BaseMaskTex", gpuTextureView2, gpuSampler);
            renderPass.bindTexture("OutlineMaskTex", gpuTextureView2, gpuSampler);
            renderPass.bindTexture("GlowTex", gpuTextureView3, gpuSampler);
            renderPass.bindTexture("BoundsTexL", gpuTextureView4, RenderSampler.nearest());
            renderPass.bindTexture("BoundsTexR", gpuTextureView5, RenderSampler.nearest());
            renderPass.setUniform("OutlineConfig", outlineBuffer);
            renderPass.draw(0, 6);
        }
    }
    
    public static void endHandCapture() {
        if (!capturing) {
            return;
        }
        
        RenderSystem.outputColorTextureOverride = prevColorOverride;
        RenderSystem.outputDepthTextureOverride = prevDepthOverride;
        
        prevColorOverride = null;
        prevDepthOverride = null;
        capturing = false;
        handReady = true;
        handCapturedFrame = true;
    }
    
    public static void captureScene(boolean bl) {
        Framebuffer framebuffer;
        
        handCapturedFrame = false;
        if (disabledAfterError) {
            return;
        }
        
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient != null ? minecraftClient.getFramebuffer() : null;
        
        if (framebuffer == null || framebuffer.getColorAttachment() == null || framebuffer.getColorAttachmentView() == null) {
            return;
        }
        
        ShaderHandsRenderer.init();
        if (kawaseDownPipeline == null || kawaseUpPipeline == null || !ShaderHandsRenderer.ensureTargets(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        
        try {
            RenderPass renderPass;
            GpuSampler gpuSampler = RenderSampler.linear();
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            
            if (handFbo != null && (renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_clear", handFbo.getColorAttachmentView(), OptionalInt.of(0), handFbo.getDepthAttachmentView(), OptionalDouble.of(1.0))) != null) {
                renderPass.close();
            }
            
            handReady = false;
            sceneReady = false;
            
            if (bl) {
                commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
                ShaderHandsRenderer.kawaseBlur(framebuffer.textureWidth, framebuffer.textureHeight, gpuSampler);
            }
            
            sceneReady = true;
        } catch (Throwable throwable) {
            ShaderHandsRenderer.fail(throwable);
        }
    }

    public static void captureScene() {
        ShaderHandsRenderer.captureScene(true);
    }
    
    public static void updateHandMask() {
        if (disabledAfterError || handFbo == null || handMaskPipeline == null || handFbo.getDepthAttachmentView() == null) {
            return;
        }
        
        try {
            if (maskGrid == null) {
                maskGrid = new SimpleFramebuffer("meteor_hand_mask_grid", 256, 144, false);
            }
            
            
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_hand_mask_grid", maskGrid.getColorAttachmentView(), OptionalInt.of(0))) {
                renderPass.setPipeline(handMaskPipeline);
                renderPass.bindTexture("HandTex", handFbo.getColorAttachmentView(), RenderSampler.linear());
                renderPass.bindTexture("HandDepth", handFbo.getDepthAttachmentView(), RenderSampler.linear());
                renderPass.draw(0, 6);
            }
            
            GpuTexture maskGridTex = maskGrid.getColorAttachment();
            if (maskGridTex == null) {
                return;
            }
            
            int n = maskGridTex.getFormat().pixelSize();
            int n2 = 36864 * n;
            if (maskReadBuffer == null || maskReadBuffer.isClosed()) {
                maskReadBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_hand_mask_read", 9, n2);
            }
            
            RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(maskGridTex, maskReadBuffer, 0L, () -> {
                try {
                    CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                    try (GpuBuffer.MappedView mappedView = encoder.mapBuffer(maskReadBuffer, true, false);) {
                        ByteBuffer byteBuffer = mappedView.data();
                        int[] nArray = new int[36864];
                        for (int i = 0; i < 144; ++i) {
                            for (int j = 0; j < 256; ++j) {
                                nArray[i * 256 + j] = byteBuffer.getInt((j + i * 256) * n);
                            }
                        }
                        handMaskCpu = nArray;
                        handMaskValid = true;
                    }
                } catch (Throwable ignored) {}
            }, 0);
        } catch (Throwable ignored) {}
    }
    
    public static boolean beginHandCapture() {
        Framebuffer framebuffer;
        if (disabledAfterError || !sceneReady) {
            return false;
        }
        
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient != null ? minecraftClient.getFramebuffer() : null;
        if (framebuffer == null) {
            return false;
        }
        
        ShaderHandsRenderer.init();
        if (!ShaderHandsRenderer.ensureTargets(framebuffer.textureWidth, framebuffer.textureHeight) || handFbo == null) {
            return false;
        }
        
        try {
            prevColorOverride = RenderSystem.outputColorTextureOverride;
            prevDepthOverride = RenderSystem.outputDepthTextureOverride;
            RenderSystem.outputColorTextureOverride = handFbo.getColorAttachmentView();
            RenderSystem.outputDepthTextureOverride = handFbo.getDepthAttachmentView();
            capturing = true;
            return true;
        } catch (Throwable throwable) {
            ShaderHandsRenderer.fail(throwable);
            return false;
        }
    }
    
    public static boolean isCapturing() {
        return capturing;
    }
    
    public static void compositePlain() {
        Framebuffer framebuffer;
        if (disabledAfterError || !handReady) {
            handReady = false;
            return;
        }
        
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient != null ? minecraftClient.getFramebuffer() : null;
        
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null || passthroughPipeline == null || handFbo == null) {
            handReady = false;
            return;
        }
        
        try {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_plain", framebuffer.getColorAttachmentView(), OptionalInt.empty());) {
                renderPass.setPipeline(passthroughPipeline);
                renderPass.bindTexture("HandTex", handFbo.getColorAttachmentView(), RenderSampler.linear());
                renderPass.draw(0, 6);
            }
        } catch (Throwable throwable) {
            ShaderHandsRenderer.fail(throwable);
        } finally {
            handReady = false;
        }
    }

    private static void closeBuffers() {
        kawaseBuffer = ShaderHandsRenderer.closeBuffer(kawaseBuffer);
        glassBuffer = ShaderHandsRenderer.closeBuffer(glassBuffer);
        glowBuffer = ShaderHandsRenderer.closeBuffer(glowBuffer);
        gaussBuffer = ShaderHandsRenderer.closeBuffer(gaussBuffer);
        outlineBuffer = ShaderHandsRenderer.closeBuffer(outlineBuffer);
        reduceBuffer = ShaderHandsRenderer.closeBuffer(reduceBuffer);
        initBuffer = ShaderHandsRenderer.closeBuffer(initBuffer);
        maskReadBuffer = ShaderHandsRenderer.closeBuffer(maskReadBuffer);
        paletteBuffer = ShaderHandsRenderer.closeBuffer(paletteBuffer);
    }
    
    public static void beginHandFrame() {
        handCapturedFrame = false;
    }
    
    public static float handDepthAt(float f, float f2, float f3, float f4) {
        int[] nArray = handMaskCpu;
        if (!handMaskValid || nArray == null || f3 < 0.5f || f4 < 0.5f) {
            return -1.0f;
        }
        
        int n = Math.clamp((int) (f / f3 * 256.0f), 0, 255);
        int n2 = Math.clamp((int) (f2 / f4 * 144.0f), 0, 143);
        int n3 = 143 - n2;
        int n4 = nArray[n3 * 256 + n];
        
        if ((n4 >>> 24 & 0xFF) < 110) {
            return -1.0f;
        }
        
        int n5 = n4 & 0xFF;
        if (n5 <= 0 || n5 >= 255) {
            return -1.0f;
        }
        
        return 0.2f + (float) n5 / 255.0f * 1.8f;
    }
    
    public static boolean isHandCoveredAt(float f, float f2, float f3, float f4) {
        int[] nArray = handMaskCpu;
        
        if (!handMaskValid || nArray == null || f3 < 0.5f || f4 < 0.5f) {
            return false;
        }
        
        int n = Math.clamp((int) (f / f3 * 256.0f), 0, 255);
        int n2 = Math.clamp((int) (f2 / f4 * 144.0f), 0, 143);
        int n3 = 143 - n2;
        int n4 = nArray[n3 * 256 + n];
        return (n4 >>> 24 & 0xFF) >= 110;
    }
    
    private static GpuBuffer ensureBuffer(GpuBuffer gpuBuffer, int n, String string) {
        if (gpuBuffer != null && !gpuBuffer.isClosed() && gpuBuffer.size() >= (long) n) {
            return gpuBuffer;
        }
        if (gpuBuffer != null) {
            gpuBuffer.close();
        }
        return RenderSystem.getDevice().createBuffer(() -> string, 136, (long) n);
    }
    
    private static void closeTargets() {
        if (sceneCopyTextureView != null) {
            sceneCopyTextureView.close();
            sceneCopyTextureView = null;
        }
        
        if (sceneCopyTexture != null) {
            sceneCopyTexture.close();
            sceneCopyTexture = null;
        }
        
        sceneA = ShaderHandsRenderer.destroy(sceneA);
        sceneB = ShaderHandsRenderer.destroy(sceneB);
        handFbo = ShaderHandsRenderer.destroy(handFbo);
        glow = ShaderHandsRenderer.destroy(glow);
        glowSwap = ShaderHandsRenderer.destroy(glowSwap);
        
        for (SimpleFramebuffer simpleFramebuffer : boundsChain) {
            if (simpleFramebuffer == null) {
                continue;
            }
            simpleFramebuffer.delete();
        }
        
        boundsChain.clear();
        boundsUnion = ShaderHandsRenderer.destroy(boundsUnion);
        boundsLeft = ShaderHandsRenderer.destroy(boundsLeft);
        boundsRight = ShaderHandsRenderer.destroy(boundsRight);
        maskGrid = ShaderHandsRenderer.destroy(maskGrid);
        handMaskValid = false;
        targetWidth = -1;
        targetHeight = -1;
    }
    
    private static boolean ensureTargets(int n, int n2) {
        GpuDevice gpuDevice = RenderSystem.tryGetDevice();
        if (gpuDevice == null || n <= 0 || n2 <= 0) {
            return false;
        }
        
        if (sceneCopyTexture != null && targetWidth == n && targetHeight == n2) {
            return true;
        }
        
        ShaderHandsRenderer.closeTargets();
        sceneCopyTexture = gpuDevice.createTexture(() -> "meteor_shader_hands_scene_copy", 5, TextureFormat.RGBA8, n, n2, 1, 1);
        sceneCopyTextureView = gpuDevice.createTextureView(sceneCopyTexture);
        sceneA = new SimpleFramebuffer("meteor_shader_hands_scene_a", n, n2, false);
        sceneB = new SimpleFramebuffer("meteor_shader_hands_scene_b", n, n2, false);
        handFbo = new SimpleFramebuffer("meteor_shader_hands_hand", n, n2, true);
        glow = new SimpleFramebuffer("meteor_shader_hands_glow", n, n2, false);
        glowSwap = new SimpleFramebuffer("meteor_shader_hands_glow_swap", n, n2, false);
        boundsChain.clear();
        
        int n3 = n;
        int n4 = n2;
        int n5 = 0;
        
        boundsChain.add(new SimpleFramebuffer("meteor_shader_hands_bounds_" + n5, n3, n4, false));
        
        while (n3 > 1 || n4 > 1) {
            n3 = Math.max(1, n3 / 2);
            n4 = Math.max(1, n4 / 2);
            boundsChain.add(new SimpleFramebuffer("meteor_shader_hands_bounds_" + ++n5, n3, n4, false));
        }
        
        boundsUnion = new SimpleFramebuffer("meteor_shader_hands_bounds_union", 1, 1, false);
        boundsLeft = new SimpleFramebuffer("meteor_shader_hands_bounds_left", 1, 1, false);
        boundsRight = new SimpleFramebuffer("meteor_shader_hands_bounds_right", 1, 1, false);
        targetWidth = n;
        targetHeight = n2;
        return true;
    }
    
    public static GpuTextureView capturedHandDepthView() {
        return ShaderHandsRenderer.wasHandCapturedThisFrame() ? handFbo.getDepthAttachmentView() : null;
    }
    
    private static int[] gradientColors(int n) {
//        if (ClientPalette.count() >= 2) {
//            float f = ClientPalette.phase() * 20.0f;
//            return new int[] { 0xFF000000 | ClientPalette.loopColor(f + 0.75f) & 0xFFFFFF, 0xFF000000 | ClientPalette.loopColor(f + 0.5f) & 0xFFFFFF, 0xFF000000 | ClientPalette.loopColor(f + 0.25f) & 0xFFFFFF, 0xFF000000 | ClientPalette.loopColor(f) & 0xFFFFFF };
//        }
        return new int[] { n, ColorUtil.lerpColor(n, -1, 0.1f), n, ColorUtil.lerpColor(n, -16777216, 0.1f) };
    }
    
    private static int nearestCovered(int[] nArray, int n, int n2) {
        for (int i = 0; i <= 4; ++i) {
            for (int j = -i; j <= i; ++j) {
                for (int k = -i; k <= i; ++k) {
                    int n3;
                    if (Math.max(Math.abs(k), Math.abs(j)) != i) {
                        continue;
                    }
                    
                    int n4 = n + k;
                    int n5 = n2 + j;
                    if (n4 < 0 || n4 >= 256 || n5 < 0 || n5 >= 144 || (nArray[n3 = n5 * 256 + n4] >>> 24 & 0xFF) < 110) {
                        continue;
                    }
                    
                    return n3;
                }
            }
        }
        return -1;
    }
    
    private static float[] marchSquares(float[] fArray, float f, float f2, float f3) {
        if (marchBuf == null) {
            marchBuf = new float[65536];
        }
        
        float[] fArray2 = marchBuf;
        int n = 0;
        float[] fArray3 = marchPts;
        
        for (int i = 0; i < 143; ++i) {
            for (int j = 0; j < 255; ++j) {
                float f4;
                float f5 = fArray[i * 256 + j];
                float f6 = fArray[i * 256 + j + 1];
                float f7 = fArray[(i + 1) * 256 + j];
                float f8 = fArray[(i + 1) * 256 + j + 1];
                int n2 = 0;
                
                if (f5 < f != f6 < f) {
                    f4 = (f - f5) / (f6 - f5);
                    fArray3[n2 * 2] = ShaderHandsRenderer.sxCell((float) j + f4, f2);
                    fArray3[n2 * 2 + 1] = ShaderHandsRenderer.syCell(i, f3);
                    ++n2;
                }
                
                if (f6 < f != f8 < f) {
                    f4 = (f - f6) / (f8 - f6);
                    fArray3[n2 * 2] = ShaderHandsRenderer.sxCell(j + 1, f2);
                    fArray3[n2 * 2 + 1] = ShaderHandsRenderer.syCell((float) i + f4, f3);
                    ++n2;
                }
                
                if (f7 < f != f8 < f) {
                    f4 = (f - f7) / (f8 - f7);
                    fArray3[n2 * 2] = ShaderHandsRenderer.sxCell((float) j + f4, f2);
                    fArray3[n2 * 2 + 1] = ShaderHandsRenderer.syCell(i + 1, f3);
                    ++n2;
                }
                
                if (f5 < f != f7 < f) {
                    f4 = (f - f5) / (f7 - f5);
                    fArray3[n2 * 2] = ShaderHandsRenderer.sxCell(j, f2);
                    fArray3[n2 * 2 + 1] = ShaderHandsRenderer.syCell((float) i + f4, f3);
                    ++n2;
                }
                
                if (n2 >= 2 && n + 4 <= fArray2.length) {
                    fArray2[n++] = fArray3[0];
                    fArray2[n++] = fArray3[1];
                    fArray2[n++] = fArray3[2];
                    fArray2[n++] = fArray3[3];
                }
                
                if (n2 != 4 || n + 4 > fArray2.length) {
                    continue;
                }
                
                fArray2[n++] = fArray3[4];
                fArray2[n++] = fArray3[5];
                fArray2[n++] = fArray3[6];
                fArray2[n++] = fArray3[7];
            }
        }
        return Arrays.copyOf(fArray2, n);
    }
    
    private static void toTexBounds(int[] nArray, float[] fArray) {
        fArray[0] = (float) nArray[0] / 256.0f;
        fArray[1] = (float) nArray[1] / 144.0f;
        fArray[2] = (float) (nArray[2] + 1) / 256.0f;
        fArray[3] = (float) (nArray[3] + 1) / 144.0f;
    }
    
    public static boolean isHandMaskReady() {
        return !disabledAfterError && handFbo != null && targetWidth > 0 && targetHeight > 0;
    }
    
    public static List<float[]> handContours(float f, float f2, float f3, float f4, float[] fArray) {
        int n;
        int n2;
        int n3;
        int n4;
        int[] nArray = handMaskCpu;
        
        if (!handMaskValid || nArray == null || f3 < 0.5f || f4 < 0.5f || fArray == null) {
            return null;
        }
        
        int n5 = Math.clamp((int) (f / f3 * 256.0f), 0, 255);
        int n6 = ShaderHandsRenderer.nearestCovered(nArray, n5, 143 - Math.clamp((int) (f2 / f4 * 144.0f), 0, 143));
        if (n6 < 0) {
            return null;
        }
        
        int n7 = 36864;
        if (floodStamp == null || floodStamp.length != n7) {
            floodStamp = new int[n7];
            floodQueue = new int[n7];
            floodDist = new float[n7];
        }
        
        float f5 = 1.0E9f;
        Arrays.fill(floodDist, 1.0E9f);
        int n8 = ++floodGen;
        int n9 = 0;
        int n10 = 0;
        ShaderHandsRenderer.floodQueue[n10++] = n6;
        ShaderHandsRenderer.floodStamp[n6] = n8;
        ShaderHandsRenderer.floodDist[n6] = 0.0f;
        
        while (n9 < n10) {
            int n11 = floodQueue[n9++];
            int n12 = n11 % 256;
            int n13 = n11 / 256;
            
            for (n3 = 0; n3 < 4; ++n3) {
                int n14;
                n2 = n12 + (n3 == 0 ? -1 : (n3 == 1 ? 1 : 0));
                n = n13 + (n3 == 2 ? -1 : (n3 == 3 ? 1 : 0));
                if (n2 < 0 || n2 >= 256 || n < 0 || n >= 144 || floodStamp[n14 = n * 256 + n2] == n8 || (nArray[n14] >>> 24 & 0xFF) < 110) {
                    continue;
                }
                ShaderHandsRenderer.floodStamp[n14] = n8;
                ShaderHandsRenderer.floodDist[n14] = 0.0f;
                ShaderHandsRenderer.floodQueue[n10++] = n14;
            }
        }
        
        float f6 = f3 / 256.0f;
        float f7 = f4 / 144.0f;
        float f8 = (float) Math.sqrt(f6 * f6 + f7 * f7);
        
        for (n3 = 0; n3 < 144; ++n3) {
            for (n2 = 0; n2 < 256; ++n2) {
                n = n3 * 256 + n2;
                float f9 = floodDist[n];
                if (n2 > 0) {
                    f9 = Math.min(f9, floodDist[n - 1] + f6);
                }
                if (n3 > 0) {
                    f9 = Math.min(f9, floodDist[n - 256] + f7);
                }
                if (n2 > 0 && n3 > 0) {
                    f9 = Math.min(f9, floodDist[n - 256 - 1] + f8);
                }
                if (n2 < 255 && n3 > 0) {
                    f9 = Math.min(f9, floodDist[n - 256 + 1] + f8);
                }
                ShaderHandsRenderer.floodDist[n] = f9;
            }
        }
        
        for (n3 = 143; n3 >= 0; --n3) {
            for (n2 = 255; n2 >= 0; --n2) {
                n = n3 * 256 + n2;
                float f10 = floodDist[n];
                if (n2 < 255) {
                    f10 = Math.min(f10, floodDist[n + 1] + f6);
                }
                if (n3 < 143) {
                    f10 = Math.min(f10, floodDist[n + 256] + f7);
                }
                if (n2 < 255 && n3 < 143) {
                    f10 = Math.min(f10, floodDist[n + 256 + 1] + f8);
                }
                if (n2 > 0 && n3 < 143) {
                    f10 = Math.min(f10, floodDist[n + 256 - 1] + f8);
                }
                ShaderHandsRenderer.floodDist[n] = f10;
            }
        }
        
        ArrayList<float[]> arrayList = new ArrayList<>(fArray.length);
        for (float f11 : fArray) {
            arrayList.add(ShaderHandsRenderer.marchSquares(floodDist, f11, f6, f7));
        }
        return arrayList;
    }
    
    public static boolean wasHandCapturedThisFrame() {
        return !disabledAfterError && handCapturedFrame && handFbo != null && handFbo.getColorAttachmentView() != null;
    }
    
    public static GpuTextureView capturedHandColorView() {
        return ShaderHandsRenderer.wasHandCapturedThisFrame() ? handFbo.getColorAttachmentView() : null;
    }
    
    public static GpuTextureView handBoundsLeftView() {
        return ShaderHandsRenderer.wasHandCapturedThisFrame() && boundsLeft != null ? boundsLeft.getColorAttachmentView() : null;
    }
    
    private static void computeHandBounds() {
        ShaderHandsRenderer.setBounds(boundsAData, 1.0f, 1.0f, 0.0f, 0.0f);
        ShaderHandsRenderer.setBounds(boundsBData, 1.0f, 1.0f, 0.0f, 0.0f);
        
        int[] nArray = handMaskCpu;
        if (!handMaskValid || nArray == null) {
            return;
        }
        int n = 36864;
        
        if (floodStamp == null || floodStamp.length != n) {
            floodStamp = new int[n];
            floodQueue = new int[n];
        }
        
        int n2 = ++floodGen;
        int n3 = -1;
        int n4 = -1;
        int[] nArray2 = null;
        int[] nArray3 = null;
        
        for (int i = 0; i < n; ++i) {
            if ((nArray[i] >>> 24 & 0xFF) < 110 || floodStamp[i] == n2) {
                continue;
            }
            
            int n5 = 0;
            int n6 = 0;
            
            ShaderHandsRenderer.floodQueue[n6++] = i;
            ShaderHandsRenderer.floodStamp[i] = n2;
            
            int n7 = 256;
            int n8 = -1;
            int n9 = 144;
            int n10 = -1;
            int n11 = 0;
            
            while (n5 < n6) {
                int n12 = floodQueue[n5++];
                int n13 = n12 % 256;
                int n14 = n12 / 256;
                
                ++n11;
                
                if (n13 < n7) {
                    n7 = n13;
                }
                if (n13 > n8) {
                    n8 = n13;
                }
                if (n14 < n9) {
                    n9 = n14;
                }
                if (n14 > n10) {
                    n10 = n14;
                }
                
                for (int j = 0; j < 4; ++j) {
                    int n15;
                    int n16 = n13 + (j == 0 ? -1 : (j == 1 ? 1 : 0));
                    int n17 = n14 + (j == 2 ? -1 : (j == 3 ? 1 : 0));
                    
                    if (n16 < 0 || n16 >= 256 || n17 < 0 || n17 >= 144 || floodStamp[n15 = n17 * 256 + n16] == n2 || (nArray[n15] >>> 24 & 0xFF) < 110) {
                        continue;
                    }
                    
                    ShaderHandsRenderer.floodStamp[n15] = n2;
                    ShaderHandsRenderer.floodQueue[n6++] = n15;
                }
            }
            
            if (n11 < 12) {
                continue;
            }
            
            int[] nArray4 = new int[] { n7, n9, n8, n10 };
            
            if (n11 > n3) {
                n4 = n3;
                nArray3 = nArray2;
                n3 = n11;
                nArray2 = nArray4;
                continue;
            }
            
            if (n11 <= n4) {
                continue;
            }
            
            n4 = n11;
            nArray3 = nArray4;
        }
        
        if (nArray2 == null) {
            return;
        }
        
        if (nArray3 == null) {
            ShaderHandsRenderer.toTexBounds(nArray2, boundsAData);
            ShaderHandsRenderer.toTexBounds(nArray2, boundsBData);
            return;
        }
        
        float f = (float) (nArray2[0] + nArray2[2]) * 0.5f;
        float f2 = (float) (nArray3[0] + nArray3[2]) * 0.5f;
        
        if (f <= f2) {
            ShaderHandsRenderer.toTexBounds(nArray2, boundsAData);
            ShaderHandsRenderer.toTexBounds(nArray3, boundsBData);
        } else {
            ShaderHandsRenderer.toTexBounds(nArray3, boundsAData);
            ShaderHandsRenderer.toTexBounds(nArray2, boundsBData);
        }
    }
    
    public static void uploadHandBounds() {
        ShaderHandsRenderer.computeHandBounds();
        ShaderHandsRenderer.writeBounds(boundsLeft, boundsAData);
        ShaderHandsRenderer.writeBounds(boundsRight, boundsBData);
    }

    private static void uploadPalette(CommandEncoder commandEncoder, int color) {
        if (paletteBuffer == null) {
            return;
        }
        
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.calloc(PALETTE_BYTES);
            float r = (float) (color >> 16 & 0xFF) / 255.0f;
            float g = (float) (color >> 8 & 0xFF) / 255.0f;
            float b = (float) (color & 0xFF) / 255.0f;
            // paletteMeta.x = stop count. 1 keeps glass.fsh on Multiplier (GlassConfig) tint.
            byteBuffer.putFloat(0, 1.0f);
            byteBuffer.putFloat(4, 0.0f);
            byteBuffer.putFloat(8, 0.0f);
            byteBuffer.putFloat(12, -1.0f);
            for (int i = 0; i < 6; i++) {
                int off = 16 + i * 16;
                byteBuffer.putFloat(off, r);
                byteBuffer.putFloat(off + 4, g);
                byteBuffer.putFloat(off + 8, b);
                byteBuffer.putFloat(off + 12, 1.0f);
            }
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(paletteBuffer.slice(0L, PALETTE_BYTES), byteBuffer);
        }
    }

    private static void writeBounds(SimpleFramebuffer simpleFramebuffer, float[] fArray) {
        if (simpleFramebuffer == null || boundsWritePipeline == null) {
            return;
        }
        
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (MemoryStack memoryStack = MemoryStack.stackPush();) {
            ByteBuffer byteBuffer = memoryStack.calloc(16);
            byteBuffer.putFloat(0, fArray[0]);
            byteBuffer.putFloat(4, fArray[1]);
            byteBuffer.putFloat(8, fArray[2]);
            byteBuffer.putFloat(12, fArray[3]);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(boundsWriteBuffer.slice(0L, 16L), byteBuffer);
        }
        
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_bounds_write", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0))) {
            renderPass.setPipeline(boundsWritePipeline);
            renderPass.setUniform("BoundsWrite", boundsWriteBuffer);
            renderPass.draw(0, 6);
        }
    }
    
    public static float[] handBoundsAt(float f, float f2, float f3, float f4) {
        int n;
        int[] nArray = handMaskCpu;
        if (!handMaskValid || nArray == null || f3 < 0.5f || f4 < 0.5f) {
            return null;
        }
        
        int n2 = Math.max(0, Math.min(255, (int) (f / f3 * 256.0f)));
        int n3 = ShaderHandsRenderer.nearestCovered(nArray, n2, 143 - (n = Math.max(0, Math.min(143, (int) (f2 / f4 * 144.0f)))));
        if (n3 < 0) {
            return null;
        }
        
        if (floodStamp == null || floodStamp.length != 36864) {
            floodStamp = new int[36864];
            floodQueue = new int[36864];
        }
        
        int n4 = ++floodGen;
        int n5 = 0;
        int n6 = 0;
        
        ShaderHandsRenderer.floodQueue[n6++] = n3;
        ShaderHandsRenderer.floodStamp[n3] = n4;
        
        int n7 = 256;
        int n8 = -1;
        int n9 = 144;
        int n10 = -1;
        
        while (n5 < n6) {
            int n11 = floodQueue[n5++];
            int n12 = n11 % 256;
            int n13 = n11 / 256;
            
            if (n12 < n7) {
                n7 = n12;
            }
            if (n12 > n8) {
                n8 = n12;
            }
            if (n13 < n9) {
                n9 = n13;
            }
            if (n13 > n10) {
                n10 = n13;
            }
            
            for (int i = 0; i < 4; ++i) {
                int n14;
                int n15 = n12 + (i == 0 ? -1 : (i == 1 ? 1 : 0));
                int n16 = n13 + (i == 2 ? -1 : (i == 3 ? 1 : 0));
                
                if (n15 < 0 || n15 >= 256 || n16 < 0 || n16 >= 144 || floodStamp[n14 = n16 * 256 + n15] == n4 || (nArray[n14] >>> 24 & 0xFF) < 110) {
                    continue;
                }
                
                ShaderHandsRenderer.floodStamp[n14] = n4;
                ShaderHandsRenderer.floodQueue[n6++] = n14;
            }
        }
        
        float f5 = (float) n7 / 256.0f * f3;
        float f6 = (float) (n8 + 1) / 256.0f * f3;
        float f7 = (float) (143 - n10) / 144.0f * f4;
        float f8 = (float) (144 - n9) / 144.0f * f4;
        
        return new float[] { f5, f7, f6 - f5, f8 - f7 };
    }
    
    public static GpuTextureView handBoundsRightView() {
        return ShaderHandsRenderer.wasHandCapturedThisFrame() && boundsRight != null ? boundsRight.getColorAttachmentView() : null;
    }
    
    private static void computeBoundsInto(GpuTextureView gpuTextureView, float f, float f2, SimpleFramebuffer simpleFramebuffer) {
        ShaderHandsRenderer.computeBounds(gpuTextureView, f, f2, 0, boundsLeft.getColorAttachmentView(), simpleFramebuffer);
    }
    
    private static void boundsReduce(GpuTextureView gpuTextureView, SimpleFramebuffer simpleFramebuffer, int n, int n2) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        
        try (MemoryStack memoryStack = MemoryStack.stackPush();) {
            ByteBuffer byteBuffer = memoryStack.calloc(16);
            byteBuffer.putFloat(0, 1.0f / (float) n);
            byteBuffer.putFloat(4, 1.0f / (float) n2);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(reduceBuffer.slice(0L, 16L), byteBuffer);
        }
        
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "meteor_shader_hands_bounds_reduce", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0))) {
            renderPass.setPipeline(boundsReducePipeline);
            renderPass.bindTexture("BoundsTex", gpuTextureView, RenderSampler.nearest());
            renderPass.setUniform("ReduceConfig", reduceBuffer);
            renderPass.draw(0, 6);
        }
    }
    
    private static void putGradient(ByteBuffer byteBuffer, int n, int[] nArray) {
        for (int i = 0; i < 4; ++i) {
            ShaderHandsRenderer.putColor(byteBuffer, n + i * 16, nArray[i]);
        }
    }
    
    private static RenderPipeline registerKawase(Identifier identifier, Identifier identifier2) {
        return RenderPipelines.register((RenderPipeline) RenderPipeline.builder((RenderPipeline.Snippet[]) new RenderPipeline.Snippet[0]).withLocation(identifier).withVertexShader(VERTEX).withFragmentShader(identifier2).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("KawaseParams", UniformType.UNIFORM_BUFFER).withSampler("Image").withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
    }
    
    private static void computeBounds(GpuTextureView gpuTextureView, float f, float f2, int n, GpuTextureView gpuTextureView2, SimpleFramebuffer simpleFramebuffer) {
        ShaderHandsRenderer.boundsInit(gpuTextureView, boundsChain.get(0), f, f2, n, gpuTextureView2);
        int n2 = boundsChain.size();
        for (int i = 0; i < n2 - 2; ++i) {
            SimpleFramebuffer simpleFramebuffer2 = boundsChain.get(i);
            ShaderHandsRenderer.boundsReduce(simpleFramebuffer2.getColorAttachmentView(), boundsChain.get(i + 1), simpleFramebuffer2.textureWidth, simpleFramebuffer2.textureHeight);
        }
        SimpleFramebuffer simpleFramebuffer3 = boundsChain.get(n2 - 2);
        ShaderHandsRenderer.boundsReduce(simpleFramebuffer3.getColorAttachmentView(), simpleFramebuffer, simpleFramebuffer3.textureWidth, simpleFramebuffer3.textureHeight);
    }
    
}

