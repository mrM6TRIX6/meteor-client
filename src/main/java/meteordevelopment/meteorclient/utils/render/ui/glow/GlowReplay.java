package meteordevelopment.meteorclient.utils.render.ui.glow;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.utils.render.ui.UiRenderers;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.BufferAllocator;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Replays captured gui render states into the glow atlas.
 * <p>
 * The trick is that nothing about the captured elements changes - same pipeline, same shader, same uniform block -
 * only the destination framebuffer and two uniform blocks do. {@code Projection} becomes an ortho over the atlas in
 * atlas texel space and {@code DynamicTransforms.ModelViewMat} slides each group's content into its own tile, so a
 * captured arc or msdf glyph lands exactly where the tile packer put it.
 * <p>
 * All vertex data for the frame lives in one grow-only gpu buffer per vertex format, all transform blocks in one
 * grow-only uniform buffer, and the whole thing is issued as a single render pass.
 */
final class GlowReplay implements AutoCloseable {

    private static final int TRANSFORMS_BYTES = 160;
    private static final int ALLOCATOR_BYTES = 1 << 16;

    private final Map<FormatKey, Bucket> buckets = new HashMap<>(4);
    private final List<Bucket> bucketList = new ArrayList<>(4);
    private final List<Draw> draws = new ArrayList<>(64);
    private final CountingVertexConsumer counter = new CountingVertexConsumer();
    private final Matrix4f scratchMatrix = new Matrix4f();

    private int drawCount;
    private int maxIndexCount;

    private GpuBuffer transformsBuffer;
    private ByteBuffer transformsStaging;
    private int transformsStride;
    private int transformsCount;

    private GpuBuffer projectionBuffer;
    private ByteBuffer projectionStaging;
    private int projectionWidth;
    private int projectionHeight;

    void begin() {
        drawCount = 0;
        maxIndexCount = 0;
        transformsCount = 0;

        for (int i = 0; i < bucketList.size(); i++) {
            bucketList.get(i).begin();
        }
    }

    boolean isEmpty() {
        return drawCount == 0;
    }

    /**
     * Runs {@code setupVertices} for every captured state of the group and records the resulting draws.
     * <p>
     * Must happen while the other ui renderers are still collecting, because {@code setupVertices} is what makes
     * an element reserve its slot in its own params uniform block.
     */
    void record(GlowGroup group) {
        if (group.kind != GlowGroup.CAPTURED || !group.placed() || group.states.isEmpty()) {
            group.replayFrom = drawCount;
            group.replayTo = drawCount;
            return;
        }

        float scale = group.resolution;
        float offsetX = group.tileX - (group.contentX - group.pad) * scale;
        float offsetY = group.tileY - (group.contentY - group.pad) * scale;
        long transformsOffset = writeTransforms(offsetX, offsetY, scale);
        group.transformsOffset = transformsOffset;

        int tileLeft = group.tileX;
        int tileTop = group.tileY;
        int tileRight = group.tileX + group.tileWidth;
        int tileBottom = group.tileY + group.tileHeight;

        group.replayFrom = drawCount;

        for (int i = 0; i < group.states.size(); i++) {
            SimpleGuiElementRenderState state = group.states.get(i);

            try {
                RenderPipeline pipeline = state.pipeline();
                if (pipeline == null) {
                    continue;
                }

                Bucket bucket = bucket(pipeline.getVertexFormat(), pipeline.getVertexFormatMode());
                if (bucket == null) {
                    continue;
                }

                int baseVertex = bucket.vertexCount;
                counter.delegate = bucket.builder;
                counter.count = 0;
                state.setupVertices(counter);
                int vertexCount = counter.count;
                counter.delegate = null;
                if (vertexCount <= 0) {
                    continue;
                }
                bucket.vertexCount += vertexCount;

                int scissorLeft = tileLeft;
                int scissorTop = tileTop;
                int scissorRight = tileRight;
                int scissorBottom = tileBottom;

                ScreenRect scissorArea = state.scissorArea();
                if (scissorArea != null) {
                    scissorLeft = Math.max(scissorLeft, (int) Math.floor(scissorArea.getLeft() * scale + offsetX));
                    scissorTop = Math.max(scissorTop, (int) Math.floor(scissorArea.getTop() * scale + offsetY));
                    scissorRight = Math.min(scissorRight, (int) Math.ceil(scissorArea.getRight() * scale + offsetX));
                    scissorBottom = Math.min(scissorBottom, (int) Math.ceil(scissorArea.getBottom() * scale + offsetY));
                }

                int scissorWidth = scissorRight - scissorLeft;
                int scissorHeight = scissorBottom - scissorTop;
                if (scissorWidth <= 0 || scissorHeight <= 0) {
                    continue;
                }

                TextureSetup setup = state.textureSetup();
                append(bucket, pipeline, setup, transformsOffset, baseVertex, vertexCount, scissorLeft, scissorTop, scissorWidth, scissorHeight);
            } catch (RuntimeException ignored) {
                counter.delegate = null;
            }
        }

        group.replayTo = drawCount;
    }

