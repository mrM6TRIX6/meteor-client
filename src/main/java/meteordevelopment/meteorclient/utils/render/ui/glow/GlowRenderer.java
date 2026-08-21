package meteordevelopment.meteorclient.utils.render.ui.glow;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.DrawContextAccessor;
import meteordevelopment.meteorclient.utils.render.ScissorUtil;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Draws blurred halos for arbitrary ui content.
 * <p>
 * There are two ways in: {@link #enqueue(BuiltGlow)} for the procedural rounded rect that
 * {@code GLOW_SOURCE_PIPELINE} rasterises from an sdf, and {@link #addShape(GlowShapeOptions, Runnable)} for
 * "whatever this lambda draws". Both end up as a {@link GlowGroup}, and one group is one tile in a shared atlas with
 * its own blur radius.
 * <p>
 * Per frame the work is:
 * <ol>
 *     <li>quantise the requested radii into at most {@link #MAX_BUCKETS} buckets;</li>
 *     <li>shelf-pack every group into the atlas, forcing a new shelf whenever the bucket changes, so each bucket owns
 *     a contiguous band of atlas rows;</li>
 *     <li>rasterise procedural groups and replay captured ones into the shape atlas;</li>
 *     <li>run one kawase dual-filter chain over the whole atlas, six render passes total, where each pass issues one
 *     scissored draw per bucket - so a band only ever gets blurred with its own radius;</li>
 *     <li>subtract the body from the halo (the cutout pass) and let the composite quads sample their tile.</li>
 * </ol>
 * Everything transient - tile quads, per group sdf params, per bucket blur params - lives in one grow-only gpu buffer
 * that is written once and then bound as offset slices, so a frame with 200 glows still allocates nothing.
 */
public final class GlowRenderer implements AutoCloseable {

    private static final int ATLAS_WIDTH = 2048;
    private static final int ATLAS_MAX_HEIGHT = 2048;
    private static final int ATLAS_HEIGHT_BUCKET = 256;
    private static final int MIN_TILE_GAP = 8;

    /** Band edges are kept aligned so {@code bandY >> level} stays exact for every blur level. */
    private static final int BAND_ALIGN = 16;

    private static final int MAX_GROUPS = 224;
    private static final int MAX_BUCKETS = 8;
    private static final int BLUR_LEVELS = 3;
    private static final int BLUR_STAGES = 6;

    /** The composite shader hardcodes {@code QuadIndex * 5} into a {@code vec4[1120]}, so this stride is fixed. */
    private static final int COMPOSITE_STRIDE = 80;
    private static final int COMPOSITE_BYTES = MAX_GROUPS * COMPOSITE_STRIDE;

    private static final int KAWASE_BYTES = 48;
    private static final int KAWASE_SLOTS = BLUR_STAGES * MAX_BUCKETS + 2;
    private static final int CUTOUT_ON_SLOT = BLUR_STAGES * MAX_BUCKETS;
    private static final int CUTOUT_OFF_SLOT = CUTOUT_ON_SLOT + 1;

    private static final int SHAPE_BYTES = 1312;
    private static final int MAX_SPANS = 64;
    private static final int PALETTE_BASE = 1120;
    private static final int CORNER_COLOR_BASE = 78 * 16;

    private static final int QUAD_VERTICES = 4;
    private static final int FULLSCREEN_QUAD = 0;

    private static final GlowShapeOptions DEFAULT_OPTIONS = new GlowShapeOptions();

    public static final RenderPipeline GLOW_COMPOSITE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_composite"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_composite"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_composite"))
        .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS)
        .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline GLOW_SHAPE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_shape"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_shape"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_shape"))
        .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
        .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline GLOW_SOURCE_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_source"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_source"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_shape"))
        .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
        .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline GLOW_BLUR_DOWN_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_blur_down"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_blur"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_blur_down"))
        .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline GLOW_BLUR_UP_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_blur_up"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_blur"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_blur_up"))
        .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline GLOW_CUTOUT_PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/glow_cutout"))
        .withVertexShader(MeteorClient.identifier("ui/glow/glow_blur"))
        .withFragmentShader(MeteorClient.identifier("ui/glow/glow_cutout"))
        .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withCull(false)
        .withSampler("Sampler0")
        .withSampler("Sampler1")
        .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
        .build();

    private static volatile GlowRenderer instance;

    private final List<GlowGroup> groupPool = new ArrayList<>(32);
    private final List<GlowGroup> pending = new ArrayList<>(32);
    private final List<Bucket> bucketPool = new ArrayList<>(MAX_BUCKETS);
    private final List<Bucket> buckets = new ArrayList<>(MAX_BUCKETS);
    private final float[] scratchRadii = new float[MAX_GROUPS];
    private int groupsUsed;

    private final List<GlowCapture> preparedCaptures = new ArrayList<>(32);
    private float[] preparedAlpha = new float[MAX_GROUPS];

    private final GlowReplay replay = new GlowReplay();
    private final GlowShapeOptions scratchOptions = new GlowShapeOptions();

    private SimpleFramebuffer shapeTarget;
    private SimpleFramebuffer blurTarget;
    private SimpleFramebuffer resultTarget;
    private final SimpleFramebuffer[] downTargets = new SimpleFramebuffer[BLUR_LEVELS];
    private final SimpleFramebuffer[] upTargets = new SimpleFramebuffer[BLUR_LEVELS];

    private int atlasWidth;
    private int atlasHeight;
    private TextureSetup resultSetup = TextureSetup.empty();

    private GpuBuffer compositeBuffer;
    private ByteBuffer compositeStaging;
    private boolean compositeDirty;

    private GpuBuffer quadBuffer;
    private ByteBuffer quadStaging;
    private int quadCount;

    private GpuBuffer shapeParamsBuffer;
    private ByteBuffer shapeParamsStaging;
    private int shapeParamsStride;
    private int shapeParamsCount;

    private GpuBuffer kawaseBuffer;
    private ByteBuffer kawaseStaging;
    private int kawaseStride;

    private DrawContext activeGraphics;
    private boolean frameReady;

    private float pendingMaxRadius;
    private float lastMaxRadius;

    private GlowRenderer() {}

    public static GlowRenderer getInstance() {
        GlowRenderer local = instance;
        if (local == null) {
            synchronized (GlowRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new GlowRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }

    public static void closeInstance() {
        GlowRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // submission
    // ------------------------------------------------------------------------------------------------------------

    public void beginFrame(DrawContext context) {
        activeGraphics = context;
    }

    public void flush() {
        activeGraphics = null;
    }

    public void draw(DrawContext context, BuiltGlow glow) {
        beginFrame(context);
        enqueue(glow);
        flush();
    }

    public void enqueue(BuiltGlow glow) {
        DrawContext context = activeGraphics;
        if (context == null || glow == null || !glow.visible()) {
            return;
        }

        // A procedural glow submitted from inside a capture scope would put its own composite quad into the atlas it
        // is sampling from, so it is dropped rather than rendered wrong.
        if (GlowCaptureScope.active() || pending.size() >= MAX_GROUPS) {
            return;
        }

        try {
            GlowGroup group = acquireGroup();
            group.kind = GlowGroup.PROCEDURAL;
            group.glow = glow;
            group.pose.set(Render2D.pose(context));
            group.contentX = glow.x();
            group.contentY = glow.y();
            group.contentWidth = glow.width();
            group.contentHeight = glow.height();
            group.radius = Math.max(0.0f, glow.glowRadius());
            group.expand = 0.0f;
            group.resolution = 1.0f;
            group.intensity = glow.intensity();
            group.alpha = glow.alpha();
            group.cutout = glow.cutout();

            pendingMaxRadius = Math.max(pendingMaxRadius, group.radius);
            pending.add(group);

            submitComposite(context, group);
        } catch (RuntimeException ignored) {
        }
    }

    /** Collects everything {@code shape} draws into one atlas tile and glows it with the default radius. */
    public void addShape(Runnable shape) {
        addShape(DEFAULT_OPTIONS, shape);
    }

    public void addShape(float radius, Runnable shape) {
        addShape(scratchOptions.copyFrom(DEFAULT_OPTIONS).radius(radius), shape);
    }

    /**
     * Runs {@code shape} with every gui element it produces rerouted into this group's atlas tile instead of the
     * screen, then submits a single composite quad that draws the blurred result.
     * <p>
     * Nested calls merge into the group that is already collecting, so a helper that glows internally does not
     * multiply the number of tiles.
     */
    public void addShape(GlowShapeOptions options, Runnable shape) {
        if (shape == null) {
            return;
        }

        if (GlowCaptureScope.active()) {
            shape.run();
            return;
        }

        DrawContext context = activeGraphics;
        if (context == null || pending.size() >= MAX_GROUPS) {
            return;
        }

        GlowShapeOptions opts = options == null ? DEFAULT_OPTIONS : options;
        GlowGroup group = acquireGroup();
        group.kind = GlowGroup.CAPTURED;
        group.pose.identity();
        group.radius = Math.max(0.0f, opts.radius);
        group.expand = Math.max(0.0f, opts.expand);
        group.resolution = Math.clamp(opts.resolution, 0.25f, 4.0f);
        group.intensity = opts.intensity;
        group.alpha = opts.alpha;
        group.cutout = opts.cutout;

        GlowCaptureScope.begin(group.states);
        try {
            shape.run();
        } catch (RuntimeException ignored) {
        } finally {
            GlowCaptureScope.end();
        }

        if (group.states.isEmpty() || !resolveBounds(group, opts)) {
            releaseGroup(group);
            return;
        }

        pendingMaxRadius = Math.max(pendingMaxRadius, group.radius);
        pending.add(group);

        try {
            submitComposite(context, group);
        } catch (RuntimeException ignored) {
        }
    }

    private void submitComposite(DrawContext context, GlowGroup group) {
        float hintPad = BuiltGlow.padFor(submitRadiusHint(group.radius)) + group.expand;
        ((DrawContextAccessor) context).meteor$getState().addSimpleElement(new GlowRenderState(
            group.pose,
            group.capture,
            ScissorUtil.current(),
            group.contentX,
            group.contentY,
            group.contentWidth,
            group.contentHeight,
            hintPad,
            group.intensity * group.alpha
        ));
    }

    /** Union of the captured element bounds, unless the options pinned an explicit rect. */
    private static boolean resolveBounds(GlowGroup group, GlowShapeOptions options) {
        if (options.hasBounds) {
            if (!(options.boundsWidth > 0.0f) || !(options.boundsHeight > 0.0f)) {
                return false;
            }
            group.contentX = options.boundsX;
            group.contentY = options.boundsY;
            group.contentWidth = options.boundsWidth;
            group.contentHeight = options.boundsHeight;
            return true;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < group.states.size(); i++) {
            ScreenRect rect = group.states.get(i).bounds();
            if (rect == null || rect.width() <= 0 || rect.height() <= 0) {
                continue;
            }
            minX = Math.min(minX, rect.getLeft());
            minY = Math.min(minY, rect.getTop());
            maxX = Math.max(maxX, rect.getRight());
            maxY = Math.max(maxY, rect.getBottom());
        }

        if (maxX <= minX || maxY <= minY) {
            return false;
        }

        group.contentX = minX;
        group.contentY = minY;
        group.contentWidth = maxX - minX;
        group.contentHeight = maxY - minY;
        return true;
    }

    /**
     * Padding the composite render state should reserve for its bounds before the real value is known. Erring high is
     * free - it only makes the gui state treat the element as covering more area - while erring low could reorder it.
     */
    float submitRadiusHint(float ownRadius) {
        return Math.max(ownRadius, Math.max(pendingMaxRadius, lastMaxRadius));
    }

    // ------------------------------------------------------------------------------------------------------------
    // composite params, driven by the vanilla gui frame
    // ------------------------------------------------------------------------------------------------------------

    public void beginGuiFrame() {
        // A frame that got prepared but never rendered (an aborted gui pass) would otherwise leak its groups.
        if (frameReady) {
            frameReady = false;
            replay.endFrame();
            recycleGroups();
        }
        preparedCaptures.clear();
        compositeDirty = false;
    }

    int reserve(GlowCapture capture, float alpha) {
        int index = preparedCaptures.size();
        if (index >= MAX_GROUPS) {
            return -1;
        }
        preparedCaptures.add(capture);
        preparedAlpha[index] = alpha;
        compositeDirty = true;
        return index;
    }

    public boolean isGlowPipeline(RenderPipeline pipeline) {
        return pipeline == GLOW_COMPOSITE_PIPELINE;
    }

    public void prepareBuffers() {
        if (preparedCaptures.isEmpty() || !compositeDirty) {
            return;
        }
        GpuBuffer buffer = ensureCompositeBuffer();
        if (buffer == null) {
            return;
        }
        try {
            ByteBuffer staging = buildCompositeData();
            int bytes = preparedCaptures.size() * COMPOSITE_STRIDE;
            staging.position(0).limit(bytes);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(0L, bytes), staging);
            staging.position(0).limit(staging.capacity());
            compositeDirty = false;
        } catch (RuntimeException ignored) {
            compositeDirty = true;
        }
    }

    public void bindParams(RenderPass pass) {
        if (pass == null || preparedCaptures.isEmpty()) {
            return;
        }
        if (compositeDirty) {
            prepareBuffers();
        }
        if (compositeBuffer != null && !compositeBuffer.isClosed()) {
            pass.setUniform("GlowParamsArray", compositeBuffer);
        }
        if (resultSetup.texure0() != null && resultSetup.sampler0() != null) {
            pass.bindTexture("Sampler0", resultSetup.texure0(), resultSetup.sampler0());
        }
    }

    private ByteBuffer buildCompositeData() {
        ByteBuffer staging = compositeStaging;
        if (staging == null) {
            staging = ByteBuffer.allocateDirect(COMPOSITE_BYTES).order(ByteOrder.nativeOrder());
            compositeStaging = staging;
        }

        for (int i = 0; i < preparedCaptures.size(); i++) {
            GlowCapture capture = preparedCaptures.get(i);
            int base = i * COMPOSITE_STRIDE;
            zero(staging, base, COMPOSITE_STRIDE);
            // Only vec4[2].a and vec4[3] are read by glow_composite.fsh.
            staging.putFloat(base + 44, capture.prepared ? preparedAlpha[i] : 0.0f);
            staging.putFloat(base + 48, capture.regionU0);
            staging.putFloat(base + 52, capture.regionV0);
            staging.putFloat(base + 56, capture.regionUW);
            staging.putFloat(base + 60, capture.regionVH);
        }
        return staging;
    }

    // ------------------------------------------------------------------------------------------------------------
    // phase a: layout, injected at GuiRenderer.prepare HEAD
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Decides where every group lives in the atlas and runs {@code setupVertices} on the captured elements.
     * <p>
     * This has to happen before the other ui renderers upload their own uniform blocks, because replaying a captured
     * element is what makes it reserve its slot in them. No gpu work beyond buffer staging happens here.
     */
    public void preparePending() {
        lastMaxRadius = pendingMaxRadius;
        pendingMaxRadius = 0.0f;

        if (pending.isEmpty()) {
            return;
        }

        try {
            assignBuckets();
            int height = packTiles();
            atlasWidth = ATLAS_WIDTH;
            atlasHeight = Math.clamp((height + ATLAS_HEIGHT_BUCKET - 1) / ATLAS_HEIGHT_BUCKET * ATLAS_HEIGHT_BUCKET,
                ATLAS_HEIGHT_BUCKET, ATLAS_MAX_HEIGHT);

            shapeTarget = ensureTarget(shapeTarget, "meteor_glow_shape", atlasWidth, atlasHeight);
            blurTarget = ensureTarget(blurTarget, "meteor_glow_blur", atlasWidth, atlasHeight);
            resultTarget = ensureTarget(resultTarget, "meteor_glow_result", atlasWidth, atlasHeight);
            resultSetup = TextureSetup.of(resultTarget.getColorAttachmentView(), linearSampler());

            beginQuads();
            shapeParamsCount = 0;
            replay.begin();

            for (int i = 0; i < pending.size(); i++) {
                GlowGroup group = pending.get(i);
                GlowCapture capture = group.capture;
                capture.effectivePad = group.pad;
                capture.prepared = true;
                capture.setup = resultSetup;

                if (!group.placed()) {
                    capture.regionU0 = 0.0f;
                    capture.regionV0 = 0.0f;
                    capture.regionUW = 0.0f;
                    capture.regionVH = 0.0f;
                    continue;
                }

                float extentW = Math.min((group.contentWidth + group.pad * 2.0f) * group.resolution, group.tileWidth);
                float extentH = Math.min((group.contentHeight + group.pad * 2.0f) * group.resolution, group.tileHeight);
                capture.regionU0 = (float) group.tileX / atlasWidth;
                capture.regionV0 = (float) group.tileY / atlasHeight;
                capture.regionUW = Math.max(extentW / atlasWidth, 1.0E-6f);
                capture.regionVH = Math.max(extentH / atlasHeight, 1.0E-6f);

                if (group.kind == GlowGroup.PROCEDURAL) {
                    group.sourceQuad = addTileQuad(group.tileX, group.tileY, extentW, extentH);
                    group.shapeParamsOffset = writeShapeParams(group);
                } else {
                    replay.record(group);
                }

                if (!group.cutout) {
                    group.patchQuad = addTileQuad(group.tileX, group.tileY, group.tileWidth, group.tileHeight);
                }
            }

            frameReady = true;
        } catch (RuntimeException ignored) {
            frameReady = false;
            replay.endFrame();
            recycleGroups();
        }
    }

    /**
     * Quantises the requested radii down to at most {@link #MAX_BUCKETS} distinct values by growing the quantisation
     * step until few enough survive. Rounding up only ever over-blurs by less than one step, and the padding is
     * derived from the bucket radius so the tile always has room for it.
     */
    private void assignBuckets() {
        for (int i = 0; i < buckets.size(); i++) {
            buckets.get(i).groups.clear();
        }
        buckets.clear();

        float step = 0.5f;
        int distinct = collectRadii(step);
        while (distinct > MAX_BUCKETS && step < 8192.0f) {
            step *= 2.0f;
            distinct = collectRadii(step);
        }

        // Insertion sort - at most MAX_BUCKETS entries, and ascending order keeps the bands ordered by radius which
        // makes the gap between neighbouring bands as small as it can be.
        for (int i = 1; i < distinct; i++) {
            float value = scratchRadii[i];
            int j = i - 1;
            while (j >= 0 && scratchRadii[j] > value) {
                scratchRadii[j + 1] = scratchRadii[j];
                j--;
            }
            scratchRadii[j + 1] = value;
        }

        // Absurd radii could in theory survive the doubling loop. Keeping the largest values means the fallback below
        // always over-pads rather than under-pads.
        int first = Math.max(distinct - MAX_BUCKETS, 0);
        for (int i = first; i < distinct; i++) {
            Bucket bucket = acquireBucket();
            bucket.radius = scratchRadii[i];
            bucket.pad = BuiltGlow.padFor(bucket.radius);
            bucket.gap = Math.max(MIN_TILE_GAP, (int) Math.ceil(bucket.pad));
            bucket.levels = bucket.radius > 0.0f ? BLUR_LEVELS : 0;
            bucket.offset = bucket.radius / 20.0f;
            bucket.bandY0 = 0;
            bucket.bandY1 = 0;
            buckets.add(bucket);
        }

        for (int i = 0; i < pending.size(); i++) {
            GlowGroup group = pending.get(i);
            float quantised = quantise(group.radius, step);
            // Ascending radii, so the first bucket that reaches the requested radius is both a valid and the tightest
            // choice. Falling through to the last bucket can only over-blur.
            int index = buckets.size() - 1;
            for (int b = 0; b < buckets.size(); b++) {
                if (buckets.get(b).radius >= quantised) {
                    index = b;
                    break;
                }
            }
            Bucket bucket = buckets.get(index);
            group.bucket = index;
            group.pad = bucket.pad + group.expand;
            bucket.groups.add(group);
        }
    }

    private int collectRadii(float step) {
        int count = 0;
        outer:
        for (int i = 0; i < pending.size(); i++) {
            float quantised = quantise(pending.get(i).radius, step);
            for (int j = 0; j < count; j++) {
                if (scratchRadii[j] == quantised) {
                    continue outer;
                }
            }
            if (count < scratchRadii.length) {
                scratchRadii[count++] = quantised;
            }
        }
        return count;
    }

    private static float quantise(float radius, float step) {
        if (radius <= 0.0f) {
            return 0.0f;
        }
        return (float) Math.ceil(radius / step) * step;
    }

    /**
     * Shelf-packs every group, bucket by bucket, so each bucket ends up owning a contiguous band of atlas rows. The
     * gap on either side of a band is at least the blur reach of both neighbours, which is what lets the blur passes
     * treat a band as if it were the only thing in the atlas.
     *
     * @return the number of atlas rows actually used
     */
    private int packTiles() {
        int cursorY = 0;

        for (int b = 0; b < buckets.size(); b++) {
            Bucket bucket = buckets.get(b);
            int gap = bucket.gap;
            int bandY0 = alignUp(cursorY + gap, BAND_ALIGN);

            int x = gap;
            int rowY = bandY0;
            int rowHeight = 0;
            boolean placedAny = false;

            for (int i = 0; i < bucket.groups.size(); i++) {
                GlowGroup group = bucket.groups.get(i);
                int width = Math.max(Math.round((group.contentWidth + group.pad * 2.0f) * group.resolution), 1);
                int height = Math.max(Math.round((group.contentHeight + group.pad * 2.0f) * group.resolution), 1);

                if (x + width + gap > ATLAS_WIDTH) {
                    x = gap;
                    rowY += rowHeight + gap;
                    rowHeight = 0;
                }

                if (width + gap * 2 > ATLAS_WIDTH || rowY + height + gap > ATLAS_MAX_HEIGHT) {
                    group.tileWidth = 0;
                    group.tileHeight = 0;
                    continue;
                }

                group.tileX = x;
                group.tileY = rowY;
                group.tileWidth = width;
                group.tileHeight = height;
                x += width + gap;
                rowHeight = Math.max(rowHeight, height);
                placedAny = true;
            }

            if (!placedAny) {
                bucket.bandY0 = 0;
                bucket.bandY1 = 0;
                continue;
            }

            bucket.bandY0 = bandY0;
            bucket.bandY1 = Math.min(alignUp(rowY + rowHeight + gap, BAND_ALIGN), ATLAS_MAX_HEIGHT);
            cursorY = bucket.bandY1;
        }

        return cursorY;
    }

    private static int alignUp(int value, int alignment) {
        return (value + alignment - 1) / alignment * alignment;
    }

    // ------------------------------------------------------------------------------------------------------------
    // phase b: the actual gpu work, injected after every ui renderer has uploaded its uniform blocks
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Rasterises the atlas, blurs it band by band and produces the texture the composite quads sample.
     * <p>
     * Must run after the other ui renderers have uploaded their params, because a replayed element reads the same
     * uniform block it would have read on screen.
     */
    public void renderPending() {
        if (!frameReady) {
            return;
        }
        frameReady = false;

        try {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            GpuBuffer quads = uploadQuads(encoder);
            GpuBuffer shapeParams = uploadShapeParams(encoder);
            GpuBuffer kawase = uploadKawaseParams(encoder);
            replay.upload();

            if (quads == null || kawase == null) {
                return;
            }

            renderSource(encoder, quads, shapeParams);
            replay.render(shapeTarget.getColorAttachmentView(), atlasWidth, atlasHeight);
            runBlurChain(encoder, quads, kawase);
            renderCutout(encoder, quads, kawase);
        } catch (RuntimeException ignored) {
        } finally {
            replay.endFrame();
            recycleGroups();
        }
    }

    /** Clears the shape atlas and draws the sdf for every procedural group into its tile. */
    private void renderSource(CommandEncoder encoder, GpuBuffer quads, GpuBuffer shapeParams) {
        RenderSystem.ShapeIndexBuffer sequential = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
        GpuBuffer indices = sequential.getIndexBuffer(6);

        try (RenderPass pass = encoder.createRenderPass(
            () -> "meteor_glow_source",
            shapeTarget.getColorAttachmentView(),
            OptionalInt.of(0),
            null,
            OptionalDouble.empty()
        )) {
            if (shapeParams == null || shapeParamsCount == 0) {
                return;
            }

            pass.setPipeline(GLOW_SOURCE_PIPELINE);
            pass.setVertexBuffer(0, quads);
            pass.setIndexBuffer(indices, sequential.getIndexType());

            GpuBuffer splits = Stubs.ClientSplits.buffer();
            if (splits != null) {
                pass.setUniform("SplitParams", splits);
            }

            for (int i = 0; i < pending.size(); i++) {
                GlowGroup group = pending.get(i);
                if (group.kind != GlowGroup.PROCEDURAL || group.sourceQuad < 0 || group.shapeParamsOffset < 0L) {
                    continue;
                }
                pass.setUniform("GlowParamsArray", shapeParams.slice(group.shapeParamsOffset, SHAPE_BYTES));
                pass.drawIndexed(group.sourceQuad * QUAD_VERTICES, 0, 6, 1);
            }
        }
    }

    /**
     * Six passes, one per stage of the kawase dual filter, each issuing one draw per bucket clipped to that bucket's
     * band. The quad and its {@code SourceRect} are the same fullscreen mapping every stage uses; the scissor is the
     * only thing that keeps a band from being blurred with somebody else's radius.
     */
    private void runBlurChain(CommandEncoder encoder, GpuBuffer quads, GpuBuffer kawase) {
        boolean anyLevels = false;
        for (int i = 0; i < buckets.size(); i++) {
            if (buckets.get(i).levels > 0 && buckets.get(i).bandHeight() > 0) {
                anyLevels = true;
                break;
            }
        }

        ensureScratchTargets(anyLevels ? BLUR_LEVELS : 0);

        if (anyLevels) {
            blurStage(encoder, quads, kawase, 0, GLOW_BLUR_DOWN_PIPELINE, shapeTarget, downTargets[0], 0);
            blurStage(encoder, quads, kawase, 1, GLOW_BLUR_DOWN_PIPELINE, downTargets[0], downTargets[1], 1);
            blurStage(encoder, quads, kawase, 2, GLOW_BLUR_DOWN_PIPELINE, downTargets[1], downTargets[2], 2);
            blurStage(encoder, quads, kawase, 3, GLOW_BLUR_UP_PIPELINE, downTargets[2], upTargets[1], 1);
            blurStage(encoder, quads, kawase, 4, GLOW_BLUR_UP_PIPELINE, upTargets[1], upTargets[0], 0);
        }

        // Final resolve back to full size. Buckets with no blur read the shape atlas straight through - with a zero
        // half pixel the up filter degenerates into an exact copy.
        RenderSystem.ShapeIndexBuffer sequential = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
        GpuBuffer indices = sequential.getIndexBuffer(6);
        GpuSampler sampler = linearSampler();

        try (RenderPass pass = encoder.createRenderPass(
            () -> "meteor_glow_blur_resolve",
            blurTarget.getColorAttachmentView(),
            OptionalInt.of(0),
            null,
            OptionalDouble.empty()
        )) {
            pass.setPipeline(GLOW_BLUR_UP_PIPELINE);
            pass.setVertexBuffer(0, quads);
            pass.setIndexBuffer(indices, sequential.getIndexType());

            for (int i = 0; i < buckets.size(); i++) {
                Bucket bucket = buckets.get(i);
                if (bucket.bandHeight() <= 0) {
                    continue;
                }
                SimpleFramebuffer source = bucket.levels > 0 ? upTargets[0] : shapeTarget;
                if (source == null) {
                    continue;
                }
                pass.bindTexture("Sampler0", source.getColorAttachmentView(), sampler);
                pass.setUniform("KawaseParams", kawase.slice(kawaseOffset(5, i), KAWASE_BYTES));
                pass.enableScissor(0, bucket.bandY0, blurTarget.textureWidth, bucket.bandHeight());
                pass.drawIndexed(FULLSCREEN_QUAD * QUAD_VERTICES, 0, 6, 1);
            }
        }
    }

    private void blurStage(
        CommandEncoder encoder,
        GpuBuffer quads,
        GpuBuffer kawase,
        int stage,
        RenderPipeline pipeline,
        SimpleFramebuffer source,
        SimpleFramebuffer target,
        int targetLevel
    ) {
        if (source == null || target == null) {
            return;
        }

        RenderSystem.ShapeIndexBuffer sequential = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
        GpuBuffer indices = sequential.getIndexBuffer(6);

        try (RenderPass pass = encoder.createRenderPass(
            () -> "meteor_glow_blur_" + stage,
            target.getColorAttachmentView(),
            OptionalInt.of(0),
            null,
            OptionalDouble.empty()
        )) {
            pass.setPipeline(pipeline);
            pass.bindTexture("Sampler0", source.getColorAttachmentView(), linearSampler());
            pass.setVertexBuffer(0, quads);
            pass.setIndexBuffer(indices, sequential.getIndexType());

            for (int i = 0; i < buckets.size(); i++) {
                Bucket bucket = buckets.get(i);
                if (bucket.levels <= 0) {
                    continue;
                }
                int y = bucket.bandY0 >> targetLevel;
                int height = bucket.bandHeight() >> targetLevel;
                if (height <= 0) {
                    continue;
                }
                pass.setUniform("KawaseParams", kawase.slice(kawaseOffset(stage, i), KAWASE_BYTES));
                pass.enableScissor(0, y, target.textureWidth, height);
                pass.drawIndexed(FULLSCREEN_QUAD * QUAD_VERTICES, 0, 6, 1);
            }
        }
    }

    /**
     * Punches the body out of the halo. One pass: the fullscreen draw does it for the whole atlas, then every group
     * that opted out gets its tile overwritten by an uncut copy. The cutout pipeline has no blending, so the patch
     * simply wins.
     */
    private void renderCutout(CommandEncoder encoder, GpuBuffer quads, GpuBuffer kawase) {
        RenderSystem.ShapeIndexBuffer sequential = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
        GpuBuffer indices = sequential.getIndexBuffer(6);
        GpuSampler sampler = linearSampler();

        try (RenderPass pass = encoder.createRenderPass(
            () -> "meteor_glow_cutout",
            resultTarget.getColorAttachmentView(),
            OptionalInt.of(0),
            null,
            OptionalDouble.empty()
        )) {
            pass.setPipeline(GLOW_CUTOUT_PIPELINE);
            pass.bindTexture("Sampler0", blurTarget.getColorAttachmentView(), sampler);
            pass.bindTexture("Sampler1", shapeTarget.getColorAttachmentView(), sampler);
            pass.setVertexBuffer(0, quads);
            pass.setIndexBuffer(indices, sequential.getIndexType());

            pass.setUniform("KawaseParams", kawase.slice(kawaseOffset0(CUTOUT_ON_SLOT), KAWASE_BYTES));
            pass.drawIndexed(FULLSCREEN_QUAD * QUAD_VERTICES, 0, 6, 1);

            boolean patched = false;
            for (int i = 0; i < pending.size(); i++) {
                GlowGroup group = pending.get(i);
                if (group.patchQuad < 0) {
                    continue;
                }
                if (!patched) {
                    pass.setUniform("KawaseParams", kawase.slice(kawaseOffset0(CUTOUT_OFF_SLOT), KAWASE_BYTES));
                    patched = true;
                }
                pass.drawIndexed(group.patchQuad * QUAD_VERTICES, 0, 6, 1);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // shared transient buffers
    // ------------------------------------------------------------------------------------------------------------

    private void beginQuads() {
        quadCount = 0;
        addQuad(-1.0f, -1.0f, 1.0f, 1.0f);
    }

    private int addTileQuad(int tileX, int tileY, float width, float height) {
        float x0 = (float) tileX / atlasWidth * 2.0f - 1.0f;
        float y0 = (float) tileY / atlasHeight * 2.0f - 1.0f;
        float x1 = (tileX + width) / atlasWidth * 2.0f - 1.0f;
        float y1 = (tileY + height) / atlasHeight * 2.0f - 1.0f;
        return addQuad(x0, y0, x1, y1);
    }

    private int addQuad(float x0, float y0, float x1, float y1) {
        int stride = VertexFormats.POSITION.getVertexSize();
        int index = quadCount;
        int required = (index + 1) * QUAD_VERTICES * stride;

        if (quadStaging == null || quadStaging.capacity() < required) {
            int capacity = Math.max(required, Math.max(64 * QUAD_VERTICES * stride,
                quadStaging == null ? 0 : quadStaging.capacity() * 2));
            ByteBuffer grown = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
            if (quadStaging != null) {
                quadStaging.position(0).limit(index * QUAD_VERTICES * stride);
                grown.put(quadStaging);
                quadStaging.limit(quadStaging.capacity());
            }
            grown.position(0).limit(capacity);
            quadStaging = grown;
        }

        int base = index * QUAD_VERTICES * stride;
        putVertex(base, x0, y0);
        putVertex(base + stride, x0, y1);
        putVertex(base + stride * 2, x1, y1);
        putVertex(base + stride * 3, x1, y0);

        quadCount++;
        return index;
    }

    private void putVertex(int offset, float x, float y) {
        quadStaging.putFloat(offset, x);
        quadStaging.putFloat(offset + 4, y);
        quadStaging.putFloat(offset + 8, 0.0f);
    }

    private GpuBuffer uploadQuads(CommandEncoder encoder) {
        int bytes = quadCount * QUAD_VERTICES * VertexFormats.POSITION.getVertexSize();
        if (bytes <= 0 || quadStaging == null) {
            return null;
        }
        if (quadBuffer == null || quadBuffer.isClosed() || quadBuffer.size() < bytes) {
            if (quadBuffer != null) {
                quadBuffer.close();
            }
            quadBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_glow_quads",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                Math.max(bytes, 1024L)
            );
        }
        quadStaging.position(0).limit(bytes);
        encoder.writeToBuffer(quadBuffer.slice(0L, bytes), quadStaging);
        quadStaging.position(0).limit(quadStaging.capacity());
        return quadBuffer;
    }

    /** Stages one 1312 byte sdf params block for the group and returns its aligned offset. */
    private long writeShapeParams(GlowGroup group) {
        BuiltGlow glow = group.glow;
        if (glow == null) {
            return -1L;
        }

        if (shapeParamsStride == 0) {
            int alignment = Math.max(1, RenderSystem.getDevice().getUniformOffsetAlignment());
            shapeParamsStride = alignUp(SHAPE_BYTES, alignment);
        }

        int index = shapeParamsCount;
        int required = (index + 1) * shapeParamsStride;
        if (shapeParamsStaging == null || shapeParamsStaging.capacity() < required) {
            int capacity = Math.max(required, Math.max(shapeParamsStride * 8,
                shapeParamsStaging == null ? 0 : shapeParamsStaging.capacity() * 2));
            ByteBuffer grown = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
            if (shapeParamsStaging != null) {
                shapeParamsStaging.position(0).limit(index * shapeParamsStride);
                grown.put(shapeParamsStaging);
                shapeParamsStaging.limit(shapeParamsStaging.capacity());
            }
            grown.position(0).limit(capacity);
            shapeParamsStaging = grown;
        }

        int base = index * shapeParamsStride;
        ByteBuffer staging = shapeParamsStaging;
        zero(staging, base, shapeParamsStride);

        writeGlowParams(staging, base, glow, group.pad);
        staging.putFloat(base + 48, glow.splitIndex());
        writeShapeSpans(staging, base, glow, group.pad);
        writePalette(staging, base);
        writeCornerColors(staging, base, glow);

        shapeParamsCount++;
        return base;
    }

    private GpuBuffer uploadShapeParams(CommandEncoder encoder) {
        if (shapeParamsCount == 0 || shapeParamsStaging == null) {
            return null;
        }
        int bytes = shapeParamsCount * shapeParamsStride;
        if (shapeParamsBuffer == null || shapeParamsBuffer.isClosed() || shapeParamsBuffer.size() < bytes) {
            if (shapeParamsBuffer != null) {
                shapeParamsBuffer.close();
            }
            shapeParamsBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_glow_shape_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                Math.max(bytes, (long) shapeParamsStride * 8)
            );
        }
        shapeParamsStaging.position(0).limit(bytes);
        encoder.writeToBuffer(shapeParamsBuffer.slice(0L, bytes), shapeParamsStaging);
        shapeParamsStaging.position(0).limit(shapeParamsStaging.capacity());
        return shapeParamsBuffer;
    }

    private int kawaseOffset(int stage, int bucket) {
        return kawaseOffset0(stage * MAX_BUCKETS + bucket);
    }

    private int kawaseOffset0(int slot) {
        return slot * kawaseStride;
    }

    /**
     * Writes all {@link #KAWASE_SLOTS} blur params at once: six stages times {@link #MAX_BUCKETS} buckets, plus the
     * two cutout variants. {@code SourceRect} is always the full source and {@code HalfPixel} carries the bucket's
     * radius, scaled by the source mip the stage reads from.
     */
    private GpuBuffer uploadKawaseParams(CommandEncoder encoder) {
        if (kawaseStride == 0) {
            int alignment = Math.max(1, RenderSystem.getDevice().getUniformOffsetAlignment());
            kawaseStride = alignUp(KAWASE_BYTES, alignment);
        }

        int bytes = KAWASE_SLOTS * kawaseStride;
        if (kawaseStaging == null || kawaseStaging.capacity() < bytes) {
            kawaseStaging = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
        }
        zero(kawaseStaging, 0, bytes);

        int halfWidth = Math.max(atlasWidth / 2, 1);
        int halfHeight = Math.max(atlasHeight / 2, 1);
        int quarterWidth = Math.max(atlasWidth / 4, 1);
        int quarterHeight = Math.max(atlasHeight / 4, 1);

        for (int stage = 0; stage < BLUR_STAGES; stage++) {
            int sourceWidth;
            int sourceHeight;
            switch (stage) {
                case 2 -> {
                    sourceWidth = halfWidth;
                    sourceHeight = halfHeight;
                }
                case 3 -> {
                    sourceWidth = quarterWidth;
                    sourceHeight = quarterHeight;
                }
                case 4 -> {
                    sourceWidth = halfWidth;
                    sourceHeight = halfHeight;
                }
                default -> {
                    sourceWidth = Math.max(atlasWidth, 1);
                    sourceHeight = Math.max(atlasHeight, 1);
                }
            }

            for (int i = 0; i < MAX_BUCKETS; i++) {
                float offset = i < buckets.size() ? buckets.get(i).offset : 0.0f;
                int base = kawaseOffset(stage, i);
                kawaseStaging.putFloat(base, 0.0f);
                kawaseStaging.putFloat(base + 4, 0.0f);
                kawaseStaging.putFloat(base + 8, 1.0f);
                kawaseStaging.putFloat(base + 12, 1.0f);
                kawaseStaging.putFloat(base + 16, offset / sourceWidth);
                kawaseStaging.putFloat(base + 20, offset / sourceHeight);
            }
        }

        writeCutoutSlot(CUTOUT_ON_SLOT, 1.0f);
        writeCutoutSlot(CUTOUT_OFF_SLOT, 0.0f);

        if (kawaseBuffer == null || kawaseBuffer.isClosed() || kawaseBuffer.size() < bytes) {
            if (kawaseBuffer != null) {
                kawaseBuffer.close();
            }
            kawaseBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_glow_kawase_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                bytes
            );
        }

        kawaseStaging.position(0).limit(bytes);
        encoder.writeToBuffer(kawaseBuffer.slice(0L, bytes), kawaseStaging);
        kawaseStaging.position(0).limit(kawaseStaging.capacity());
        return kawaseBuffer;
    }

    private void writeCutoutSlot(int slot, float cutout) {
        int base = kawaseOffset0(slot);
        kawaseStaging.putFloat(base, 0.0f);
        kawaseStaging.putFloat(base + 4, 0.0f);
        kawaseStaging.putFloat(base + 8, 1.0f);
        kawaseStaging.putFloat(base + 12, 1.0f);
        kawaseStaging.putFloat(base + 32, cutout);
    }

    private GpuBuffer ensureCompositeBuffer() {
        if (compositeBuffer != null && !compositeBuffer.isClosed() && compositeBuffer.size() >= COMPOSITE_BYTES) {
            return compositeBuffer;
        }
        if (compositeBuffer != null) {
            compositeBuffer.close();
            compositeBuffer = null;
        }
        try {
            compositeBuffer = RenderSystem.getDevice().createBuffer(
                () -> "meteor_glow_params",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                COMPOSITE_BYTES
            );
            return compositeBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void zero(ByteBuffer buffer, int base, int length) {
        int i = 0;
        for (; i + 8 <= length; i += 8) {
            buffer.putLong(base + i, 0L);
        }
        for (; i < length; i++) {
            buffer.put(base + i, (byte) 0);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // sdf params layout
    // ------------------------------------------------------------------------------------------------------------

    private static void writeGlowParams(ByteBuffer buffer, int base, BuiltGlow glow, float pad) {
        float[] radii = glow.radii();
        buffer.putFloat(base, radii[0]);
        buffer.putFloat(base + 4, radii[1]);
        buffer.putFloat(base + 8, radii[2]);
        buffer.putFloat(base + 12, radii[3]);
        buffer.putFloat(base + 16, glow.width());
        buffer.putFloat(base + 20, glow.height());
        buffer.putFloat(base + 24, pad);
        buffer.putFloat(base + 28, 2.0f);

        int color = glow.color();
        buffer.putFloat(base + 32, (color >>> 16 & 0xFF) / 255.0f);
        buffer.putFloat(base + 36, (color >>> 8 & 0xFF) / 255.0f);
        buffer.putFloat(base + 40, (color & 0xFF) / 255.0f);
        buffer.putFloat(base + 44, glow.intensity() * glow.alpha());

        buffer.putFloat(base + 48, 0.0f);
        buffer.putFloat(base + 52, 0.0f);
        buffer.putFloat(base + 56, 1.0f);
        buffer.putFloat(base + 60, 1.0f);

        int second = glow.secondColor();
        buffer.putFloat(base + 64, (second >>> 16 & 0xFF) / 255.0f);
        buffer.putFloat(base + 68, (second >>> 8 & 0xFF) / 255.0f);
        buffer.putFloat(base + 72, (second & 0xFF) / 255.0f);
        buffer.putFloat(base + 76, glow.colorOffset());
    }

    private static void writeShapeSpans(ByteBuffer buffer, int base, BuiltGlow glow, float pad) {
        float[] spans = glow.spans();
        int count = glow.spanCount();
        if (spans == null || count <= 0) {
            return;
        }

        float[] radii = glow.radii();
        float innerRadius = Math.min(Math.min(radii[0], radii[1]), Math.min(radii[2], radii[3]));
        int header = base + 80;
        int used = Math.min(count, MAX_SPANS);

        buffer.putFloat(header, used);
        buffer.putFloat(header + 4, innerRadius);
        buffer.putFloat(header + 8, glow.leftAligned() ? 1.0f : 0.0f);
        buffer.putFloat(header + 12, glow.bottomAnchored() ? 0.0f : 1.0f);

        int spanBase = header + 16;
        float paddedHeight = glow.height() + pad * 2.0f;
        for (int i = 0; i < used; i++) {
            int source = i * 4;
            float left = spans[source] + pad;
            float right = spans[source + 1] + pad;
            float top = spans[source + 2] + pad;
            float bottom = spans[source + 3] + pad;
            int offset = spanBase + i * 16;
            buffer.putFloat(offset, left);
            buffer.putFloat(offset + 4, right);
            buffer.putFloat(offset + 8, paddedHeight - bottom);
            buffer.putFloat(offset + 12, paddedHeight - top);
        }
    }

    private static void writePalette(ByteBuffer buffer, int base) {
        int paletteBase = base + PALETTE_BASE;
        int[] colors = Stubs.ClientPalette.colors();
        int count = Stubs.ClientPalette.count();

        buffer.putFloat(paletteBase, count);
        buffer.putFloat(paletteBase + 4, Stubs.ClientPalette.phase());
        buffer.putFloat(paletteBase + 8, Stubs.ClientPalette.styleId());
        buffer.putFloat(paletteBase + 12, Stubs.GradientSweep.progress());

        int tail = paletteBase + 112;
        buffer.putFloat(tail, Stubs.ClientPalette.prevStyle());
        buffer.putFloat(tail + 4, Stubs.ClientPalette.closed());

        for (int i = 0; i < count && i < colors.length; i++) {
            int rgb = colors[i] & 0xFFFFFF;
            int offset = paletteBase + (1 + i) * 16;
            buffer.putFloat(offset, (rgb >>> 16 & 0xFF) / 255.0f);
            buffer.putFloat(offset + 4, (rgb >>> 8 & 0xFF) / 255.0f);
            buffer.putFloat(offset + 8, (rgb & 0xFF) / 255.0f);
            buffer.putFloat(offset + 12, 1.0f);
        }
    }

    private static void writeCornerColors(ByteBuffer buffer, int base, BuiltGlow glow) {
        int corners = base + CORNER_COLOR_BASE;
        putColor(buffer, corners, glow.colorTopLeft());
        putColor(buffer, corners + 16, glow.colorTopRight());
        putColor(buffer, corners + 32, glow.colorBottomRight());
        putColor(buffer, corners + 48, glow.colorBottomLeft());
    }

    private static void putColor(ByteBuffer buffer, int offset, int color) {
        buffer.putFloat(offset, (color >>> 16 & 0xFF) / 255.0f);
        buffer.putFloat(offset + 4, (color >>> 8 & 0xFF) / 255.0f);
        buffer.putFloat(offset + 8, (color & 0xFF) / 255.0f);
        buffer.putFloat(offset + 12, (color >>> 24 & 0xFF) / 255.0f);
    }

    // ------------------------------------------------------------------------------------------------------------
    // pools
    // ------------------------------------------------------------------------------------------------------------

    private GlowGroup acquireGroup() {
        while (groupPool.size() <= groupsUsed) {
            groupPool.add(new GlowGroup());
        }
        GlowGroup group = groupPool.get(groupsUsed++);
        group.reset();
        return group;
    }

    /** Only ever called on the group that was acquired last, so unwinding the cursor is enough. */
    private void releaseGroup(GlowGroup group) {
        if (groupsUsed > 0 && groupPool.get(groupsUsed - 1) == group) {
            groupsUsed--;
        }
        group.states.clear();
        group.glow = null;
    }

    private void recycleGroups() {
        for (int i = 0; i < pending.size(); i++) {
            GlowGroup group = pending.get(i);
            group.states.clear();
            group.glow = null;
        }
        pending.clear();
        groupsUsed = 0;

        for (int i = 0; i < buckets.size(); i++) {
            buckets.get(i).groups.clear();
        }
        buckets.clear();
    }

    private Bucket acquireBucket() {
        for (int i = 0; i < bucketPool.size(); i++) {
            Bucket bucket = bucketPool.get(i);
            if (!buckets.contains(bucket)) {
                return bucket;
            }
        }
        Bucket bucket = new Bucket();
        bucketPool.add(bucket);
        return bucket;
    }

    // ------------------------------------------------------------------------------------------------------------
    // framebuffers
    // ------------------------------------------------------------------------------------------------------------

    private static GpuSampler linearSampler() {
        return RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
    }

    private void ensureScratchTargets(int levels) {
        int width = Math.max(atlasWidth, 1);
        int height = Math.max(atlasHeight, 1);
        for (int i = 0; i < levels; i++) {
            downTargets[i] = ensureTarget(downTargets[i], "meteor_glow_down_" + i, width, height);
            if (i < levels - 1) {
                upTargets[i] = ensureTarget(upTargets[i], "meteor_glow_up_" + i, width, height);
            }
            width = Math.max(width / 2, 1);
            height = Math.max(height / 2, 1);
        }
    }

    private static SimpleFramebuffer ensureTarget(SimpleFramebuffer target, String name, int width, int height) {
        if (target == null) {
            return new SimpleFramebuffer(name, width, height, false);
        }
        if (target.textureWidth != width || target.textureHeight != height) {
            target.resize(width, height);
        }
        return target;
    }

    private static SimpleFramebuffer deleteTarget(SimpleFramebuffer target) {
        if (target != null) {
            target.delete();
        }
        return null;
    }

    @Override
    public void close() {
        recycleGroups();
        preparedCaptures.clear();
        activeGraphics = null;
        frameReady = false;
        resultSetup = TextureSetup.empty();

        replay.close();

        compositeBuffer = closeBuffer(compositeBuffer);
        quadBuffer = closeBuffer(quadBuffer);
        shapeParamsBuffer = closeBuffer(shapeParamsBuffer);
        kawaseBuffer = closeBuffer(kawaseBuffer);
        compositeStaging = null;
        quadStaging = null;
        shapeParamsStaging = null;
        kawaseStaging = null;

        shapeTarget = deleteTarget(shapeTarget);
        blurTarget = deleteTarget(blurTarget);
        resultTarget = deleteTarget(resultTarget);
        for (int i = 0; i < BLUR_LEVELS; i++) {
            downTargets[i] = deleteTarget(downTargets[i]);
            upTargets[i] = deleteTarget(upTargets[i]);
        }
    }

    private static GpuBuffer closeBuffer(GpuBuffer buffer) {
        if (buffer != null) {
            buffer.close();
        }
        return null;
    }

    /** One blur radius, one contiguous band of atlas rows. */
    private static final class Bucket {

        final List<GlowGroup> groups = new ArrayList<>(16);

        float radius;
        float pad;
        float offset;
        int gap;
        int levels;
        int bandY0;
        int bandY1;

        int bandHeight() {
            return bandY1 - bandY0;
        }

    }

    // temporary
    public static final class Stubs {

        public static final class ClientPalette {

            private static final int[] COLORS = new int[] { 0xFFFFFFFF, 0xFFFF0000, 0xFFFF0000, 0xFFFFFFFF, 0xFFFF0000, 0xFFFFFFFF };

            public static int count() {
                return 1;
            }

            public static int[] colors() {
                return COLORS;
            }

            public static float phase() {
                return 0.0f;
            }

            public static float styleId() {
                return 0.0f;
            }

            public static float prevStyle() {
                return 0.0f;
            }

            public static float closed() {
                return 0.0f;
            }

        }

        public static final class ClientSplits {

            public static GpuBuffer buffer() {
                return null;
            }

        }

        public static final class GradientSweep {

            public static float progress() {
                return -1.0f;
            }

        }

    }

}
