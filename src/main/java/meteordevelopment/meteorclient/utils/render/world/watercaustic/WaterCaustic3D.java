package meteordevelopment.meteorclient.utils.render.world.watercaustic;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class WaterCaustic3D {
    
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    private static final int UNIFORM_SIZE = 128;
    private static final int WATER_DATA_SIZE = 80;
    private static final int MAX_VERTICES = 16384;
    private static final int VERTEX_SIZE = 24;
    private static final int VERTEX_STRIDE = 5;
    
    private static final float[] vertices = new float[MAX_VERTICES * VERTEX_STRIDE];
    private static int vertexCount;
    private static RenderPipeline pipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer waterDataBuffer;
    private static GpuBuffer vertexBuffer;
    private static final Matrix4f projectionMatrix = new Matrix4f();
    private static final Matrix4f viewMatrix = new Matrix4f();
    private static final Matrix4f combinedMatrix = new Matrix4f();
    private static final Matrix4f identityMatrix = new Matrix4f();
    private static boolean disabledAfterError;
    
    public static void init() {
        if (pipeline != null) {
            return;
        }
        
        try {
            VertexFormat format = VertexFormat.builder()
                .add("inPosition", VertexFormatElement.POSITION)
                .add("inColor", VertexFormatElement.COLOR)
                .add("inUV", VertexFormatElement.UV)
                .build();
            
            pipeline = RenderPipeline.builder()
                .withLocation(MeteorClient.identifier("pipeline/world/watercaustic"))
                .withVertexShader(MeteorClient.identifier("world/shared/particle_billboard"))
                .withFragmentShader(MeteorClient.identifier("world/watercaustic/watercaustic"))
                .withVertexFormat(format, VertexFormat.DrawMode.TRIANGLES)
                .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                .withUniform("WaterData", UniformType.UNIFORM_BUFFER)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();
            
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "WaterCaustic3D Uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_SIZE
            );
            
            waterDataBuffer = RenderSystem.getDevice().createBuffer(
                () -> "WaterCaustic3D Data",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                WATER_DATA_SIZE
            );
            
            vertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "WaterCaustic3D Vertices",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                MAX_VERTICES * VERTEX_SIZE
            );
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    
    public static void setMatrices(Matrix4f projection, Matrix4f view) {
        projectionMatrix.set(projection);
        viewMatrix.set(view);
    }
    
    public static void begin() {
        if (disabledAfterError) {
            return;
        }
        if (pipeline == null) {
            init();
        }
        vertexCount = 0;
    }
    
    public static void box(Box box) {
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;
        
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        float rx1 = (float) (x1 - cam.x);
        float ry1 = (float) (y1 - cam.y);
        float rz1 = (float) (z1 - cam.z);
        float rx2 = (float) (x2 - cam.x);
        float ry2 = (float) (y2 - cam.y);
        float rz2 = (float) (z2 - cam.z);
        
        quad(rx1, ry1, rz1, rx2, ry1, rz1, rx2, ry1, rz2, rx1, ry1, rz2);
        quad(rx1, ry2, rz1, rx1, ry2, rz2, rx2, ry2, rz2, rx2, ry2, rz1);
        quad(rx1, ry1, rz1, rx1, ry2, rz1, rx2, ry2, rz1, rx2, ry1, rz1);
        quad(rx1, ry1, rz2, rx2, ry1, rz2, rx2, ry2, rz2, rx1, ry2, rz2);
        quad(rx1, ry1, rz1, rx1, ry1, rz2, rx1, ry2, rz2, rx1, ry2, rz1);
        quad(rx2, ry1, rz1, rx2, ry2, rz1, rx2, ry2, rz2, rx2, ry1, rz2);
    }
    
    private static void quad(float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, float x4, float y4, float z4) {
        addVertex(x1, y1, z1, 0, 0);
        addVertex(x2, y2, z2, 1, 0);
        addVertex(x3, y3, z3, 1, 1);
        addVertex(x1, y1, z1, 0, 0);
        addVertex(x3, y3, z3, 1, 1);
        addVertex(x4, y4, z4, 0, 1);
    }
    
    private static void addVertex(float x, float y, float z, float u, float v) {
        if (vertexCount >= MAX_VERTICES || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || !Float.isFinite(u) || !Float.isFinite(v)) {
            return;
        }
        int offset = vertexCount++ * VERTEX_STRIDE;
        vertices[offset] = x;
        vertices[offset + 1] = y;
        vertices[offset + 2] = z;
        vertices[offset + 3] = u;
        vertices[offset + 4] = v;
    }
    
    public static void end(int color, float alpha, float time, float scale) {
        end(color, 0xFFFFFF, alpha, time, scale);
    }
    
    public static void end(int color, int patternColor, float alpha, float time, float scale) {
        if (disabledAfterError) {
            return;
        }
        if (pipeline == null || vertexCount == 0 || uniformBuffer == null || waterDataBuffer == null || vertexBuffer == null) {
            return;
        }
        if (!Float.isFinite(alpha) || !Float.isFinite(time) || !Float.isFinite(scale)) {
            vertexCount = 0;
            return;
        }
        alpha = Math.clamp(alpha, 0.0f, 1.0f);
        scale = Math.clamp(scale, 0.05f, 4.0f);
        
        int count = Math.min(vertexCount, MAX_VERTICES);
        
        Matrix4f combined = combinedMatrix.set(projectionMatrix).mul(viewMatrix);
        if (!isFiniteMatrix(combined)) {
            vertexCount = 0;
            return;
        }
        ByteBuffer uniformData = MemoryUtil.memAlloc(UNIFORM_SIZE);
        putMatrix(uniformData, combined);
        putMatrix(uniformData, identityMatrix);
        uniformData.flip();
        
        ByteBuffer waterData = MemoryUtil.memAlloc(WATER_DATA_SIZE);
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        waterData.putFloat(r).putFloat(g).putFloat(b).putFloat(alpha);
        
        float pr = ((patternColor >> 16) & 0xFF) / 255.0f;
        float pg = ((patternColor >> 8) & 0xFF) / 255.0f;
        float pb = (patternColor & 0xFF) / 255.0f;
        waterData.putFloat(pr).putFloat(pg).putFloat(pb).putFloat(scale);
        waterData.putFloat(time);
        waterData.flip();
        
        ByteBuffer vertexData = MemoryUtil.memAlloc(count * VERTEX_SIZE);
        for (int i = 0; i < count; i++) {
            int offset = i * VERTEX_STRIDE;
            vertexData.putFloat(vertices[offset]).putFloat(vertices[offset + 1]).putFloat(vertices[offset + 2]);
            vertexData.putInt(0xFFFFFFFF);
            vertexData.putFloat(vertices[offset + 3]).putFloat(vertices[offset + 4]);
        }
        vertexData.flip();
        
        try {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(), uniformData);
            encoder.writeToBuffer(waterDataBuffer.slice(), waterData);
            encoder.writeToBuffer(vertexBuffer.slice(), vertexData);
            
            Framebuffer framebuffer = mc.getFramebuffer();
            if (framebuffer == null || framebuffer.getColorAttachmentView() == null || framebuffer.getDepthAttachmentView() == null) {
                vertexCount = 0;
                return;
            }
            try (RenderPass pass = encoder.createRenderPass(
                () -> "WaterCaustic3D",
                framebuffer.getColorAttachmentView(),
                OptionalInt.empty(),
                framebuffer.getDepthAttachmentView(),
                OptionalDouble.empty()
            )) {
                pass.setPipeline(pipeline);
                pass.setUniform("Uniforms", uniformBuffer);
                pass.setUniform("WaterData", waterDataBuffer);
                pass.setVertexBuffer(0, vertexBuffer);
                pass.draw(0, count);
            }
        } catch (Throwable ignored) {
            disabledAfterError = true;
        } finally {
            MemoryUtil.memFree(uniformData);
            MemoryUtil.memFree(waterData);
            MemoryUtil.memFree(vertexData);
        }
        
        vertexCount = 0;
    }
    
    private static boolean isFiniteMatrix(Matrix4f m) {
        return Float.isFinite(m.m00()) && Float.isFinite(m.m01()) && Float.isFinite(m.m02()) && Float.isFinite(m.m03())
            && Float.isFinite(m.m10()) && Float.isFinite(m.m11()) && Float.isFinite(m.m12()) && Float.isFinite(m.m13())
            && Float.isFinite(m.m20()) && Float.isFinite(m.m21()) && Float.isFinite(m.m22()) && Float.isFinite(m.m23())
            && Float.isFinite(m.m30()) && Float.isFinite(m.m31()) && Float.isFinite(m.m32()) && Float.isFinite(m.m33());
    }
    
    private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
        buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
        buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
        buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
        buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
    }
    
}