    private void append(
        Bucket bucket,
        RenderPipeline pipeline,
        TextureSetup setup,
        long transformsOffset,
        int baseVertex,
        int vertexCount,
        int scissorX,
        int scissorY,
        int scissorWidth,
        int scissorHeight
    ) {
        if (drawCount > 0) {
            Draw previous = draws.get(drawCount - 1);
            if (previous.bucket == bucket
                && previous.pipeline == pipeline
                && previous.transformsOffset == transformsOffset
                && previous.scissorX == scissorX
                && previous.scissorY == scissorY
                && previous.scissorWidth == scissorWidth
                && previous.scissorHeight == scissorHeight
                && previous.baseVertex + previous.vertexCount == baseVertex
                && sameSetup(previous.setup, setup)) {
                previous.vertexCount += vertexCount;
                maxIndexCount = Math.max(maxIndexCount, bucket.mode.getIndexCount(previous.vertexCount));
                return;
            }
        }

        Draw draw;
        if (drawCount < draws.size()) {
            draw = draws.get(drawCount);
        } else {
            draw = new Draw();
            draws.add(draw);
        }
        drawCount++;

        draw.bucket = bucket;
        draw.pipeline = pipeline;
        draw.setup = setup;
        draw.transformsOffset = transformsOffset;
        draw.baseVertex = baseVertex;
        draw.vertexCount = vertexCount;
        draw.scissorX = scissorX;
        draw.scissorY = scissorY;
        draw.scissorWidth = scissorWidth;
        draw.scissorHeight = scissorHeight;

        maxIndexCount = Math.max(maxIndexCount, bucket.mode.getIndexCount(vertexCount));
    }

    private static boolean sameSetup(TextureSetup a, TextureSetup b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.texure0() == b.texure0()
            && a.texure1() == b.texure1()
            && a.texure2() == b.texure2()
            && a.sampler0() == b.sampler0()
            && a.sampler1() == b.sampler1()
            && a.sampler2() == b.sampler2();
    }

