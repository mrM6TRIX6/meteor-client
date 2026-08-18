package meteordevelopment.meteorclient.utils.render.ui;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.reflect.PreInit;
import meteordevelopment.meteorclient.utils.render.ScissorUtil;
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import meteordevelopment.meteorclient.utils.render.ui.arc.ArcOutlineRenderer;
import meteordevelopment.meteorclient.utils.render.ui.arc.ArcRenderer;
import meteordevelopment.meteorclient.utils.render.ui.arc.BuiltArc;
import meteordevelopment.meteorclient.utils.render.ui.arc.BuiltArcOutline;
import meteordevelopment.meteorclient.utils.render.ui.blur.BlurBuilder;
import meteordevelopment.meteorclient.utils.render.ui.blur.BlurFramebuffer;
import meteordevelopment.meteorclient.utils.render.ui.blur.BuiltBlur;
import meteordevelopment.meteorclient.utils.render.ui.effecticon.BuiltEffectIcon;
import meteordevelopment.meteorclient.utils.render.ui.effecticon.EffectIconRenderer;
import meteordevelopment.meteorclient.utils.render.ui.glass.BuiltGlass;
import meteordevelopment.meteorclient.utils.render.ui.glass.GlassRenderer;
import meteordevelopment.meteorclient.utils.render.ui.image.BuiltImage;
import meteordevelopment.meteorclient.utils.render.ui.image.ImageRenderer;
import meteordevelopment.meteorclient.utils.render.ui.msdf.BuiltMsdf;
import meteordevelopment.meteorclient.utils.render.ui.msdf.MsdfRenderer;
import meteordevelopment.meteorclient.utils.render.ui.outline.outline360.BuiltOutline360;
import meteordevelopment.meteorclient.utils.render.ui.outline.outline360.Outline360Range;
import meteordevelopment.meteorclient.utils.render.ui.outline.outline360.Outline360Renderer;
import meteordevelopment.meteorclient.utils.render.ui.outline.outlinedefault.BuiltOutline;
import meteordevelopment.meteorclient.utils.render.ui.outline.outlinedefault.DefaultOutlineRenderer;
import meteordevelopment.meteorclient.utils.render.ui.outline.outlineglass.BuiltGlassOutline;
import meteordevelopment.meteorclient.utils.render.ui.outline.outlineglass.GlassOutlineRenderer;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectdefault.BuiltRectangle;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectdefault.DefaultRectangleRenderer;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectgradient.BuiltGradientRectangle;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectgradient.GradientRectangleRenderer;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.recthalftone.BuiltHalftoneRectangle;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.recthalftone.HalftoneRectangleRenderer;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectrotatinggradient.BuiltRotatingGradientRectangle;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectrotatinggradient.RotatingGradientRectangleRenderer;
import meteordevelopment.meteorclient.utils.render.ui.ripple.BuiltRipple;
import meteordevelopment.meteorclient.utils.render.ui.ripple.RippleRenderer;
import meteordevelopment.meteorclient.utils.render.ui.zippy.BuiltZippy;
import meteordevelopment.meteorclient.utils.render.ui.zippy.ZippyRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class Render2D {
    
    private static final float SCALE = 2.0f;
    private static final Deque<Space> SPACE_STACK = new ArrayDeque<>();
    private static final PointProjector IDENTITY_PROJECTOR = ProjectedPoint::new;
    private static final Deque<Matrix4f> PROJECTION_OVERRIDES = new ArrayDeque<>();
    private static final Deque<PointProjector> POINT_PROJECTORS = new ArrayDeque<>();
    
    private Render2D() {}
    
    @PreInit
    public static void init() {
        MeteorClient.LOGGER.info("Render2D initialized");
    }
    
    public static Space currentSpace() {
        return SPACE_STACK.isEmpty() ? Space.INDEPENDENT : SPACE_STACK.peekLast();
    }
    
    public static void pushSpace(Space space) {
        SPACE_STACK.addLast(space == null ? Space.INDEPENDENT : space);
    }
    
    public static void popSpace() {
        if (!SPACE_STACK.isEmpty()) {
            SPACE_STACK.removeLast();
        }
    }
    
    public static void withSpace(Space space, Runnable block) {
        pushSpace(space);
        try {
            block.run();
        } finally {
            popSpace();
        }
    }
    
    public static void withIndependent(Runnable block) {
        withSpace(Space.INDEPENDENT, block);
    }
    
    public static void withVanilla(Runnable block) {
        withSpace(Space.VANILLA, block);
    }
    
    public static Matrix3x2f pose(DrawContext context) {
        Matrix3x2f pose = new Matrix3x2f(context.getMatrices());
        
        if (currentSpace() == Space.INDEPENDENT) {
            float s = scaleFactor();
            if (s != 1.0f) {
                pose.scale(s);
            }
        }
        
        return pose;
    }
    
    public static float scaleFactor() {
        if (mc == null || mc.getWindow() == null) {
            return 1;
        }
        return 1f / Math.max(1, mc.getWindow().getScaleFactor());
        //return Math.min(width() / 1920f, height() / 1080f) / SCALE;
    }
    
    public static int width() {
        return mc.getWindow().getFramebufferWidth();
    }
    
    public static int height() {
        return mc.getWindow().getFramebufferHeight();
    }
    
    public static int independentWidth() {
        return width();
    }
    
    public static int independentHeight() {
        return height();
    }
    
    public static int vanillaWidth() {
        return Math.max(1, (int) Math.ceil((double) width() / scaleFactor()));
    }
    
    public static int vanillaHeight() {
        return Math.max(1, (int) Math.ceil((double) height() / scaleFactor()));
    }
    
    public static float toIndependentX(float vanillaX) {
        return vanillaX * scaleFactor();
    }
    
    public static float toIndependentY(float vanillaY) {
        return vanillaY * scaleFactor();
    }
    
    public static float toVanillaX(float independentX) {
        return independentX / scaleFactor();
    }
    
    public static float toVanillaY(float independentY) {
        return independentY / scaleFactor();
    }
    
    public static float mouseX(float screenMouseX) {
        return currentSpace() == Space.INDEPENDENT ? toIndependentX(screenMouseX) : screenMouseX;
    }
    
    public static float mouseY(float screenMouseY) {
        return currentSpace() == Space.INDEPENDENT ? toIndependentY(screenMouseY) : screenMouseY;
    }
    
    public static PointProjector getCurrentProjector() {
        return POINT_PROJECTORS.peekLast();
    }
    
    public static boolean hasProjectionOverride() {
        return !PROJECTION_OVERRIDES.isEmpty();
    }
    
    public static ProjectedRect projectRect(float x, float y, float width, float height) {
        PointProjector projector = getCurrentProjector();
        if (projector == null) {
            return new ProjectedRect(x, y, width, height);
        }
        
        ProjectedPoint[] points = {
            projector.project(x, y),
            projector.project(x + width, y),
            projector.project(x + width, y + height),
            projector.project(x, y + height)
        };
        
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        boolean hasPoint = false;
        
        for (ProjectedPoint point : points) {
            if (point == null) {
                continue;
            }
            hasPoint = true;
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
        }
        
        if (!hasPoint) {
            return null;
        }
        return new ProjectedRect(minX, minY, Math.max(0.0f, maxX - minX), Math.max(0.0f, maxY - minY));
    }
    
    public static void beginFrame(DrawContext context) {
        beginFrame(context, Space.INDEPENDENT);
    }
    
    public static void beginFrame(DrawContext context, Space space) {
        pushSpace(space);
        
        blur().beginFrame(context);
        glass().beginFrame(context);
        glassOutline().beginFrame(context);
        rectangle().beginFrame(context);
        gradientRectangle().beginFrame(context);
        rotatingGradientRectangle().beginFrame(context);
        halftoneRectangle().beginFrame(context);
        zippy().beginFrame(context);
        arc().beginFrame(context);
        arcOutline().beginFrame(context);
        outline().beginFrame(context);
        outline360().beginFrame(context);
        msdf().beginFrame(context);
        image().beginFrame(context);
        effectIcon().beginFrame(context);
        ripple().beginFrame(context);
    }
    
    public static void flush() {
        blur().flush();
        glass().flush();
        glassOutline().flush();
        rectangle().flush();
        gradientRectangle().flush();
        rotatingGradientRectangle().flush();
        halftoneRectangle().flush();
        zippy().flush();
        arc().flush();
        arcOutline().flush();
        outline().flush();
        outline360().flush();
        msdf().flush();
        image().flush();
        effectIcon().flush();
        ripple().flush();
    }
    
    public static void endFrame() {
        popSpace();
    }
    
    public static void blur(float x, float y, float width, float height, float radius) {
        blur(x, y, width, height, radius, 16.0f, 1.0f, ColorUtil.WHITE);
    }
    
    public static void blur(float x, float y, float width, float height, float radius, float blurRadius) {
        blur(x, y, width, height, radius, blurRadius, 1.0f, ColorUtil.WHITE);
    }
    
    public static void blur(float x, float y, float width, float height, float radius, float blurRadius, float smoothness, int color) {
        imageBarrier();
        blur().enqueue(new BuiltBlur(x, y, width, height, radius, smoothness, blurRadius).withColor(color));
    }
    
    public static void blur(
        float x,
        float y,
        float width,
        float height,
        float radius,
        float blurRadius,
        float smoothness,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft
    ) {
        imageBarrier();
        blur().enqueue(new BuiltBlur(x, y, width, height, radius, smoothness, blurRadius)
            .withColors(colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft));
    }
    
    public static void blur(DrawContext context, float x, float y, float width, float height, float radius, float blurRadius) {
        beginFrame(context);
        try {
            blur(x, y, width, height, radius, blurRadius);
            flush();
        } finally {
            endFrame();
        }
    }
    
    public static void blur(BuiltBlur blur) {
        imageBarrier();
        Render2D.blur().enqueue(blur);
    }
    
    public static BlurBuilder blurBuilder() {
        return new BlurBuilder();
    }
    
    public static void glass(
        float x,
        float y,
        float width,
        float height,
        float[] radius,
        int color,
        float globalAlpha,
        float fresnelPower,
        int fresnelColor,
        float baseAlpha,
        boolean fresnelInvert,
        float fresnelMix,
        float distortStrength,
        float squirt,
        float z
    ) {
        imageBarrier();
        glass().enqueue(new BuiltGlass(
            x,
            y,
            width,
            height,
            radius,
            color,
            globalAlpha,
            fresnelPower,
            fresnelColor,
            baseAlpha,
            fresnelInvert,
            fresnelMix,
            distortStrength,
            squirt,
            z
        ));
    }
    
    public static void glass(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        int color,
        float globalAlpha,
        float fresnelPower,
        int fresnelColor,
        float baseAlpha,
        boolean fresnelInvert,
        float fresnelMix,
        float distortStrength,
        float squirt,
        float z
    ) {
        imageBarrier();
        glass().enqueue(new BuiltGlass(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color,
            globalAlpha,
            fresnelPower,
            fresnelColor,
            baseAlpha,
            fresnelInvert,
            fresnelMix,
            distortStrength,
            squirt,
            z
        ));
    }
    
    public static void glass(
        float x,
        float y,
        float width,
        float height,
        float radius,
        int color,
        float globalAlpha,
        float fresnelPower,
        int fresnelColor,
        float baseAlpha,
        boolean fresnelInvert,
        float fresnelMix,
        float distortStrength,
        float squirt,
        float z
    ) {
        glass(x, y, width, height, radius, radius, radius, radius, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, z);
    }
    
    public static void glass(BuiltGlass glass) {
        imageBarrier();
        Render2D.glass().enqueue(glass);
    }
    
    public static void liquidGlass(float x, float y, float width, float height, float squirt, float power, float radius, int color) {
        glass(
            x,
            y,
            width,
            height,
            radius * squirt / 2.0f,
            radius * squirt / 2.0f,
            radius * squirt / 2.0f,
            radius * squirt / 2.0f,
            color,
            ((color >>> 24) & 0xFF) / 255.0f,
            height == 240.0f ? 100.0f : 50.0f,
            color | 0xFF000000,
            1.0f,
            true,
            0.0f,
            power,
            squirt,
            0.0f
        );
    }
    
    public static void glassOutline(
        float x,
        float y,
        float width,
        float height,
        float[] radius,
        float thickness,
        int color,
        float globalAlpha,
        float fresnelPower,
        int fresnelColor,
        float baseAlpha,
        boolean fresnelInvert,
        float fresnelMix,
        float distortStrength,
        float squirt,
        float z
    ) {
        imageBarrier();
        glassOutline().enqueue(new BuiltGlassOutline(
            x,
            y,
            width,
            height,
            radius,
            thickness,
            color,
            globalAlpha,
            fresnelPower,
            fresnelColor,
            baseAlpha,
            fresnelInvert,
            fresnelMix,
            distortStrength,
            squirt,
            z
        ));
    }
    
    public static void glassOutline(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        float thickness,
        int color,
        float globalAlpha,
        float fresnelPower,
        int fresnelColor,
        float baseAlpha,
        boolean fresnelInvert,
        float fresnelMix,
        float distortStrength,
        float squirt,
        float z
    ) {
        imageBarrier();
        glassOutline().enqueue(new BuiltGlassOutline(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            color,
            globalAlpha,
            fresnelPower,
            fresnelColor,
            baseAlpha,
            fresnelInvert,
            fresnelMix,
            distortStrength,
            squirt,
            BuiltGlassOutline.DEFAULT_SMOOTHNESS,
            z
        ));
    }
    
    public static void glassOutline(
        float x,
        float y,
        float width,
        float height,
        float radius,
        float thickness,
        int color,
        float globalAlpha,
        float fresnelPower,
        int fresnelColor,
        float baseAlpha,
        boolean fresnelInvert,
        float fresnelMix,
        float distortStrength,
        float squirt,
        float z
    ) {
        glassOutline(x, y, width, height, radius, radius, radius, radius, thickness, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, z);
    }
    
    public static void glassOutline(BuiltGlassOutline outline) {
        imageBarrier();
        Render2D.glassOutline().enqueue(outline);
    }
    
    public static void liquidGlassOutline(float x, float y, float width, float height, float squirt, float power, float radius, float thickness, int color) {
        glassOutline(
            x,
            y,
            width,
            height,
            radius * squirt / 2.0f,
            radius * squirt / 2.0f,
            radius * squirt / 2.0f,
            radius * squirt / 2.0f,
            thickness,
            color,
            ((color >>> 24) & 0xFF) / 255.0f,
            height == 240.0f ? 100.0f : 50.0f,
            color | 0xFF000000,
            1.0f,
            true,
            0.0f,
            power,
            squirt,
            0.0f
        );
    }
    
    public static void rect(float x, float y, float width, float height, int color) {
        rect(x, y, width, height, 0.0f, color);
    }
    
    public static void rect(float x, float y, float width, float height, float radius, int color) {
        imageBarrier();
        rectangle().enqueue(new BuiltRectangle(x, y, width, height, radius, color));
    }
    
    public static void rect(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        int color
    ) {
        imageBarrier();
        rectangle().enqueue(new BuiltRectangle(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color
        ));
    }
    
    public static void rect(
        float x,
        float y,
        float width,
        float height,
        float radius,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft
    ) {
        rect(x, y, width, height, radius, radius, radius, radius, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
    }
    
    public static void rect(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft
    ) {
        imageBarrier();
        rectangle().enqueue(new BuiltRectangle(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            colorTopLeft,
            colorTopRight,
            colorBottomRight,
            colorBottomLeft,
            BuiltRectangle.DEFAULT_SMOOTHNESS
        ));
    }
    
    public static void rect(DrawContext context, float x, float y, float width, float height, float radius, int color) {
        beginFrame(context, Space.INDEPENDENT);
        try {
            rect(x, y, width, height, radius, color);
            flush();
        } finally {
            endFrame();
        }
    }
    
    public static void rect(BuiltRectangle rectangle) {
        rectangle().enqueue(rectangle);
    }
    
    public static void gradientRect(float x, float y, float width, float height, float radius, int firstColor, int secondColor, float speed, float frequency, float angle) {
        imageBarrier();
        gradientRectangle().enqueue(new BuiltGradientRectangle(x, y, width, height, radius, firstColor, secondColor, speed, frequency, angle));
    }
    
    public static void gradientRect(DrawContext context, BuiltGradientRectangle built) {
        beginFrame(context, Space.INDEPENDENT);
        try {
            gradientRect(built);
            flush();
        } finally {
            endFrame();
        }
    }
    
    public static void gradientRect(BuiltGradientRectangle built) {
        imageBarrier();
        gradientRectangle().enqueue(built);
    }
    
    public static void rotatingGradientRect(float x, float y, float width, float height, float radius, int firstColor, int secondColor, float speed) {
        imageBarrier();
        rotatingGradientRectangle().enqueue(new BuiltRotatingGradientRectangle(x, y, width, height, radius, firstColor, firstColor, secondColor, secondColor, speed));
    }
    
    public static void rotatingGradientRect(float x, float y, float width, float height, float radius, int firstColor, int secondColor, int thirdColor, int fourthColor, float speed) {
        imageBarrier();
        rotatingGradientRectangle().enqueue(new BuiltRotatingGradientRectangle(x, y, width, height, radius, firstColor, secondColor, thirdColor, fourthColor, speed));
    }
    
    public static void rotatingGradientRect(DrawContext context, BuiltRotatingGradientRectangle built) {
        beginFrame(context, Space.INDEPENDENT);
        try {
            rotatingGradientRect(built);
            flush();
        } finally {
            endFrame();
        }
    }
    
    public static void rotatingGradientRect(BuiltRotatingGradientRectangle built) {
        imageBarrier();
        rotatingGradientRectangle().enqueue(built);
    }
    
    public static void halftoneRect(
        float x,
        float y,
        float width,
        float height,
        float radius,
        int color,
        int dotColor,
        float dotSize,
        float dotSpacing
    ) {
        imageBarrier();
        halftoneRectangle().enqueue(new BuiltHalftoneRectangle(x, y, width, height, radius, color, dotColor, dotSize, dotSpacing));
    }
    
    public static void halftoneRect(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        int color,
        int dotColor,
        float dotSize,
        float dotSpacing
    ) {
        imageBarrier();
        halftoneRectangle().enqueue(new BuiltHalftoneRectangle(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color,
            dotColor,
            dotSize,
            dotSpacing
        ));
    }
    
    public static void halftoneRect(
        float x,
        float y,
        float width,
        float height,
        float radius,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft,
        int dotColor,
        float dotSize,
        float dotSpacing
    ) {
        halftoneRect(
            x,
            y,
            width,
            height,
            radius,
            radius,
            radius,
            radius,
            colorTopLeft,
            colorTopRight,
            colorBottomRight,
            colorBottomLeft,
            dotColor,
            dotSize,
            dotSpacing
        );
    }
    
    public static void halftoneRect(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft,
        int dotColor,
        float dotSize,
        float dotSpacing
    ) {
        imageBarrier();
        halftoneRectangle().enqueue(new BuiltHalftoneRectangle(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            colorTopLeft,
            colorTopRight,
            colorBottomRight,
            colorBottomLeft,
            BuiltHalftoneRectangle.DEFAULT_SMOOTHNESS,
            dotColor,
            dotSize,
            dotSpacing
        ));
    }
    
    public static void halftoneRect(BuiltHalftoneRectangle rectangle) {
        imageBarrier();
        Render2D.halftoneRectangle().enqueue(rectangle);
    }
    
    public static void zippy(float x, float y, float width, float height, float radius, int color) {
        imageBarrier();
        zippy().enqueue(new BuiltZippy(x, y, width, height, radius, color));
    }
    
    public static void zippy(float x, float y, float width, float height, float radius, int color, float smoothness) {
        imageBarrier();
        zippy().enqueue(new BuiltZippy(x, y, width, height, radius, color).withSmoothness(smoothness));
    }
    
    public static void zippy(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        int color
    ) {
        imageBarrier();
        zippy().enqueue(new BuiltZippy(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color
        ));
    }
    
    public static void zippy(BuiltZippy zippy) {
        imageBarrier();
        Render2D.zippy().enqueue(zippy);
    }
    
    public static void arc(float x, float y, float size, float thickness, float degree, float rotation, int color) {
        imageBarrier();
        arc().enqueue(new BuiltArc(x, y, size, thickness, degree, rotation, color));
    }
    
    public static void arc(float x, float y, float size, float thickness, float degree, float rotation, int... colors) {
        imageBarrier();
        arc().enqueue(new BuiltArc(x, y, size, thickness, degree, rotation, colors));
    }
    
    public static void arc(BuiltArc arc) {
        imageBarrier();
        Render2D.arc().enqueue(arc);
    }
    
    public static void arcOutline(float x, float y, float size, float arcThickness, float degree,
        float rotation, float outlineThickness, int fillColor, int outlineColor) {
        imageBarrier();
        arcOutline().enqueue(new BuiltArcOutline(
            x,
            y,
            size,
            arcThickness,
            degree,
            rotation,
            outlineThickness,
            fillColor,
            outlineColor
        ));
    }
    
    public static void arcOutline(BuiltArcOutline outline) {
        imageBarrier();
        Render2D.arcOutline().enqueue(outline);
    }
    
    public static void outline(float x, float y, float width, float height, float radius, float thickness, int color) {
        imageBarrier();
        outline().enqueue(new BuiltOutline(x, y, width, height, radius, thickness, color));
    }
    
    public static void outline(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        float thickness,
        int color
    ) {
        imageBarrier();
        outline().enqueue(new BuiltOutline(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            color
        ));
    }
    
    public static void outline(
        float x,
        float y,
        float width,
        float height,
        float radius,
        float thickness,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft
    ) {
        outline(x, y, width, height, radius, radius, radius, radius, thickness, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
    }
    
    public static void outline(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        float thickness,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft
    ) {
        imageBarrier();
        outline().enqueue(new BuiltOutline(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            colorTopLeft,
            colorTopRight,
            colorBottomRight,
            colorBottomLeft,
            BuiltOutline.DEFAULT_SMOOTHNESS
        ));
    }
    
    public static void outline(DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color) {
        beginFrame(context);
        try {
            outline(x, y, width, height, radius, thickness, color);
            flush();
        } finally {
            endFrame();
        }
    }
    
    public static void outline(BuiltOutline outline) {
        imageBarrier();
        Render2D.outline().enqueue(outline);
    }
    
    public static Outline360Range outline360Range(float startDegrees, float endDegrees, int color) {
        return Outline360Range.of(startDegrees, endDegrees, color);
    }
    
    public static void outline360(
        float x,
        float y,
        float width,
        float height,
        float radius,
        float thickness,
        int defaultColor,
        Outline360Range... ranges
    ) {
        imageBarrier();
        outline360(x, y, width, height, radius, thickness, defaultColor, Arrays.asList(ranges));
    }
    
    public static void outline360(
        float x,
        float y,
        float width,
        float height,
        float radius,
        float thickness,
        int defaultColor,
        List<Outline360Range> ranges
    ) {
        imageBarrier();
        outline360().enqueue(new BuiltOutline360(x, y, width, height, radius, thickness, defaultColor, ranges));
    }
    
    public static void outline360(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        float thickness,
        int defaultColor,
        Outline360Range... ranges
    ) {
        imageBarrier();
        outline360(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, thickness, defaultColor, Arrays.asList(ranges));
    }
    
    public static void outline360(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        float thickness,
        int defaultColor,
        List<Outline360Range> ranges
    ) {
        imageBarrier();
        outline360().enqueue(new BuiltOutline360(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            defaultColor,
            ranges
        ));
    }
    
    public static void outline360(BuiltOutline360 outline) {
        imageBarrier();
        Render2D.outline360().enqueue(outline);
    }
    
    public static void ripple(
        float x,
        float y,
        float width,
        float height,
        float radius,
        float centerX,
        float centerY,
        float progress,
        int sourceColor,
        int targetColor
    ) {
        ripple(x, y, width, height, radius, 0.5f, centerX, centerY, progress, 25.0f, sourceColor, targetColor);
    }
    
    public static void ripple(
        float x,
        float y,
        float width,
        float height,
        float radius,
        float smoothness,
        float centerX,
        float centerY,
        float progress,
        float rippleSmoothness,
        int sourceColor,
        int targetColor
    ) {
        ripple().enqueue(new BuiltRipple(
            x, y, width, height,
            radius, radius, radius, radius,
            smoothness,
            centerX, centerY,
            progress, rippleSmoothness,
            sourceColor, targetColor,
            false
        ));
    }
    
    public static void rippleIcon(
        float x,
        float y,
        float width,
        float height,
        float radius,
        float centerX,
        float centerY,
        float progress,
        int sourceColor,
        int targetColor
    ) {
        ripple().enqueue(new BuiltRipple(
            x, y, width, height,
            radius, radius, radius, radius,
            0.5f,
            centerX, centerY,
            progress, 25.0f,
            sourceColor, targetColor,
            true
        ));
    }
    
    public static void image(String texture, float x, float y, float size, float radius) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, size, radius));
    }
    
    public static void image(String texture, float x, float y, float size, float radius, int color) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, size, radius, color));
    }
    
    public static void image(String texture, float x, float y, float size, float radius, int... colors) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, size, radius).withColors(colors));
    }
    
    public static void image(String texture, float x, float y, float width, float height, float radius, int color) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, width, height, radius, color));
    }
    
    public static void image(String texture, float x, float y, float width, float height, float radius, int... colors) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, width, height, radius).withColors(colors));
    }
    
    public static void imageUv(String texture, float x, float y, float width, float height, float radius, float u0, float v0, float u1, float v1, int color) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, width, height, radius, color).withUv(u0, v0, u1, v1));
    }
    
    public static void imageUv(String texture, float x, float y, float width, float height, float radius, float u0, float v0, float u1, float v1, int... colors) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, width, height, radius).withUv(u0, v0, u1, v1).withColors(colors));
    }
    
    public static void imageUvNearest(String texture, float x, float y, float width, float height, float radius, float smoothness, float u0, float v0, float u1, float v1, int color) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, width, height, radius, color)
            .withSmoothness(smoothness)
            .withNearestFilter()
            .withUv(u0, v0, u1, v1));
    }
    
    public static void imageUvNearest(String texture, float x, float y, float width, float height, float radius, float smoothness, float u0, float v0, float u1, float v1, int... colors) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, width, height, radius)
            .withSmoothness(smoothness)
            .withNearestFilter()
            .withUv(u0, v0, u1, v1)
            .withColors(colors));
    }
    
    public static void image(String texture, float x, float y, float size, float radius, float rotationDegrees, float originX, float originY, int color) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, size, radius, color).withRotation(rotationDegrees, originX, originY));
    }
    
    public static void image(String texture, float x, float y, float size, float radius, float rotationDegrees, float originX, float originY, int... colors) {
        Render2D.image().enqueue(new BuiltImage(texture, x, y, size, radius).withColors(colors).withRotation(rotationDegrees, originX, originY));
    }
    
    public static void effectIcon(RegistryEntry<StatusEffect> effect, float x, float y, float size) {
        effectIcon().enqueue(new BuiltEffectIcon(effect, x, y, size));
    }
    
    public static void effectIcon(RegistryEntry<StatusEffect> effect, float x, float y, float size, int color) {
        effectIcon().enqueue(new BuiltEffectIcon(effect, x, y, size, color));
    }
    
    public static void effectIcon(StatusEffectInstance effect, float x, float y, float size) {
        if (effect != null && effect.shouldShowIcon()) {
            effectIcon().enqueue(new BuiltEffectIcon(effect, x, y, size));
        }
    }
    
    public static void effectIcon(StatusEffectInstance effect, float x, float y, float size, int color) {
        if (effect != null && effect.shouldShowIcon()) {
            effectIcon().enqueue(new BuiltEffectIcon(effect, x, y, size, color));
        }
    }
    
    public static void msdf(BuiltMsdf built) {
        imageBarrier();
        msdf().enqueue(built);
    }
    
    public static void msdf(DrawContext context, BuiltMsdf msdf) {
        beginFrame(context);
        try {
            msdf(msdf);
            flush();
        } finally {
            endFrame();
        }
    }
    
    public static void pushScissor(DrawContext context, float x, float y, float width, float height) {
        if (context == null) {
            return;
        }
        
        float scissorX = x;
        float scissorY = y;
        float scissorWidth = width;
        float scissorHeight = height;
        
        if (hasProjectionOverride()) {
            ProjectedRect projected = projectRect(x, y, width, height);
            if (projected == null) {
                projected = new ProjectedRect(0, 0, 0, 0);
            }
            scissorX = projected.x();
            scissorY = projected.y();
            scissorWidth = projected.width();
            scissorHeight = projected.height();
        } else if (currentSpace() == Space.VANILLA) {
            float s = scaleFactor();
            scissorX *= s;
            scissorY *= s;
            scissorWidth *= s;
            scissorHeight *= s;
        }
        
        int left = Math.round(scissorX);
        int top = Math.round(scissorY);
        int right = Math.round(scissorX + scissorWidth);
        int bottom = Math.round(scissorY + scissorHeight);
        
        ScissorUtil.push(left, top, right, bottom);
        context.enableScissor(left, top, right, bottom);
    }
    
    public static void popScissor(DrawContext context) {
        if (context != null) {
            context.disableScissor();
            ScissorUtil.pop();
        }
    }
    
    public static void close() {
        BlurFramebuffer.closeInstance();
        GlassRenderer.closeInstance();
        GlassOutlineRenderer.closeInstance();
        DefaultRectangleRenderer.closeInstance();
        GradientRectangleRenderer.closeInstance();
        RotatingGradientRectangleRenderer.closeInstance();
        HalftoneRectangleRenderer.closeInstance();
        ZippyRenderer.closeInstance();
        ArcRenderer.closeInstance();
        ArcOutlineRenderer.closeInstance();
        DefaultOutlineRenderer.closeInstance();
        Outline360Renderer.closeInstance();
        MsdfRenderer.closeInstance();
        ImageRenderer.closeInstance();
        EffectIconRenderer.closeInstance();
        RippleRenderer.closeInstance();
    }
    
    private static BlurFramebuffer blur() {
        return BlurFramebuffer.getInstance();
    }
    
    private static GlassRenderer glass() {
        return GlassRenderer.getInstance();
    }
    
    private static GlassOutlineRenderer glassOutline() {
        return GlassOutlineRenderer.getInstance();
    }
    
    private static DefaultRectangleRenderer rectangle() {
        return DefaultRectangleRenderer.getInstance();
    }
    
    private static GradientRectangleRenderer gradientRectangle() {
        return GradientRectangleRenderer.getInstance();
    }
    
    private static RotatingGradientRectangleRenderer rotatingGradientRectangle() {
        return RotatingGradientRectangleRenderer.getInstance();
    }
    
    private static HalftoneRectangleRenderer halftoneRectangle() {
        return HalftoneRectangleRenderer.getInstance();
    }
    
    private static ZippyRenderer zippy() {
        return ZippyRenderer.getInstance();
    }
    
    private static ArcRenderer arc() {
        return ArcRenderer.getInstance();
    }
    
    private static ArcOutlineRenderer arcOutline() {
        return ArcOutlineRenderer.getInstance();
    }
    
    private static DefaultOutlineRenderer outline() {
        return DefaultOutlineRenderer.getInstance();
    }
    
    private static Outline360Renderer outline360() {
        return Outline360Renderer.getInstance();
    }
    
    private static MsdfRenderer msdf() {
        return MsdfRenderer.getInstance();
    }
    
    private static ImageRenderer image() {
        return ImageRenderer.getInstance();
    }
    
    private static EffectIconRenderer effectIcon() {
        return EffectIconRenderer.getInstance();
    }
    
    private static RippleRenderer ripple() {
        return RippleRenderer.getInstance();
    }
    
    private static void imageBarrier() {
        ImageRenderer.getInstance().barrier();
        EffectIconRenderer.getInstance().barrier();
    }
    
    private static float boldOffset(float size) {
        return Math.clamp(size * 0.035f, 0.16f, 0.42f);
    }
    
    @FunctionalInterface
    public interface PointProjector {
        
        ProjectedPoint project(float x, float y);
        
    }
    
    public record ProjectedPoint(float x, float y) {
    
    }
    
    public record ProjectedRect(float x, float y, float width, float height) {
    
    }
    
    public enum Space {
        
        INDEPENDENT,
        VANILLA
        
    }
    
}