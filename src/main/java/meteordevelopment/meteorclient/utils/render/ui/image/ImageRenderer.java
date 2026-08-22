package meteordevelopment.meteorclient.utils.render.ui.image;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.DrawContextAccessor;
import meteordevelopment.meteorclient.mixininterface.IGuiRenderStateLayer;
import meteordevelopment.meteorclient.utils.render.ScissorUtil;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ImageRenderer implements AutoCloseable {
    
    private static final int MAX_IMAGES = 1024;
    private static final int PARAMS_PER_IMAGE = 3;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = MAX_IMAGES * PARAMS_PER_IMAGE * FLOATS_PER_PARAM * Float.BYTES;
    
    private static volatile ImageRenderer instance;
    
    private static final VertexFormat IMAGE_VERTEX_FORMAT = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("Color", VertexFormatElement.COLOR)
        .add("LineWidth", VertexFormatElement.LINE_WIDTH)
        .build();
    
    public static final RenderPipeline IMAGE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/image"))
        .withVertexShader(MeteorClient.identifier("ui/image/image"))
        .withFragmentShader(MeteorClient.identifier("ui/image/image"))
        .withVertexFormat(IMAGE_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("ImageParamsArray", UniformType.UNIFORM_BUFFER)
        .build();
    
    private final Map<TextureCacheKey, CachedTexture> textures = new HashMap<>();
    private final Map<FrameBatchKey, ImageRenderState> frameBatches = new LinkedHashMap<>(32);
    private final java.util.List<ImageQuad> preparedImages = new java.util.ArrayList<>(128);
    private DrawContext activeContext;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty;
    
    private ImageRenderer() {}
    
    public static ImageRenderer getInstance() {
        ImageRenderer local = instance;
        if (local == null) {
            synchronized (ImageRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new ImageRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public static void closeInstance() {
        ImageRenderer local = instance;
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
    
    public void enqueue(BuiltImage image) {
        submit(activeContext, image);
    }
    
    public void flush() {
        activeContext = null;
        frameBatches.clear();
    }
    
    public void barrier() {
        frameBatches.clear();
    }
    
    public void beginGuiFrame() {
        preparedImages.clear();
        paramsDirty = false;
    }
    
    public boolean isImagePipeline(RenderPipeline pipeline) {
        return pipeline == IMAGE_PIPELINE;
    }
    
    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || preparedImages.isEmpty()) {
            return;
        }
        
        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("ImageParamsArray", buffer);
        }
    }
    
    public void prepareBuffers() {
        if (preparedImages.isEmpty() || !paramsDirty) {
            return;
        }
        
        GpuBuffer buffer = ensureWritableParamsBuffer();
        if (buffer == null) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer uniformData = buildUniformData(stack);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(buffer.slice(0, uniformData.remaining()), uniformData);
            paramsDirty = false;
        } catch (RuntimeException ignored) {
            paramsDirty = true;
        }
    }
    
    int reserve(ImageQuad image) {
        int index = preparedImages.size();
        if (index == MAX_IMAGES) {
            return -1;
        }
        
        preparedImages.add(image);
        paramsDirty = true;
        return index;
    }
    
    private void submit(DrawContext context, BuiltImage image) {
        if (context == null || image == null || !image.visible()) {
            return;
        }
        
        ImageTexture texture = resolveTexture(image.texture(), image.nearestFilter());
        if (texture == null) {
            return;
        }
        
        float width = image.hasExplicitDimensions() ? image.explicitWidth() : texture.drawWidth(image.size());
        float height = image.hasExplicitDimensions() ? image.explicitHeight() : texture.drawHeight(image.size());
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        
        float maxRadius = Math.max(0.0f, Math.min(width, height) * 0.5f);
        float rotation = image.rotationDegrees();
        float radians = (float) Math.toRadians(rotation);
        ImageQuad quad = new ImageQuad(
            image.x(),
            image.y(),
            width,
            height,
            clamp(image.radius(), 0.0f, maxRadius),
            Math.max(0.001f, image.smoothness()),
            rotation,
            image.rotationOriginX(),
            image.rotationOriginY(),
            (float) Math.cos(radians),
            (float) Math.sin(radians),
            clamp(image.u0(), 0.0f, 1.0f),
            clamp(image.v0(), 0.0f, 1.0f),
            clamp(image.u1(), 0.0f, 1.0f),
            clamp(image.v1(), 0.0f, 1.0f),
            normalizeColor(image.colorTopLeft()),
            normalizeColor(image.colorTopRight()),
            normalizeColor(image.colorBottomRight()),
            normalizeColor(image.colorBottomLeft())
        );
        
        try {
            GuiRenderState guiState = ((DrawContextAccessor) context).meteor$getState();
            int layerSerial = ((IGuiRenderStateLayer) guiState).meteor$getLayerSerial();
            Matrix3x2f pose = Render2D.pose(context);
            ScreenRect scissorArea = ScissorUtil.current();
            FrameBatchKey key = new FrameBatchKey(guiState, layerSerial, texture.id(), texture.nearestFilter(), PoseKey.of(pose), scissorArea);
            ImageRenderState state = frameBatches.get(key);
            if (state == null) {
                state = new ImageRenderState(pose, texture, scissorArea);
                state.add(quad);
                frameBatches.put(key, state);
                guiState.addSimpleElement(state);
            } else {
                state.add(quad);
            }
        } catch (RuntimeException exception) {
        }
    }
    
    private ImageTexture resolveTexture(Identifier identifier, boolean nearestFilter) {
        if (identifier == null) {
            return null;
        }
        
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft == null) {
            return null;
        }
        
        TextureManager textureManager = minecraft.getTextureManager();
        if (textureManager == null) {
            return null;
        }
        
        AbstractTexture texture = textureManager.getTexture(identifier);
        if (texture == null || texture.getGlTextureView() == null || texture.getGlTexture() == null) {
            return null;
        }
        
        TextureCacheKey key = new TextureCacheKey(identifier, nearestFilter);
        CachedTexture cached = textures.get(key);
        if (cached != null && cached.texture() == texture) {
            return cached.value();
        }
        
        int width = Math.max(1, texture.getGlTexture().getWidth(0));
        int height = Math.max(1, texture.getGlTexture().getHeight(0));
        TextureSetup setup = TextureSetup.of(
            texture.getGlTextureView(),
            RenderSystem.getSamplerCache().get(nearestFilter ? FilterMode.NEAREST : FilterMode.LINEAR)
        );
        ImageTexture value = new ImageTexture(identifier, setup, nearestFilter, width, height);
        textures.put(key, new CachedTexture(texture, value));
        return value;
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
            ByteBuffer uniformData = buildUniformData(stack);
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "meteor_image_params", GpuBuffer.USAGE_UNIFORM, uniformData);
            paramsDirty = false;
            return paramsBuffer;
        }
    }
    
    private GpuBuffer ensureWritableParamsBuffer() {
        if (paramsBuffer != null && !paramsBuffer.isClosed() && paramsBuffer.size() >= UNIFORM_BYTES) {
            return paramsBuffer;
        }
        
        closeParamsBuffer();
        
        try {
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_image_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    private ByteBuffer buildUniformData(MemoryStack stack) {
        int usedBytes = Math.max(1, preparedImages.size()) * PARAMS_PER_IMAGE * FLOATS_PER_PARAM * Float.BYTES;
        ByteBuffer data = stack.calloc(usedBytes);
        for (int i = 0; i < preparedImages.size(); i++) {
            ImageQuad image = preparedImages.get(i);
            int offset = i * PARAMS_PER_IMAGE * FLOATS_PER_PARAM * Float.BYTES;
            data.putFloat(offset, image.radius());
            data.putFloat(offset + 4, image.radius());
            data.putFloat(offset + 8, image.radius());
            data.putFloat(offset + 12, image.radius());
            
            data.putFloat(offset + 16, image.width());
            data.putFloat(offset + 20, image.height());
            data.putFloat(offset + 24, image.smoothness());
            data.putFloat(offset + 28, 0.0f);
            
            data.putFloat(offset + 32, image.u0());
            data.putFloat(offset + 36, image.v0());
            data.putFloat(offset + 40, image.u1());
            data.putFloat(offset + 44, image.v1());
        }
        data.position(0);
        return data;
    }
    
    private static int normalizeColor(int color) {
        if ((color & 0xFF000000) == 0 && (color & 0x00FFFFFF) != 0) {
            return color | 0xFF000000;
        }
        return color;
    }
    
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    private void closeParamsBuffer() {
        if (paramsBuffer != null) {
            paramsBuffer.close();
            paramsBuffer = null;
        }
    }
    
    private static Identifier id(String path) {
        return Identifier.of("meteor", path);
    }
    
    @Override
    public void close() {
        frameBatches.clear();
        preparedImages.clear();
        textures.clear();
        activeContext = null;
        closeParamsBuffer();
    }
    
    private record FrameBatchKey(GuiRenderState state, int layerSerial, Identifier texture, boolean nearestFilter, PoseKey pose, ScreenRect scissorArea) {
    
    }
    
    private record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
        
        static PoseKey of(Matrix3x2f matrix) {
            return new PoseKey(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21());
        }
        
    }
    
    private record CachedTexture(AbstractTexture texture, ImageTexture value) {
    
    }
    
    private record TextureCacheKey(Identifier id, boolean nearestFilter) {
    
    }
    
}