    /** Uploads the collected vertex data and transform blocks. Called once, right before {@link #render}. */
    void upload() {
        if (drawCount == 0) {
            return;
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        for (int i = 0; i < bucketList.size(); i++) {
            bucketList.get(i).upload(encoder);
        }

        if (transformsCount > 0) {
            int bytes = transformsCount * transformsStride;
            GpuBuffer buffer = ensureTransformsBuffer(bytes);
            if (buffer != null) {
                transformsStaging.position(0).limit(bytes);
                encoder.writeToBuffer(buffer.slice(0L, bytes), transformsStaging);
                transformsStaging.position(0).limit(transformsStaging.capacity());
            }
        }
    }

    void render(GpuTextureView atlasView, int atlasWidth, int atlasHeight) {
        if (drawCount == 0 || atlasView == null) {
            return;
        }

        GpuBuffer projection = ensureProjectionBuffer(atlasWidth, atlasHeight);
        if (projection == null || transformsBuffer == null) {
            return;
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        RenderSystem.ShapeIndexBuffer sequential = null;
        GpuBuffer indexBuffer = null;

        try (RenderPass pass = encoder.createRenderPass(
            () -> "meteor_glow_replay",
            atlasView,
            OptionalInt.empty(),
            null,
            OptionalDouble.empty()
        )) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("Projection", projection);

            Bucket boundBucket = null;
            VertexFormat.DrawMode boundMode = null;
            long boundTransforms = Long.MIN_VALUE;

            for (int i = 0; i < drawCount; i++) {
                Draw draw = draws.get(i);
                if (draw.vertexCount <= 0 || draw.bucket.buffer == null) {
                    continue;
                }

                pass.setPipeline(draw.pipeline);
                pass.setUniform("Projection", projection);
                UiRenderers.bindParams(draw.pipeline, pass);

                if (draw.transformsOffset != boundTransforms) {
                    pass.setUniform("DynamicTransforms", transformsBuffer.slice(draw.transformsOffset, TRANSFORMS_BYTES));
                    boundTransforms = draw.transformsOffset;
                }

                if (boundBucket != draw.bucket) {
                    pass.setVertexBuffer(0, draw.bucket.buffer);
                    boundBucket = draw.bucket;
                }

                if (boundMode != draw.bucket.mode) {
                    sequential = RenderSystem.getSequentialBuffer(draw.bucket.mode);
                    indexBuffer = sequential.getIndexBuffer(maxIndexCount);
                    pass.setIndexBuffer(indexBuffer, sequential.getIndexType());
                    boundMode = draw.bucket.mode;
                }

                TextureSetup setup = draw.setup;
                if (setup != null) {
                    if (setup.texure0() != null) {
                        pass.bindTexture("Sampler0", setup.texure0(), setup.sampler0());
                    }
                    if (setup.texure1() != null) {
                        pass.bindTexture("Sampler1", setup.texure1(), setup.sampler1());
                    }
                    if (setup.texure2() != null) {
                        pass.bindTexture("Sampler2", setup.texure2(), setup.sampler2());
                    }
                }

                pass.enableScissor(draw.scissorX, draw.scissorY, draw.scissorWidth, draw.scissorHeight);
                pass.drawIndexed(draw.baseVertex, 0, draw.bucket.mode.getIndexCount(draw.vertexCount), 1);
            }
        } catch (RuntimeException ignored) {
        }
    }

    /** Releases the transient built buffers so the allocators can be reused next frame. */
    void endFrame() {
        for (int i = 0; i < bucketList.size(); i++) {
            bucketList.get(i).endFrame();
        }

        for (int i = 0; i < drawCount; i++) {
            Draw draw = draws.get(i);
            draw.bucket = null;
            draw.pipeline = null;
            draw.setup = null;
        }

        drawCount = 0;
    }

    private long writeTransforms(float offsetX, float offsetY, float scale) {
        int alignment = Math.max(1, RenderSystem.getDevice().getUniformOffsetAlignment());
        if (transformsStride == 0) {
            transformsStride = (TRANSFORMS_BYTES + alignment - 1) / alignment * alignment;
        }

        int index = transformsCount;
        int required = (index + 1) * transformsStride;
        if (transformsStaging == null || transformsStaging.capacity() < required) {
            int capacity = Math.max(required, Math.max(transformsStride * 16, transformsStaging == null ? 0 : transformsStaging.capacity() * 2));
            ByteBuffer grown = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
            if (transformsStaging != null) {
                transformsStaging.position(0).limit(index * transformsStride);
                grown.put(transformsStaging);
                transformsStaging.limit(transformsStaging.capacity());
            }
            grown.position(0).limit(capacity);
            transformsStaging = grown;
        }

        int base = index * transformsStride;
        scratchMatrix.identity().translation(offsetX, offsetY, 0.0f).scale(scale);
        scratchMatrix.get(base, transformsStaging);

        // ColorModulator
        transformsStaging.putFloat(base + 64, 1.0f);
        transformsStaging.putFloat(base + 68, 1.0f);
        transformsStaging.putFloat(base + 72, 1.0f);
        transformsStaging.putFloat(base + 76, 1.0f);
        // ModelOffset
        transformsStaging.putFloat(base + 80, 0.0f);
        transformsStaging.putFloat(base + 84, 0.0f);
        transformsStaging.putFloat(base + 88, 0.0f);
        transformsStaging.putFloat(base + 92, 0.0f);
        // TextureMat
        scratchMatrix.identity().get(base + 96, transformsStaging);

        transformsCount++;
        return base;
    }

    private GpuBuffer ensureTransformsBuffer(int bytes) {
        if (transformsBuffer != null && !transformsBuffer.isClosed() && transformsBuffer.size() >= bytes) {
            return transformsBuffer;
        }
        if (transformsBuffer != null) {
            transformsBuffer.close();
            transformsBuffer = null;
        }
        try {
            transformsBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_glow_replay_transforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                Math.max(bytes, transformsStride * 16L)
            );
            return transformsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private GpuBuffer ensureProjectionBuffer(int width, int height) {
        if (projectionBuffer != null && !projectionBuffer.isClosed() && projectionWidth == width && projectionHeight == height) {
            return projectionBuffer;
        }
        if (projectionBuffer == null || projectionBuffer.isClosed()) {
            try {
                projectionBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "meteor_glow_replay_projection",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    64L
                );
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        if (projectionStaging == null) {
            projectionStaging = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
        }

        // Ortho over the atlas in atlas texel space. Atlas row 0 sits at ndc -1, which is exactly the orientation
        // glow_composite.fsh expects: screen-top of the composite quad samples the tile's lower atlas edge.
        scratchMatrix.set(
            2.0f / Math.max(1, width), 0.0f, 0.0f, 0.0f,
            0.0f, 2.0f / Math.max(1, height), 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 1.0f
        );
        scratchMatrix.get(0, projectionStaging);
        projectionStaging.position(0).limit(64);

        try {
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(projectionBuffer.slice(0L, 64L), projectionStaging);
        } catch (RuntimeException ignored) {
            return null;
        } finally {
            projectionStaging.position(0).limit(64);
        }

        projectionWidth = width;
        projectionHeight = height;
        return projectionBuffer;
    }

    private Bucket bucket(VertexFormat format, VertexFormat.DrawMode mode) {
        if (format == null || mode == null) {
            return null;
        }
        FormatKey key = new FormatKey(format, mode);
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            bucket = new Bucket(format, mode);
            bucket.begin();
            buckets.put(key, bucket);
            bucketList.add(bucket);
        }
        return bucket;
    }

    @Override
    public void close() {
        for (int i = 0; i < bucketList.size(); i++) {
            bucketList.get(i).close();
        }
        bucketList.clear();
        buckets.clear();
        draws.clear();
        drawCount = 0;

        if (transformsBuffer != null) {
            transformsBuffer.close();
            transformsBuffer = null;
        }
        if (projectionBuffer != null) {
            projectionBuffer.close();
            projectionBuffer = null;
        }
        transformsStaging = null;
        projectionStaging = null;
        projectionWidth = 0;
        projectionHeight = 0;
    }

    private static final class Bucket {

        private final VertexFormat format;
        private final VertexFormat.DrawMode mode;
        private final BufferAllocator allocator;

        private BufferBuilder builder;
        private BuiltBuffer built;
        private GpuBuffer buffer;
        private int vertexCount;

        Bucket(VertexFormat format, VertexFormat.DrawMode mode) {
            this.format = format;
            this.mode = mode;
            this.allocator = new BufferAllocator(ALLOCATOR_BYTES);
        }

        void begin() {
            vertexCount = 0;
            builder = new BufferBuilder(allocator, mode, format);
        }

        void upload(CommandEncoder encoder) {
            if (builder == null || vertexCount <= 0) {
                return;
            }

            built = builder.endNullable();
            builder = null;
            if (built == null) {
                return;
            }

            ByteBuffer data = built.getBuffer();
            int bytes = data.remaining();
            if (bytes <= 0) {
                return;
            }

            if (buffer == null || buffer.isClosed() || buffer.size() < bytes) {
                if (buffer != null) {
                    buffer.close();
                }
                try {
                    long capacity = Math.max(bytes, ALLOCATOR_BYTES);
                    buffer = RenderSystem.getDevice().createBuffer(
                        () -> "meteor_glow_replay_vertices",
                        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                        capacity
                    );
                } catch (RuntimeException ignored) {
                    buffer = null;
                    return;
                }
            }

            try {
                encoder.writeToBuffer(buffer.slice(0L, bytes), data);
            } catch (RuntimeException ignored) {
            }
        }

        void endFrame() {
            if (builder != null) {
                BuiltBuffer leftover = builder.endNullable();
                builder = null;
                if (leftover != null) {
                    leftover.close();
                }
            }
            if (built != null) {
                built.close();
                built = null;
            }
            vertexCount = 0;
        }

        void close() {
            endFrame();
            if (buffer != null) {
                buffer.close();
                buffer = null;
            }
            allocator.close();
        }

    }

    private static final class Draw {

        Bucket bucket;
        RenderPipeline pipeline;
        TextureSetup setup;
        long transformsOffset;
        int baseVertex;
        int vertexCount;
        int scissorX;
        int scissorY;
        int scissorWidth;
        int scissorHeight;

    }

    private record FormatKey(VertexFormat format, VertexFormat.DrawMode mode) {}

    /**
     * Counts vertices as they stream into the shared {@link BufferBuilder}, because {@code BufferBuilder} keeps its
     * own count private and we need a per-state range to turn into a {@code baseVertex}.
     */
    private static final class CountingVertexConsumer implements VertexConsumer {

        VertexConsumer delegate;
        int count;

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            count++;
            return delegate.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return delegate.color(red, green, blue, alpha);
        }

        @Override
        public VertexConsumer color(int argb) {
            return delegate.color(argb);
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return delegate.texture(u, v);
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return delegate.overlay(u, v);
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return delegate.light(u, v);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return delegate.normal(x, y, z);
        }

        @Override
        public VertexConsumer lineWidth(float width) {
            return delegate.lineWidth(width);
        }

    }

}
