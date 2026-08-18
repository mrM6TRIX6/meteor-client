package meteordevelopment.meteorclient.utils.render.post.saturation;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.RenderSampler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

public class Saturation2D {
    
    private static final Identifier PIPELINE_ID = MeteorClient.identifier("pipeline/post/saturation");
    private static final Identifier VERTEX_SHADER = MeteorClient.identifier("post/saturation/saturation");
    private static final Identifier FRAGMENT_SHADER = MeteorClient.identifier("post/saturation/saturation");
    
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(PIPELINE_ID)
            .withVertexShader(VERTEX_SHADER)
            .withFragmentShader(FRAGMENT_SHADER)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .withUniform("SaturationData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build()
    );
    
    private static final ByteBuffer DATA_BUFFER = MemoryUtil.memAlloc(16);
    
    private static final GpuBuffer DUMMY_VERTEX_BUFFER;
    private static final GpuBuffer UNIFORM_BUFFER;
    
    private static GpuTexture tempTexture;
    private static GpuTextureView tempTextureView;
    private static int lastWidth;
    private static int lastHeight;
    private static boolean disabledAfterError;
    
    static {
        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        DUMMY_VERTEX_BUFFER = RenderSystem.getDevice().createBuffer(
            () -> "meteor-client:saturation_dummy_vertex",
            GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
            dummyData
        );
        MemoryUtil.memFree(dummyData);
        
        UNIFORM_BUFFER = RenderSystem.getDevice().createBuffer(
            () -> "meteor-client:saturation_uniform",
            GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
            16
        );
    }
    
    private static void ensureTextures(int width, int height) {
        if (tempTexture != null && width == lastWidth && height == lastHeight) {
            return;
        }
        
        if (tempTextureView != null) {
            tempTextureView.close();
        }
        if (tempTexture != null) {
            tempTexture.close();
        }
        
        tempTexture = RenderSystem.getDevice().createTexture(
            () -> "meteor-client:saturation_temp",
            GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
            TextureFormat.RGBA8,
            width,
            height,
            1,
            1
        );
        tempTextureView = RenderSystem.getDevice().createTextureView(tempTexture);
        lastWidth = width;
        lastHeight = height;
    }
    
    public static void applyWithCopy(float saturation) {
        if (disabledAfterError) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) {
            return;
        }
        if (!Float.isFinite(saturation)) {
            return;
        }
        float clampedSaturation = Math.clamp(saturation, 0.0f, 2.0f);
        if (Math.abs(clampedSaturation - 1.0f) <= 0.0005f) {
            return;
        }
        
        int width = client.getFramebuffer().textureWidth;
        int height = client.getFramebuffer().textureHeight;
        if (width <= 0 || height <= 0) {
            return;
        }
        ensureTextures(width, height);
        
        try {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.copyTextureToTexture(
                client.getFramebuffer().getColorAttachment(),
                tempTexture,
                0,
                0,
                0,
                0,
                0,
                width,
                height
            );
            
            apply(client.getFramebuffer().getColorAttachmentView(), tempTextureView, clampedSaturation);
        } catch (Throwable ignored) {
            disabledAfterError = true;
        }
    }
    
    private static void apply(GpuTextureView targetView, GpuTextureView sourceView, float saturation) {
        DATA_BUFFER.clear();
        DATA_BUFFER.putFloat(saturation);
        DATA_BUFFER.flip();
        
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(UNIFORM_BUFFER.slice(), DATA_BUFFER);
        
        try (RenderPass renderPass = encoder.createRenderPass(() -> "meteor-client:saturation_pass", targetView, OptionalInt.empty())) {
            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, DUMMY_VERTEX_BUFFER);
            renderPass.bindTexture("Sampler0", sourceView, RenderSampler.linear());
            renderPass.setUniform("SaturationData", UNIFORM_BUFFER.slice());
            renderPass.draw(0, 6);
        }
    }
    
}
