package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import meteordevelopment.meteorclient.IMinecraft;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.reflect.PreInit;
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Render3D implements IMinecraft {
    
    private static final double MAX_SAFE_COORD = 30_000_000.0;
    private static final int TARGET_ESP_CIRCLE_SEGMENTS = 40;
    private static final Map<VoxelShape, Pair<List<Box>, List<Line>>> SHAPE_OUTLINES = new HashMap<>();
    private static final Map<VoxelShape, List<Box>> SHAPE_BOXES = new HashMap<>();
    
    public static final List<Line> LINE_DEPTH = new ArrayList<>();
    public static final List<Line> LINE = new ArrayList<>();
    public static final List<Line> LINE_OVERLAY = new ArrayList<>();
    public static final List<Quad> QUAD_DEPTH = new ArrayList<>();
    public static final List<Quad> QUAD = new ArrayList<>();
    public static final List<GradientQuad> GRADIENT_QUAD = new ArrayList<>();
    public static final List<GradientQuad> GRADIENT_QUAD_DEPTH = new ArrayList<>();
    
    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();
    
    public static MatrixStack.Entry lastWorldSpaceEntry = new MatrixStack().peek();
    public static float lastTickDelta = 1.0f;
    public static Vec3d lastCameraPos = Vec3d.ZERO;
    public static Quaternionf lastCameraRotation = new Quaternionf();
    private static final Vector3f LINE_NORMAL = new Vector3f();
    
    private static final BlendFunction STANDARD_BLEND = new BlendFunction(
        SourceFactor.SRC_ALPHA,
        DestFactor.ONE_MINUS_SRC_ALPHA,
        SourceFactor.ONE,
        DestFactor.ZERO
    );
    private static final RenderPipeline.Snippet METEOR_LINES_SNIPPET = RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
        .withBlend(STANDARD_BLEND)
        .withDepthWrite(false)
        .withCull(false)
        .buildSnippet();
    
    private static final RenderLayer METEOR_LINES_OVERLAY = RenderLayerFactory.of(
        "rendertype/meteor_lines_overlay",
        256,
        RenderPipeline.builder(METEOR_LINES_SNIPPET)
            .withLocation("pipelines/meteor_lines_overlay")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );
    
    private static final RenderLayer METEOR_LINES_NO_DEPTH = RenderLayerFactory.of(
        "rendertype/meteor_lines_no_depth",
        256,
        RenderPipeline.builder(METEOR_LINES_SNIPPET)
            .withLocation("pipelines/meteor_lines_no_depth")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );
    
    private static final RenderLayer METEOR_FILLED_BOX_NO_DEPTH = RenderLayerFactory.of(
        "rendertype/meteor_filled_box_no_depth",
        256,
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation("pipelines/meteor_filled_box_no_depth")
            .withBlend(STANDARD_BLEND)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build()
    );
    
    private static float espValue = 1f;
    private static float espSpeed = 1f;
    private static float prevEspValue;
    private static float circleStep;
    private static boolean flipSpeed;
    
    private static double smoothY = 0;
    private static double smoothY2 = 0;
    
    private Render3D() {}
    
    @PreInit
    public static void init() {
        MeteorClient.LOGGER.info("Render3D initialized");
    }
    
    public static void capture(Matrix4fc projection, Matrix4fc view, Vec3d cameraPos) {
        if (projection != null) {
            lastProjMat.set(projection);
        }
        if (view != null) {
            lastModMat.set(view);
            lastWorldSpaceMatrix.set(view);
        }
        if (cameraPos != null && isFinite(cameraPos)) {
            lastCameraPos = cameraPos;
        }
    }
    
    public static void setLastWorldSpaceEntry(MatrixStack.Entry entry) {
        if (entry != null) {
            lastWorldSpaceEntry = entry;
        }
    }
    
    public static void setLastTickDelta(float tickDelta) {
        lastTickDelta = Float.isFinite(tickDelta) ? tickDelta : 1.0f;
    }
    
    public static void setLastCameraPos(Vec3d cameraPos) {
        if (cameraPos != null && isFinite(cameraPos)) {
            lastCameraPos = cameraPos;
        }
    }
    
    public static void setLastCameraRotation(Quaternionf rotation) {
        if (rotation != null) {
            lastCameraRotation = rotation;
        }
    }
    
    public static void updateTargetEsp(float deltaTime) {
        prevEspValue = espValue;
        espValue += espSpeed * deltaTime;
        if (espSpeed > 25) {
            flipSpeed = true;
        }
        if (espSpeed < -25) {
            flipSpeed = false;
        }
        espSpeed = flipSpeed ? espSpeed - 0.5f * deltaTime : espSpeed + 0.5f * deltaTime;
        circleStep += 0.06f * deltaTime;
    }
    
    public static void updateTargetEsp() {
        updateTargetEsp(1.0f);
    }
    
    public static float getEspValue() {
        return espValue;
    }
    
    public static float getPrevEspValue() {
        return prevEspValue;
    }
    
    public static float getCircleStep() {
        return circleStep;
    }
    
    private static double easeInOutSine(double t) {
        return -(Math.cos(Math.PI * t) - 1) / 2;
    }
    
    private static double smoothSinAnimation(double input) {
        double sin = (Math.sin(input) + 1) / 2;
        return easeInOutSine(sin);
    }
    
    public static void render(Render3DEvent event) {
        if (!Utils.canUpdate()) {
            clearQueues();
            return;
        }
        
        MatrixStack matrices = event.matrices;
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        
        Vec3d cameraPos = lastCameraPos;
        if (!isFinite(cameraPos)) {
            clearQueues();
            return;
        }
        
        renderGradientQuads(matrices, immediate, cameraPos);
        renderQuads(matrices, immediate, cameraPos);
        renderLines(matrices, immediate, cameraPos);
        
        immediate.draw();
    }
    
    private static void clearQueues() {
        LINE_DEPTH.clear();
        LINE.clear();
        LINE_OVERLAY.clear();
        QUAD_DEPTH.clear();
        QUAD.clear();
        GRADIENT_QUAD.clear();
        GRADIENT_QUAD_DEPTH.clear();
    }
    
    private static void renderLines(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos) {
        if (LINE.isEmpty() && LINE_DEPTH.isEmpty() && LINE_OVERLAY.isEmpty()) {
            return;
        }
        
        try {
            renderLineBatch(matrices, immediate, cameraPos, LINE_DEPTH, RenderLayers.lines());
            renderLineBatch(matrices, immediate, cameraPos, LINE, METEOR_LINES_NO_DEPTH);
            renderLineBatch(matrices, immediate, cameraPos, LINE_OVERLAY, METEOR_LINES_OVERLAY);
        } finally {
            LINE.clear();
            LINE_DEPTH.clear();
            LINE_OVERLAY.clear();
        }
    }
    
    private static void renderLineBatch(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos, List<Line> lines, RenderLayer layer) {
        if (lines.isEmpty()) {
            return;
        }
        
        VertexConsumer buffer = immediate.getBuffer(layer);
        for (Line line : lines) {
            drawLineVertex(matrices, buffer, line, cameraPos);
        }
        immediate.draw(layer);
    }
    
    private static void drawLineVertex(MatrixStack matrices, VertexConsumer buffer, Line line, Vec3d cameraPos) {
        if (line == null || !isFinite(cameraPos) || !isFinite(line.start) || !isFinite(line.end)) {
            return;
        }
        MatrixStack.Entry entry = line.entry != null ? line.entry : matrices.peek();
        Vector3f normal = lineNormal(line.start, line.end);
        float width = sanitizeLineWidth(line.width);
        
        float x1 = (float) (line.start.x - cameraPos.x);
        float y1 = (float) (line.start.y - cameraPos.y);
        float z1 = (float) (line.start.z - cameraPos.z);
        
        float x2 = (float) (line.end.x - cameraPos.x);
        float y2 = (float) (line.end.y - cameraPos.y);
        float z2 = (float) (line.end.z - cameraPos.z);
        
        buffer.vertex(entry, x1, y1, z1)
            .color(line.colorStart)
            .normal(entry, normal)
            .lineWidth(width);
        buffer.vertex(entry, x2, y2, z2)
            .color(line.colorEnd)
            .normal(entry, normal)
            .lineWidth(width);
    }
    
    private static void renderQuads(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos) {
        if (QUAD.isEmpty() && QUAD_DEPTH.isEmpty()) {
            return;
        }
        
        if (!QUAD_DEPTH.isEmpty()) {
            RenderLayer layer = RenderLayers.debugFilledBox();
            VertexConsumer buffer = immediate.getBuffer(layer);
            for (Quad quad : QUAD_DEPTH) {
                drawQuadVertex(matrices, buffer, quad, cameraPos);
            }
            immediate.draw(layer);
        }
        
        if (!QUAD.isEmpty()) {
            VertexConsumer buffer = immediate.getBuffer(METEOR_FILLED_BOX_NO_DEPTH);
            for (Quad quad : QUAD) {
                drawQuadVertex(matrices, buffer, quad, cameraPos);
            }
            immediate.draw(METEOR_FILLED_BOX_NO_DEPTH);
        }
        
        QUAD.clear();
        QUAD_DEPTH.clear();
    }
    
    private static void drawQuadVertex(MatrixStack matrices, VertexConsumer buffer, Quad quad, Vec3d cameraPos) {
        if (quad == null || !isFinite(cameraPos) || !isFinite(quad.x) || !isFinite(quad.y) || !isFinite(quad.w) || !isFinite(quad.z)) {
            return;
        }
        MatrixStack.Entry entry = quad.entry != null ? quad.entry : matrices.peek();
        
        float x1 = (float) (quad.x.x - cameraPos.x);
        float y1 = (float) (quad.x.y - cameraPos.y);
        float z1 = (float) (quad.x.z - cameraPos.z);
        
        float x2 = (float) (quad.y.x - cameraPos.x);
        float y2 = (float) (quad.y.y - cameraPos.y);
        float z2 = (float) (quad.y.z - cameraPos.z);
        
        float x3 = (float) (quad.w.x - cameraPos.x);
        float y3 = (float) (quad.w.y - cameraPos.y);
        float z3 = (float) (quad.w.z - cameraPos.z);
        
        float x4 = (float) (quad.z.x - cameraPos.x);
        float y4 = (float) (quad.z.y - cameraPos.y);
        float z4 = (float) (quad.z.z - cameraPos.z);
        
        buffer.vertex(entry, x1, y1, z1).color(quad.color);
        buffer.vertex(entry, x2, y2, z2).color(quad.color);
        buffer.vertex(entry, x3, y3, z3).color(quad.color);
        buffer.vertex(entry, x4, y4, z4).color(quad.color);
    }
    
    private static void renderGradientQuads(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos) {
        if (GRADIENT_QUAD.isEmpty() && GRADIENT_QUAD_DEPTH.isEmpty()) {
            return;
        }
        
        if (!GRADIENT_QUAD_DEPTH.isEmpty()) {
            RenderLayer layer = RenderLayers.debugFilledBox();
            VertexConsumer buffer = immediate.getBuffer(layer);
            for (GradientQuad quad : GRADIENT_QUAD_DEPTH) {
                drawGradientQuadVertex(matrices, buffer, quad, cameraPos);
            }
            immediate.draw(layer);
        }
        
        if (!GRADIENT_QUAD.isEmpty()) {
            VertexConsumer buffer = immediate.getBuffer(METEOR_FILLED_BOX_NO_DEPTH);
            for (GradientQuad quad : GRADIENT_QUAD) {
                drawGradientQuadVertex(matrices, buffer, quad, cameraPos);
            }
            immediate.draw(METEOR_FILLED_BOX_NO_DEPTH);
        }
        
        GRADIENT_QUAD.clear();
        GRADIENT_QUAD_DEPTH.clear();
    }
    
    private static void drawGradientQuadVertex(MatrixStack matrices, VertexConsumer buffer, GradientQuad quad, Vec3d cameraPos) {
        if (quad == null || !isFinite(cameraPos) || !isFinite(quad.p1) || !isFinite(quad.p2) || !isFinite(quad.p3) || !isFinite(quad.p4)) {
            return;
        }
        MatrixStack.Entry entry = matrices.peek();
        
        float x1 = (float) (quad.p1.x - cameraPos.x);
        float y1 = (float) (quad.p1.y - cameraPos.y);
        float z1 = (float) (quad.p1.z - cameraPos.z);
        
        float x2 = (float) (quad.p2.x - cameraPos.x);
        float y2 = (float) (quad.p2.y - cameraPos.y);
        float z2 = (float) (quad.p2.z - cameraPos.z);
        
        float x3 = (float) (quad.p3.x - cameraPos.x);
        float y3 = (float) (quad.p3.y - cameraPos.y);
        float z3 = (float) (quad.p3.z - cameraPos.z);
        
        float x4 = (float) (quad.p4.x - cameraPos.x);
        float y4 = (float) (quad.p4.y - cameraPos.y);
        float z4 = (float) (quad.p4.z - cameraPos.z);
        
        buffer.vertex(entry, x1, y1, z1).color(quad.c1);
        buffer.vertex(entry, x2, y2, z2).color(quad.c2);
        buffer.vertex(entry, x3, y3, z3).color(quad.c3);
        buffer.vertex(entry, x4, y4, z4).color(quad.c4);
    }
    
    public static void drawCircle(MatrixStack matrix, LivingEntity lastTarget, float anim, float red, int baseColor1, int baseColor2) {
        double cs = Utils.interpolate(circleStep - 0.17, circleStep);
        Vec3d target = Utils.interpolate(lastTarget);
        boolean canSee = mc.player != null && mc.player.canSee(lastTarget);
        
        float hitEffect = Math.min(red * 2f, 1f);
        float distanceMultiplier = 1.0f + (float) Math.sin(hitEffect * Math.PI) * 0.18f;
        int size = TARGET_ESP_CIRCLE_SEGMENTS;
        
        float entityWidth = lastTarget.getWidth() * distanceMultiplier;
        float entityHeight = lastTarget.getHeight();
        
        double targetY = smoothSinAnimation(cs) * entityHeight;
        double targetY2 = smoothSinAnimation(cs - 0.35) * entityHeight;
        
        smoothY = lerp(smoothY, targetY, 0.12);
        smoothY2 = lerp(smoothY2, targetY2, 0.10);
        
        int color1 = ColorUtil.multRed(baseColor1, 1 + red * 125);
        int color2 = ColorUtil.multRed(baseColor2, 1 + red * 125);
        
        float step = (float) (Math.PI * 2.0 / size);
        float sinStep = MathHelper.sin(step);
        float cosStep = MathHelper.cos(step);
        float currentSin = 0.0f;
        float currentCos = 1.0f;
        
        for (int i = 0; i < size; i++) {
            float nextSin = currentSin * cosStep + currentCos * sinStep;
            float nextCos = currentCos * cosStep - currentSin * sinStep;
            
            float gradientT = 0.5f - 0.5f * currentCos;
            float gradientTNext = 0.5f - 0.5f * nextCos;
            
            int currentColor = ColorUtil.lerpColor(color1, color2, gradientT);
            int nextColor = ColorUtil.lerpColor(color1, color2, gradientTNext);
            
            int brightColor = ColorUtil.multAlpha(currentColor, 0.8f * anim);
            int brightColorNext = ColorUtil.multAlpha(nextColor, 0.8f * anim);
            int fadeColor = ColorUtil.multAlpha(currentColor, 0f);
            int fadeColorNext = ColorUtil.multAlpha(nextColor, 0f);
            
            Vec3d circlePoint = target.add(currentCos * entityWidth, smoothY, -currentSin * entityWidth);
            Vec3d trailPoint = target.add(currentCos * entityWidth, smoothY2, -currentSin * entityWidth);
            Vec3d nextCirclePoint = target.add(nextCos * entityWidth, smoothY, -nextSin * entityWidth);
            Vec3d nextTrailPoint = target.add(nextCos * entityWidth, smoothY2, -nextSin * entityWidth);
            
            drawGradientQuad(
                circlePoint,
                nextCirclePoint,
                nextTrailPoint,
                trailPoint,
                brightColor,
                brightColorNext,
                fadeColorNext,
                fadeColor,
                canSee
            );
            
            drawGradientQuad(
                trailPoint,
                nextTrailPoint,
                nextCirclePoint,
                circlePoint,
                fadeColor,
                fadeColorNext,
                brightColorNext,
                brightColor,
                canSee
            );
            
            int trailColorTop = ColorUtil.multAlpha(currentColor, 0.15f * anim);
            int trailColorBottom = ColorUtil.multAlpha(currentColor, 0f);
            drawLineGradient(circlePoint, trailPoint, trailColorTop, trailColorBottom, 6f, canSee);
            
            int circleColor = ColorUtil.multAlpha(currentColor, 1f * anim);
            int circleColorNext = ColorUtil.multAlpha(nextColor, 1f * anim);
            drawLineGradient(circlePoint, nextCirclePoint, circleColor, circleColorNext, 2f, canSee);
            
            currentSin = nextSin;
            currentCos = nextCos;
        }
    }
    
    public static void drawRadiusCircle(Vec3d center, float radius, int color) {
        if (mc.player == null) {
            return;
        }
        
        double baseY = center.y;
        int fillColor = ColorUtil.multAlpha(color, 0.25f);
        
        int radiusInt = (int) Math.ceil(radius) + 1;
        
        for (int dx = -radiusInt; dx <= radiusInt; dx++) {
            for (int dz = -radiusInt; dz <= radiusInt; dz++) {
                boolean hasCornerInside = false;
                boolean hasCornerOutside = false;
                
                for (double ox = -0.5; ox <= 0.5; ox += 1.0) {
                    for (double oz = -0.5; oz <= 0.5; oz += 1.0) {
                        double cornerDist = Math.sqrt((dx + ox) * (dx + ox) + (dz + oz) * (dz + oz));
                        if (cornerDist <= radius) {
                            hasCornerInside = true;
                        } else {
                            hasCornerOutside = true;
                        }
                    }
                }
                
                if (hasCornerInside && hasCornerOutside) {
                    double x = center.x + dx;
                    double z = center.z + dz;
                    
                    Box box = new Box(
                        x - 0.5, baseY, z - 0.5,
                        x + 0.5, baseY + 1, z + 0.5
                    );
                    
                    drawBoxWithCross(box, color, fillColor, 2f);
                }
            }
        }
    }
    
    public static void drawBoxWithCross(Box box, int lineColor, int fillColor, float lineWidth) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;
        
        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, false);
        drawQuad(new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, false);
        
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);
        
        int crossColor = ColorUtil.multAlpha(lineColor, 0.6f);
        float crossWidth = lineWidth * 0.8f;
        
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z2), crossColor, crossWidth, false);
        
        drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
        
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);
        
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
        
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);
        
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
    }
    
    public static void drawBoxWithCrossFull(Box box, int lineColor, int fillColor, float lineWidth) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;
        
        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, false);
        drawQuad(new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, false);
        
        drawQuad(new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z1), new Vec3d(x1, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, false);
        
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);
        
        int crossColor = ColorUtil.multAlpha(lineColor, 0.6f);
        float crossWidth = lineWidth * 0.8f;
        
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z2), crossColor, crossWidth, false);
        
        drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
        
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);
        
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
        
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);
        
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
    }
    
    public static void drawPlastShape(BlockPos playerPos, Vec3d smooth, int lineColor, int fillColor) {
        if (mc.player == null) {
            return;
        }
        
        float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
        
        if (Math.abs(mc.player.getPitch()) > 60) {
            BlockPos blockPos = playerPos.up().offset(mc.player.getFacing(), 3);
            Vec3d pos1 = Vec3d.of(blockPos.east(3).south(3).down()).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.west(2).north(2).up()).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3);
        } else if (yaw <= -157.5F || yaw >= 157.5F) {
            BlockPos blockPos = playerPos.north(3).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3);
        } else if (yaw <= -112.5F) {
            drawSidePlast(playerPos.east(5).south().down(), smooth, lineColor, fillColor, -1, true);
        } else if (yaw <= -67.5F) {
            BlockPos blockPos = playerPos.east(2).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3);
        } else if (yaw <= -22.5F) {
            drawSidePlast(playerPos.east(5).down(), smooth, lineColor, fillColor, 1, false);
        } else if (yaw >= -22.5 && yaw <= 22.5) {
            BlockPos blockPos = playerPos.south(2).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3);
        } else if (yaw <= 67.5F) {
            drawSidePlast(playerPos.west(4).down(), smooth, lineColor, fillColor, 1, true);
        } else if (yaw <= 112.5F) {
            BlockPos blockPos = playerPos.west(3).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3);
        } else if (yaw <= 157.5F) {
            drawSidePlast(playerPos.west(4).south().down(), smooth, lineColor, fillColor, -1, false);
        }
    }
    
    private static void drawSidePlast(BlockPos blockPos, Vec3d smooth, int lineColor, int fillColor, int i, boolean ff) {
        Vec3d vec3d = Vec3d.of(blockPos).add(smooth);
        int crossColor = ColorUtil.multAlpha(lineColor, 0.6f);
        
        List<Vec3d> horizontalPoints = new ArrayList<>();
        
        float x = ff ? i : -i;
        Vec3d current = vec3d;
        
        horizontalPoints.add(current);
        current = current.add(x, 0, 0);
        horizontalPoints.add(current);
        
        for (int f = 0; f < 4; f++) {
            current = current.add(0, 0, i);
            horizontalPoints.add(current);
            current = current.add(x, 0, 0);
            horizontalPoints.add(current);
        }
        
        current = current.add(0, 0, i);
        horizontalPoints.add(current);
        current = current.add(x * -2, 0, 0);
        horizontalPoints.add(current);
        
        for (int f = 0; f < 3; f++) {
            current = current.add(0, 0, i * -1);
            horizontalPoints.add(current);
            current = current.add(x * -1, 0, 0);
            horizontalPoints.add(current);
        }
        
        current = current.add(0, 0, i * -2);
        horizontalPoints.add(current);
        
        for (int p = 0; p < horizontalPoints.size() - 1; p++) {
            Vec3d p1 = horizontalPoints.get(p);
            Vec3d p2 = horizontalPoints.get(p + 1);
            drawLine(p1, p2, lineColor, 2f, false);
            drawLine(p1.add(0, 5, 0), p2.add(0, 5, 0), lineColor, 2f, false);
        }
        
        for (Vec3d point : horizontalPoints) {
            drawLine(point, point.add(0, 5, 0), lineColor, 2f, false);
        }
        
        for (int p = 0; p < horizontalPoints.size() - 1; p++) {
            Vec3d p1 = horizontalPoints.get(p);
            Vec3d p2 = horizontalPoints.get(p + 1);
            Vec3d p1Top = p1.add(0, 5, 0);
            Vec3d p2Top = p2.add(0, 5, 0);
            
            drawQuad(p1, p2, p2Top, p1Top, fillColor, false);
            drawQuad(p1Top, p2Top, p2, p1, fillColor, false);
            
            drawLine(p1, p2Top, crossColor, 1.6f, false);
            drawLine(p2, p1Top, crossColor, 1.6f, false);
        }
        
        current = vec3d;
        drawQuad(current, current.add(x, 0, 0), current.add(x, 0, i * 2), current.add(0, 0, i * 2), fillColor, false);
        drawQuad(current.add(0, 0, i * 2), current.add(x, 0, i * 2), current.add(x, 0, 0), current, fillColor, false);
        drawLine(current, current.add(x, 0, i * 2), crossColor, 1.6f, false);
        drawLine(current.add(x, 0, 0), current.add(0, 0, i * 2), crossColor, 1.6f, false);
        
        for (int f = 0; f < 3; f++) {
            current = current.add(x, 0, i);
            drawQuad(current, current.add(x, 0, 0), current.add(x, 0, i * 2), current.add(0, 0, i * 2), fillColor, false);
            drawQuad(current.add(0, 0, i * 2), current.add(x, 0, i * 2), current.add(x, 0, 0), current, fillColor, false);
            drawLine(current, current.add(x, 0, i * 2), crossColor, 1.6f, false);
            drawLine(current.add(x, 0, 0), current.add(0, 0, i * 2), crossColor, 1.6f, false);
        }
        current = current.add(x, 0, i);
        drawQuad(current, current.add(x, 0, 0), current.add(x, 0, i), current.add(0, 0, i), fillColor, false);
        drawQuad(current.add(0, 0, i), current.add(x, 0, i), current.add(x, 0, 0), current, fillColor, false);
        drawLine(current, current.add(x, 0, i), crossColor, 1.6f, false);
        drawLine(current.add(x, 0, 0), current.add(0, 0, i), crossColor, 1.6f, false);
        
        current = vec3d.add(0, 5, 0);
        drawQuad(current, current.add(0, 0, i * 2), current.add(x, 0, i * 2), current.add(x, 0, 0), fillColor, false);
        drawQuad(current.add(x, 0, 0), current.add(x, 0, i * 2), current.add(0, 0, i * 2), current, fillColor, false);
        drawLine(current, current.add(x, 0, i * 2), crossColor, 1.6f, false);
        drawLine(current.add(x, 0, 0), current.add(0, 0, i * 2), crossColor, 1.6f, false);
        
        for (int f = 0; f < 3; f++) {
            current = current.add(x, 0, i);
            drawQuad(current, current.add(0, 0, i * 2), current.add(x, 0, i * 2), current.add(x, 0, 0), fillColor, false);
            drawQuad(current.add(x, 0, 0), current.add(x, 0, i * 2), current.add(0, 0, i * 2), current, fillColor, false);
            drawLine(current, current.add(x, 0, i * 2), crossColor, 1.6f, false);
            drawLine(current.add(x, 0, 0), current.add(0, 0, i * 2), crossColor, 1.6f, false);
        }
        current = current.add(x, 0, i);
        drawQuad(current, current.add(0, 0, i), current.add(x, 0, i), current.add(x, 0, 0), fillColor, false);
        drawQuad(current.add(x, 0, 0), current.add(x, 0, i), current.add(0, 0, i), current, fillColor, false);
        drawLine(current, current.add(x, 0, i), crossColor, 1.6f, false);
        drawLine(current.add(x, 0, 0), current.add(0, 0, i), crossColor, 1.6f, false);
    }
    
    private static double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }
    
    public static void drawGradientQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4, boolean depth) {
        if (!isFinite(p1) || !isFinite(p2) || !isFinite(p3) || !isFinite(p4)) {
            return;
        }
        GradientQuad quad = new GradientQuad(p1, p2, p3, p4, c1, c2, c3, c4);
        if (depth) {
            GRADIENT_QUAD_DEPTH.add(quad);
        } else {
            GRADIENT_QUAD.add(quad);
        }
    }
    
    public static void drawLineGradient(Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        if (!isFinite(start) || !isFinite(end)) {
            return;
        }
        Line line = new Line(null, start, end, colorStart, colorEnd, sanitizeLineWidth(width));
        if (depth) {
            LINE_DEPTH.add(line);
        } else {
            LINE.add(line);
        }
    }
    
    public static Vector3f getNormal(Vector3f start, Vector3f end) {
        Vector3f normal = new Vector3f(start).sub(end);
        float sqrt = MathHelper.sqrt(normal.lengthSquared());
        if (sqrt < 0.0001f) {
            return new Vector3f(0, 1, 0);
        }
        return normal.div(sqrt);
    }
    
    private static Vector3f lineNormal(Vec3d start, Vec3d end) {
        float x = (float) (start.x - end.x);
        float y = (float) (start.y - end.y);
        float z = (float) (start.z - end.z);
        float length = MathHelper.sqrt(x * x + y * y + z * z);
        if (length < 0.0001F) {
            return LINE_NORMAL.set(0.0F, 1.0F, 0.0F);
        }
        return LINE_NORMAL.set(x / length, y / length, z / length);
    }
    
    public static void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
        drawShape(blockPos, voxelShape, color, width, true, false);
    }
    
    public static void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        List<Box> boxes = SHAPE_BOXES.computeIfAbsent(voxelShape, VoxelShape::getBoundingBoxes);
        boxes.forEach(box -> {
            Box offsetBox = box.offset(blockPos);
            drawBox(offsetBox, color, width, true, fill, depth);
        });
    }
    
    public static void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        Vec3d vec3d = Vec3d.of(blockPos);
        
        Pair<List<Box>, List<Line>> pair = SHAPE_OUTLINES.computeIfAbsent(voxelShape, shape -> {
            List<Line> lines = new ArrayList<>();
            shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) ->
                lines.add(new Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0)));
            return new Pair<>(shape.getBoundingBoxes(), lines);
        });
        
        if (fill) {
            pair.getLeft().forEach(box -> drawBox(box.offset(vec3d), color, width, false, true, depth));
        }
        pair.getRight().forEach(line -> drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth));
    }
    
    public static void drawShapeOverlay(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
        Vec3d vec3d = Vec3d.of(blockPos);
        Pair<List<Box>, List<Line>> pair = SHAPE_OUTLINES.computeIfAbsent(voxelShape, shape -> {
            List<Line> lines = new ArrayList<>();
            shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) ->
                lines.add(new Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0)));
            return new Pair<>(shape.getBoundingBoxes(), lines);
        });
        
        pair.getRight().forEach(line -> drawLineOverlay(line.start.add(vec3d), line.end.add(vec3d), color, width));
    }
    
    public static void drawBox(Box box, int color, float width) {
        drawBox(box, color, width, true, true, false);
    }
    
    public static void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        drawBox(null, box, color, width, line, fill, depth);
    }
    
    public static void drawBox(MatrixStack.Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        if (!isFinite(box)) {
            return;
        }
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;
        
        if (fill) {
            int fillColor = ColorUtil.multAlpha(color, 0.3f);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
        }
        
        if (line) {
            drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
            drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
            drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
        }
    }
    
    public static void drawBoxOverlay(Box box, int color, float width) {
        if (!isFinite(box)) {
            return;
        }
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;
        
        drawLineOverlay(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), color, width);
        drawLineOverlay(new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), color, width);
        drawLineOverlay(new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), color, width);
        drawLineOverlay(new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), color, width);
        
        drawLineOverlay(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), color, width);
        drawLineOverlay(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), color, width);
        drawLineOverlay(new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), color, width);
        drawLineOverlay(new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), color, width);
        
        drawLineOverlay(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), color, width);
        drawLineOverlay(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), color, width);
        drawLineOverlay(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), color, width);
        drawLineOverlay(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), color, width);
    }
    
    public static void drawLine(MatrixStack.Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
        drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
    }
    
    public static void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
        drawLine(null, start, end, color, color, width, depth);
    }
    
    public static void drawLine(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        if (!isFinite(start) || !isFinite(end)) {
            return;
        }
        Line line = new Line(entry, start, end, colorStart, colorEnd, sanitizeLineWidth(width));
        if (depth) {
            LINE_DEPTH.add(line);
        } else {
            LINE.add(line);
        }
    }
    
    public static void drawLineOverlay(Vec3d start, Vec3d end, int color, float width) {
        drawLineOverlay(null, start, end, color, color, width);
    }
    
    public static void drawLineOverlay(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
        if (!isFinite(start) || !isFinite(end)) {
            return;
        }
        LINE_OVERLAY.add(new Line(entry, start, end, colorStart, colorEnd, sanitizeLineWidth(width)));
    }
    
    public static void drawQuad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        drawQuad(null, x, y, w, z, color, depth);
    }
    
    public static void drawQuad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        if (!isFinite(x) || !isFinite(y) || !isFinite(w) || !isFinite(z)) {
            return;
        }
        Quad quad = new Quad(entry, x, y, w, z, color);
        if (depth) {
            QUAD_DEPTH.add(quad);
        } else {
            QUAD.add(quad);
        }
    }
    
    private static float sanitizeLineWidth(float width) {
        if (!Float.isFinite(width)) {
            return 1.0f;
        }
        return Math.clamp(width, 0.1f, 16.0f);
    }
    
    private static boolean isFinite(Vec3d vec) {
        return vec != null && isFinite(vec.x) && isFinite(vec.y) && isFinite(vec.z);
    }
    
    private static boolean isFinite(Box box) {
        return box != null
            && isFinite(box.minX) && isFinite(box.minY) && isFinite(box.minZ)
            && isFinite(box.maxX) && isFinite(box.maxY) && isFinite(box.maxZ);
    }
    
    private static boolean isFinite(double value) {
        return Double.isFinite(value) && Math.abs(value) <= MAX_SAFE_COORD;
    }
    
    public static void resetCircleSmoothing() {
        smoothY = 0;
        smoothY2 = 0;
    }
    
    public record Line(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
    
    }
    
    public record Quad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {
    
    }
    
    public record GradientQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4) {
    
    }
    
}


