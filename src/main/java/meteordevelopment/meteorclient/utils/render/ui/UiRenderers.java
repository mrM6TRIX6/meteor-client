package meteordevelopment.meteorclient.utils.render.ui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import meteordevelopment.meteorclient.utils.render.item.RenderItem;
import meteordevelopment.meteorclient.utils.render.ui.arc.ArcOutlineRenderer;
import meteordevelopment.meteorclient.utils.render.ui.arc.ArcRenderer;
import meteordevelopment.meteorclient.utils.render.ui.blur.BlurFramebuffer;
import meteordevelopment.meteorclient.utils.render.ui.glass.GlassRenderer;
import meteordevelopment.meteorclient.utils.render.ui.glow.GlowRenderer;
import meteordevelopment.meteorclient.utils.render.ui.image.ImageRenderer;
import meteordevelopment.meteorclient.utils.render.ui.msdf.MsdfRenderer;
import meteordevelopment.meteorclient.utils.render.ui.outline.outline360.Outline360Renderer;
import meteordevelopment.meteorclient.utils.render.ui.outline.outlinedefault.DefaultOutlineRenderer;
import meteordevelopment.meteorclient.utils.render.ui.outline.outlineglass.GlassOutlineRenderer;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectdefault.DefaultRectangleRenderer;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectgradient.GradientRectangleRenderer;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.recthalftone.HalftoneRectangleRenderer;
import meteordevelopment.meteorclient.utils.render.ui.rectangle.rectrotatinggradient.RotatingGradientRectangleRenderer;
import meteordevelopment.meteorclient.utils.render.ui.ripple.RippleRenderer;
import meteordevelopment.meteorclient.utils.render.ui.zippy.ZippyRenderer;

public final class UiRenderers {

    private UiRenderers() {}

    public static void bindParams(RenderPipeline pipeline, RenderPass pass) {
        if (pipeline == null || pass == null) {
            return;
        }

        if (BlurFramebuffer.getInstance().isBlurPipeline(pipeline)) {
            BlurFramebuffer.getInstance().bindBlurParams(pass);
            return;
        }
        if (GlassRenderer.getInstance().isGlassPipeline(pipeline)) {
            GlassRenderer.getInstance().bindParams(pass);
            return;
        }
        if (GlassOutlineRenderer.getInstance().isGlassOutlinePipeline(pipeline)) {
            GlassOutlineRenderer.getInstance().bindParams(pass);
            return;
        }
        if (GlowRenderer.getInstance().isGlowPipeline(pipeline)) {
            GlowRenderer.getInstance().bindParams(pass);
            return;
        }
        if (DefaultRectangleRenderer.getInstance().isRectanglePipeline(pipeline)) {
            DefaultRectangleRenderer.getInstance().bindParams(pass);
            return;
        }
        if (GradientRectangleRenderer.getInstance().isGradientRectanglePipeline(pipeline)) {
            GradientRectangleRenderer.getInstance().bindParams(pass);
            return;
        }
        if (RotatingGradientRectangleRenderer.getInstance().isRotatingGradientRectanglePipeline(pipeline)) {
            RotatingGradientRectangleRenderer.getInstance().bindParams(pass);
            return;
        }
        if (HalftoneRectangleRenderer.getInstance().isHalftoneRectanglePipeline(pipeline)) {
            HalftoneRectangleRenderer.getInstance().bindParams(pass);
            return;
        }
        if (ZippyRenderer.getInstance().isZippyPipeline(pipeline)) {
            ZippyRenderer.getInstance().bindParams(pass);
            return;
        }
        if (ArcRenderer.getInstance().isArcPipeline(pipeline)) {
            ArcRenderer.getInstance().bindParams(pass);
            return;
        }
        if (ArcOutlineRenderer.getInstance().isArcOutlinePipeline(pipeline)) {
            ArcOutlineRenderer.getInstance().bindParams(pass);
            return;
        }
        if (DefaultOutlineRenderer.getInstance().isOutlinePipeline(pipeline)) {
            DefaultOutlineRenderer.getInstance().bindParams(pass);
            return;
        }
        if (Outline360Renderer.getInstance().isOutline360Pipeline(pipeline)) {
            Outline360Renderer.getInstance().bindParams(pass);
            return;
        }
        if (MsdfRenderer.getInstance().isMsdfPipeline(pipeline)) {
            MsdfRenderer.getInstance().bindParams(pass);
            return;
        }
        if (ImageRenderer.getInstance().isImagePipeline(pipeline)) {
            ImageRenderer.getInstance().bindParams(pass);
            return;
        }
        if (RippleRenderer.getInstance().isRipplePipeline(pipeline)) {
            RippleRenderer.getInstance().bindParams(pass);
            return;
        }
        if (RenderItem.isItemPipeline(pipeline)) {
            RenderItem.bindParams(pass);
        }
    }

}
