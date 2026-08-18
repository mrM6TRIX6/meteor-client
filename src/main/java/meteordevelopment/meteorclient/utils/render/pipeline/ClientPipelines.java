package meteordevelopment.meteorclient.utils.render.pipeline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.RenderCompatibility;
import meteordevelopment.meteorclient.utils.render.RenderLayerFactory;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

import static net.minecraft.client.gl.RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET;

public final class ClientPipelines {
    
    private static BlendFunction worldBlend() {
        return RenderCompatibility.useSafeWorldEffects() ? BlendFunction.TRANSLUCENT : BlendFunction.LIGHTNING;
    }
    
    public static final RenderPipeline ROMB_ESP_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/wtex"))
            .withVertexShader(MeteorClient.identifier("core/position_tex_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_tex_color"))
            .withSampler("Sampler0")
            .withBlend(worldBlend())
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build()
    );
    
    public static final Function<Identifier, RenderLayer> ROMB_ESP =
        Util.memoize(texture -> RenderLayerFactory.of("wtex", 1536, ROMB_ESP_PIPELINE, texture));
    
    public static final RenderPipeline CLICKGUI_CLOSE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/clickgui_close"))
            .withVertexShader(MeteorClient.identifier("core/position_tex_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_tex_color"))
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build()
    );
    
    public static final Function<Identifier, RenderLayer> CLICKGUI_CLOSE =
        Util.memoize(texture -> RenderLayerFactory.of("clickgui_close", 1536, CLICKGUI_CLOSE_PIPELINE, texture));
    
    public static final RenderPipeline GHOSTS_ESP_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/wtex"))
            .withVertexShader(MeteorClient.identifier("core/position_tex_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_tex_color"))
            .withSampler("Sampler0")
            .withBlend(worldBlend())
            .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build()
    );
    
    public static final Function<Identifier, RenderLayer> GHOSTS_ESP =
        Util.memoize(texture -> RenderLayerFactory.of("wtex", 1536, GHOSTS_ESP_PIPELINE, texture));
    
    public static final RenderPipeline CHAIN_ESP_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/wtex"))
            .withVertexShader(MeteorClient.identifier("core/position_tex_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_tex_color"))
            .withSampler("Sampler0")
            .withBlend(worldBlend())
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build()
    );
    
    public static final Function<Identifier, RenderLayer> CHAIN_ESP =
        Util.memoize(texture -> RenderLayerFactory.of("wtex", 1536, CHAIN_ESP_PIPELINE, texture));
    
    public static final RenderPipeline CRYSTAL_FILLED_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/crystal_filled"))
            .withVertexShader(MeteorClient.identifier("core/position_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_color"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .build()
    );
    
    public static final RenderLayer CRYSTAL_FILLED = RenderLayerFactory.of("crystal_filled", 8192, CRYSTAL_FILLED_PIPELINE);
    
    public static final RenderPipeline CRYSTAL_GLOW_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/crystal_glow"))
            .withVertexShader(MeteorClient.identifier("core/position_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_color"))
            .withBlend(worldBlend())
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .build()
    );
    
    public static final RenderLayer CRYSTAL_GLOW = RenderLayerFactory.of("crystal_glow", 4096, CRYSTAL_GLOW_PIPELINE);
    
    public static final RenderPipeline BLOOM_ESP_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/bloom_esp"))
            .withVertexShader(MeteorClient.identifier("core/position_tex_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_tex_color"))
            .withSampler("Sampler0")
            .withBlend(worldBlend())
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build()
    );
    
    public static final Function<Identifier, RenderLayer> BLOOM_ESP =
        Util.memoize(texture -> RenderLayerFactory.of("bloom_esp", 2048, BLOOM_ESP_PIPELINE, texture));
    
    public static final RenderPipeline CHINA_HAT_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/china_hat"))
            .withVertexShader(MeteorClient.identifier("core/position_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_color"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(true)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_FAN)
            .build()
    );
    
    public static final RenderLayer CHINA_HAT = RenderLayerFactory.of("china_hat", 8192, CHINA_HAT_PIPELINE);
    
    public static final RenderPipeline CHINA_HAT_OUTLINE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/china_hat_outline"))
            .withVertexShader(MeteorClient.identifier("core/position_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_color"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(true)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
            .build()
    );
    
    public static final RenderLayer CHINA_HAT_OUTLINE = RenderLayerFactory.of("china_hat_outline", 4096, CHINA_HAT_OUTLINE_PIPELINE);
    
    public static final RenderPipeline WORLD_PARTICLES_COLOR_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/world_particles_color"))
            .withVertexShader(MeteorClient.identifier("core/position_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_color"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(worldBlend())
            .build()
    );
    
    public static final RenderLayer WORLD_PARTICLES_QUADS = RenderLayerFactory.of("world_particles_cube", 2048, WORLD_PARTICLES_COLOR_PIPELINE);
    
    public static final RenderPipeline WORLD_PARTICLES_LINES_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/world_particles_lines"))
            .withVertexShader(MeteorClient.identifier("core/position_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_color"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(worldBlend())
            .build()
    );
    
    public static final RenderLayer WORLD_PARTICLES_LINES = RenderLayerFactory.of("world_particles_lines", 2048, WORLD_PARTICLES_LINES_PIPELINE);
    
    public static final RenderPipeline WORLD_PARTICLES_GLOW_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/world_particles_glow"))
            .withVertexShader(MeteorClient.identifier("core/position_tex_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_tex_color"))
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(worldBlend())
            .withSampler("Sampler0")
            .build()
    );
    
    public static final Function<Identifier, RenderLayer> WORLD_PARTICLES_GLOW =
        Util.memoize(texture -> RenderLayerFactory.of("world_particles_glow", 2048, WORLD_PARTICLES_GLOW_PIPELINE, texture));
    
    public static final RenderPipeline GUI_ARROW_BLEND_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/gui_arrow_blend"))
            .withVertexShader(MeteorClient.identifier("core/position_tex_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_tex_color"))
            .withSampler("Sampler0")
            .withBlend(BlendFunction.LIGHTNING)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build()
    );
    
    public static final Function<Identifier, RenderLayer> GUI_ARROW_BLEND =
        Util.memoize(texture -> RenderLayerFactory.of("gui_arrow_blend", 256, GUI_ARROW_BLEND_PIPELINE, texture));
    
    public static final RenderPipeline TRAILS_ALPHA_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(MeteorClient.identifier("pipeline/trails_alpha"))
            .withVertexShader(MeteorClient.identifier("core/position_color"))
            .withFragmentShader(MeteorClient.identifier("core/position_color"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .build()
    );
    
    public static final RenderLayer TRAILS_ALPHA = RenderLayerFactory.of("trails_alpha", 16384, TRAILS_ALPHA_PIPELINE);
    
    private ClientPipelines() {}
    
}