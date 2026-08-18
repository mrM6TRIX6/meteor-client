package meteordevelopment.meteorclient.utils.render.ui.effecticon;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.DrawContextAccessor;
import meteordevelopment.meteorclient.mixininterface.IGuiRenderStateLayer;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EffectIconRenderer implements AutoCloseable {
    
    private static volatile EffectIconRenderer instance;
    
    private static final VertexFormat EFFECT_ICON_VERTEX_FORMAT = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("Color", VertexFormatElement.COLOR)
        .build();
    
    public static final RenderPipeline EFFECT_ICON_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/effect_icon"))
        .withVertexShader(MeteorClient.identifier("ui/effect_icon/effect_icon"))
        .withFragmentShader(MeteorClient.identifier("ui/effect_icon/effect_icon"))
        .withVertexFormat(EFFECT_ICON_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final Map<Identifier, CachedTexture> textures = new HashMap<>();
    private final Map<FrameBatchKey, EffectIconRenderState> frameBatches = new LinkedHashMap<>(16);
    private DrawContext activeContext;
    
    private EffectIconRenderer() {}
    
    public static EffectIconRenderer getInstance() {
        EffectIconRenderer local = instance;
        if (local == null) {
            synchronized (EffectIconRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new EffectIconRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        EffectIconRenderer local = instance;
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
    
    public void enqueue(BuiltEffectIcon icon) {
        submit(activeContext, icon);
    }
    
    public void flush() {
        activeContext = null;
        frameBatches.clear();
    }
    
    public void barrier() {
        frameBatches.clear();
    }
    
    private void submit(DrawContext context, BuiltEffectIcon icon) {
        if (context == null || icon == null || !icon.visible()) {
            return;
        }
        
        Sprite sprite = resolveSprite(icon);
        if (sprite == null) {
            return;
        }
        
        EffectIconTexture texture = resolveTexture(sprite.getAtlasId());
        if (texture == null) {
            return;
        }
        
        EffectIconQuad quad = new EffectIconQuad(
            icon.x(),
            icon.y(),
            icon.size(),
            sprite.getMinU(),
            sprite.getMinV(),
            sprite.getMaxU(),
            sprite.getMaxV(),
            normalizeColor(icon.color())
        );
        
        try {
            GuiRenderState guiState = ((DrawContextAccessor) context).meteor$getState();
            int layerSerial = ((IGuiRenderStateLayer) guiState).meteor$getLayerSerial();
            Matrix3x2f pose = Render2D.pose(context);
            FrameBatchKey key = new FrameBatchKey(guiState, layerSerial, texture.id(), PoseKey.of(pose));
            EffectIconRenderState state = frameBatches.get(key);
            if (state == null) {
                state = new EffectIconRenderState(pose, texture);
                state.add(quad);
                frameBatches.put(key, state);
                guiState.addSimpleElement(state);
            } else {
                state.add(quad);
            }
        } catch (RuntimeException exception) {
        }
    }
    
    private Sprite resolveSprite(BuiltEffectIcon icon) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft == null || icon.effect() == null) {
            return null;
        }
        
        SpriteAtlasTexture atlas = minecraft.getAtlasManager().getAtlasTexture(Atlases.GUI);
        Identifier spriteId = InGameHud.getEffectTexture(icon.effect());
        return atlas.getSprite(spriteId);
    }
    
    private EffectIconTexture resolveTexture(Identifier atlasLocation) {
        if (atlasLocation == null) {
            return null;
        }
        
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft == null || minecraft.getTextureManager() == null) {
            return null;
        }
        
        AbstractTexture texture = minecraft.getTextureManager().getTexture(atlasLocation);
        if (texture == null || texture.getGlTextureView() == null) {
            return null;
        }
        
        CachedTexture cached = textures.get(atlasLocation);
        if (cached != null && cached.texture() == texture) {
            return cached.value();
        }
        
        TextureSetup setup = TextureSetup.of(
            texture.getGlTextureView(),
            RenderSystem.getSamplerCache().get(FilterMode.NEAREST)
        );
        EffectIconTexture value = new EffectIconTexture(atlasLocation, setup);
        textures.put(atlasLocation, new CachedTexture(texture, value));
        return value;
    }
    
    private static int normalizeColor(int color) {
        if ((color & 0xFF000000) == 0 && (color & 0x00FFFFFF) != 0) {
            return color | 0xFF000000;
        }
        return color;
    }
    
    private static Identifier id(String path) {
        return Identifier.of("meteor", path);
    }
    
    @Override
    public void close() {
        frameBatches.clear();
        textures.clear();
        activeContext = null;
    }
    
    private record FrameBatchKey(GuiRenderState state, int layerSerial, Identifier texture, PoseKey pose) {
    
    }
    
    private record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
        
        static PoseKey of(Matrix3x2f matrix) {
            return new PoseKey(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21());
        }
        
    }
    
    private record CachedTexture(AbstractTexture texture, EffectIconTexture value) {
    
    }
    
}
