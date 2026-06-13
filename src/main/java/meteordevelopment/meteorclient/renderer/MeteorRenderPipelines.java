/*
 * This file is part of the Meteor Client distribution.
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class MeteorRenderPipelines {
    
    private static List<RenderPipeline> PIPELINES;
    
    // Snippets
    
    private static final RenderPipeline.Snippet MESH_UNIFORMS = RenderPipeline.builder()
        .withUniform("MeshData", UniformType.UNIFORM_BUFFER)
        .buildSnippet();
    
    // World
    
    public static RenderPipeline WORLD_COLORED;
    public static RenderPipeline WORLD_COLORED_LINES;
    public static RenderPipeline WORLD_COLORED_DEPTH;
    public static RenderPipeline WORLD_COLORED_LINES_DEPTH;
    
    // UI
    
    public static RenderPipeline UI_COLORED;
    public static RenderPipeline UI_COLORED_LINES;
    public static RenderPipeline UI_TEXTURED;
    public static RenderPipeline UI_TEXT;
    
    // Post Process
    
    public static RenderPipeline POST_OUTLINE;
    public static RenderPipeline POST_IMAGE;
    
    // Blur
    
    public static RenderPipeline BLUR_DOWN;
    public static RenderPipeline BLUR_UP;
    public static RenderPipeline PASSTHROUGH;
    public static RenderPipeline BLIT;
    
    // Test
    
    public static RenderPipeline UI_RECTANGLE;
    public static RenderPipeline UI_TEXTURE;
    
    // Blur test
    
    public static RenderPipeline KAWASE_BLUR_DOWN;
    public static RenderPipeline KAWASE_BLUR_UP;
    
    static {
        initPipelines();
    }
    
    private static void initPipelines() {
        PIPELINES = new ArrayList<>();
        
        // World
        WORLD_COLORED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/world_colored"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
        );
        
        WORLD_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLineSmooth()
            .withLocation(MeteorClient.identifier("pipeline/world_colored_lines"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
        );
        
        WORLD_COLORED_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/world_colored_depth"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
        );
        
        WORLD_COLORED_LINES_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLineSmooth()
            .withLocation(MeteorClient.identifier("pipeline/world_colored_lines_depth"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
        );
        
        // UI
        UI_COLORED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/ui_colored"))
            .withVertexFormat(MeteorVertexFormats.POS2_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
        );
        
        UI_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/ui_colored_lines"))
            .withVertexFormat(MeteorVertexFormats.POS2_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
        );
        
        UI_TEXTURED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/ui_textured"))
            .withVertexFormat(MeteorVertexFormats.POS2_TEXTURE_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/pos_tex_color.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/pos_tex_color.frag"))
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
        );
        
        UI_TEXT = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/ui_text"))
            .withVertexFormat(MeteorVertexFormats.POS2_TEXTURE_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/text.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/text.frag"))
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
        );
        
        // Post Process
        POST_OUTLINE = add(new ExtendedRenderPipelineBuilder()
            .withLocation(MeteorClient.identifier("pipeline/post/outline"))
            .withVertexFormat(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/post/base.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/post/outline.frag"))
            .withSampler("u_Texture")
            .withUniform("PostData", UniformType.UNIFORM_BUFFER)
            .withUniform("OutlineData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
        );
        
        POST_IMAGE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/post/image"))
            .withVertexFormat(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/post/base.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/post/image.frag"))
            .withSampler("u_Texture")
            .withSampler("u_TextureI")
            .withUniform("PostData", UniformType.UNIFORM_BUFFER)
            .withUniform("ImageData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
        );
        
        // Blur
        BLUR_DOWN = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/blur/down"))
            .withVertexFormat(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/blur.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/blur_down.frag"))
            .withSampler("u_Texture")
            .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
        );
        
        BLUR_UP = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/blur/up"))
            .withVertexFormat(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/blur.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/blur_up.frag"))
            .withSampler("u_Texture")
            .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
        );
        
        PASSTHROUGH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/blur/passthrough"))
            .withVertexFormat(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/passthrough.frag"))
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
            .withCull(false)
            .build()
        );
        
        BLIT = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/blur/passthrough"))
            .withVertexFormat(MeteorVertexFormats.POS2_TEXTURE, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/blit.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/passthrough.frag"))
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
            .withCull(false)
            .build()
        );
        
        // Test
        UI_RECTANGLE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/ui_rectangle"))
            .withVertexFormat(MeteorVertexFormats.POS2_TEXTURE_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/rectangle.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/rectangle.frag"))
            .withUniform("RectangleData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
        );
        
        UI_TEXTURE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/ui_texture"))
            .withVertexFormat(MeteorVertexFormats.POS2_TEXTURE_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/rectangle.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/texture.frag"))
            .withSampler("u_Texture")
            .withUniform("TextureData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
        );
        
        // Blur test
        KAWASE_BLUR_DOWN = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/kawase_blur/down"))
            .withVertexFormat(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/kawase_blur.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/kawase_blur_down.frag"))
            .withSampler("u_Texture")
            .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
            .withCull(false)
            .build()
        );
        
        KAWASE_BLUR_UP = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(MeteorClient.identifier("pipeline/kawase_blur/up"))
            .withVertexFormat(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(MeteorClient.identifier("shaders/kawase_blur.vert"))
            .withFragmentShader(MeteorClient.identifier("shaders/kawase_blur_up.frag"))
            .withSampler("u_Texture")
            .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
            .withCull(false)
            .build()
        );
    }
    
    private static RenderPipeline add(RenderPipeline pipeline) {
        PIPELINES.add(pipeline);
        return pipeline;
    }
    
    public static void precompile() {
        GpuDevice device = RenderSystem.getDevice();
        ResourceManager resources = mc.getResourceManager();
        
        for (RenderPipeline pipeline : PIPELINES) {
            device.precompilePipeline(pipeline, (identifier, shaderType) -> {
                var resource = resources.getResource(identifier).get();
                
                try (var in = resource.getInputStream()) {
                    return IOUtils.toString(in, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
    
    public static void reload() {
        ShaderLoader loader = mc.getShaderLoader();
        loader.close();
        
        mc.gameRenderer.preloadPrograms(mc.getResourceManager());
        
        initPipelines();
        precompile();
    }
    
    private MeteorRenderPipelines() {}
    
}
