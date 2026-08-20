/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    
    @Unique
    private RenderPass meteor$currentRenderPass;
    
    @Unique
    private boolean meteor$blurDrawActive;
    
    @Unique
    private boolean meteor$glassDrawActive;
    
    @Unique
    private boolean meteor$glassOutlineDrawActive;
    
    @Unique
    private boolean meteor$glowDrawActive;
    
    @Unique
    private boolean meteor$rectangleDrawActive;
    
    @Unique
    private boolean meteor$gradientRectangleDrawActive;
    
    @Unique
    private boolean meteor$rotatingGradientRectangleDrawActive;
    
    @Unique
    private boolean meteor$halftoneRectangleDrawActive;
    
    @Unique
    private boolean meteor$zippyDrawActive;
    
    @Unique
    private boolean meteor$arcDrawActive;
    
    @Unique
    private boolean meteor$arcOutlineDrawActive;
    
    @Unique
    private boolean meteor$outlineDrawActive;
    
    @Unique
    private boolean meteor$outline360DrawActive;
    
    @Unique
    private boolean meteor$msdfDrawActive;
    
    @Unique
    private boolean meteor$imageDrawActive;
    
    @Unique
    private boolean meteor$itemDrawActive;
    
    @Unique
    private boolean meteor$rippleDrawActive;
    
    //    @Redirect(method = "renderPreparedDraws", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/systems/ProjectionType;)V", ordinal = 0))
    //    private void meteor$useModernWorldProjection(GpuBufferSlice projection, ProjectionType projectionType) {
    //        GpuBufferSlice override = WorldAnimation.projectionOverride();
    //        if (override != null) {
    //            RenderSystem.setProjectionMatrix(override, ProjectionType.PERSPECTIVE);
    //            return;
    //        }
    //        RenderSystem.setProjectionMatrix(projection, projectionType);
    //    }
    
    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"))
    private void meteor$beginBlurFrame(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        BlurFramebuffer.getInstance().beginGuiFrame();
        GlassRenderer.getInstance().beginGuiFrame();
        GlassOutlineRenderer.getInstance().beginGuiFrame();
        GlowRenderer.getInstance().beginGuiFrame();
        DefaultRectangleRenderer.getInstance().beginGuiFrame();
        GradientRectangleRenderer.getInstance().beginGuiFrame();
        RotatingGradientRectangleRenderer.getInstance().beginGuiFrame();
        HalftoneRectangleRenderer.getInstance().beginGuiFrame();
        ZippyRenderer.getInstance().beginGuiFrame();
        ArcRenderer.getInstance().beginGuiFrame();
        ArcOutlineRenderer.getInstance().beginGuiFrame();
        DefaultOutlineRenderer.getInstance().beginGuiFrame();
        Outline360Renderer.getInstance().beginGuiFrame();
        MsdfRenderer.getInstance().beginGuiFrame();
        ImageRenderer.getInstance().beginGuiFrame();
        RippleRenderer.getInstance().beginGuiFrame();
        RenderItem.beginGuiFrame();
    }
    
    @Inject(method = "prepare", at = @At("HEAD"))
    private void meteor$preparePendingBlurResources(CallbackInfo ci) {
        BlurFramebuffer.getInstance().preparePending();
        GlowRenderer.getInstance().preparePending();
    }
    
    @Inject(method = "prepare", at = @At("RETURN"))
    private void meteor$prepareRenderUniforms(CallbackInfo ci) {
        BlurFramebuffer.getInstance().prepareBuffers();
        GlassRenderer.getInstance().prepareBuffers();
        GlassOutlineRenderer.getInstance().prepareBuffers();
        GlowRenderer.getInstance().prepareBuffers();
        DefaultRectangleRenderer.getInstance().prepareBuffers();
        GradientRectangleRenderer.getInstance().prepareBuffers();
        RotatingGradientRectangleRenderer.getInstance().prepareBuffers();
        HalftoneRectangleRenderer.getInstance().prepareBuffers();
        ZippyRenderer.getInstance().prepareBuffers();
        ArcRenderer.getInstance().prepareBuffers();
        ArcOutlineRenderer.getInstance().prepareBuffers();
        DefaultOutlineRenderer.getInstance().prepareBuffers();
        Outline360Renderer.getInstance().prepareBuffers();
        MsdfRenderer.getInstance().prepareBuffers();
        ImageRenderer.getInstance().prepareBuffers();
        RippleRenderer.getInstance().prepareBuffers();
        RenderItem.prepareBuffers();
    }
    
    @Inject(method = "renderPreparedDraws", at = @At("HEAD"))
    private void meteor$prepareBlurCapture(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        BlurFramebuffer.getInstance().prepareGuiDraw();
    }
    
    @Inject(method = "renderPreparedDraws", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderBlur()V", shift = At.Shift.BEFORE))
    private void meteor$prepareBlurCaptureAfterBeforeBlur(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        BlurFramebuffer.getInstance().prepareGuiDraw();
    }
    
    @Redirect(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
    private void meteor$trackPipeline(RenderPass renderPass, RenderPipeline pipeline) {
        meteor$currentRenderPass = renderPass;
        meteor$blurDrawActive = BlurFramebuffer.getInstance().isBlurPipeline(pipeline);
        meteor$glassDrawActive = GlassRenderer.getInstance().isGlassPipeline(pipeline);
        meteor$glassOutlineDrawActive = GlassOutlineRenderer.getInstance().isGlassOutlinePipeline(pipeline);
        meteor$glowDrawActive = GlowRenderer.getInstance().isGlowPipeline(pipeline);
        meteor$rectangleDrawActive = DefaultRectangleRenderer.getInstance().isRectanglePipeline(pipeline);
        meteor$gradientRectangleDrawActive = GradientRectangleRenderer.getInstance().isGradientRectanglePipeline(pipeline);
        meteor$rotatingGradientRectangleDrawActive = RotatingGradientRectangleRenderer.getInstance().isRotatingGradientRectanglePipeline(pipeline);
        meteor$halftoneRectangleDrawActive = HalftoneRectangleRenderer.getInstance().isHalftoneRectanglePipeline(pipeline);
        meteor$zippyDrawActive = ZippyRenderer.getInstance().isZippyPipeline(pipeline);
        meteor$arcDrawActive = ArcRenderer.getInstance().isArcPipeline(pipeline);
        meteor$arcOutlineDrawActive = ArcOutlineRenderer.getInstance().isArcOutlinePipeline(pipeline);
        meteor$outlineDrawActive = DefaultOutlineRenderer.getInstance().isOutlinePipeline(pipeline);
        meteor$outline360DrawActive = Outline360Renderer.getInstance().isOutline360Pipeline(pipeline);
        meteor$msdfDrawActive = MsdfRenderer.getInstance().isMsdfPipeline(pipeline);
        meteor$imageDrawActive = ImageRenderer.getInstance().isImagePipeline(pipeline);
        meteor$rippleDrawActive = RippleRenderer.getInstance().isRipplePipeline(pipeline);
        meteor$itemDrawActive = RenderItem.isItemPipeline(pipeline);
        
        renderPass.setPipeline(pipeline);
    }
    
    @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V", shift = At.Shift.BEFORE))
    private void meteor$bindBlurParams(@Coerce Object draw, RenderPass renderPass, GpuBuffer gpuBuffer, VertexFormat.IndexType indexType, CallbackInfo ci) {
        if (meteor$blurDrawActive && meteor$currentRenderPass != null) {
            BlurFramebuffer.getInstance().bindBlurParams(meteor$currentRenderPass);
        }
        if (meteor$glassDrawActive && meteor$currentRenderPass != null) {
            GlassRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$glassOutlineDrawActive && meteor$currentRenderPass != null) {
            GlassOutlineRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$rectangleDrawActive && meteor$currentRenderPass != null) {
            DefaultRectangleRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$glowDrawActive && meteor$currentRenderPass != null) {
            GlowRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$gradientRectangleDrawActive && meteor$currentRenderPass != null) {
            GradientRectangleRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$rotatingGradientRectangleDrawActive && meteor$currentRenderPass != null) {
            RotatingGradientRectangleRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$halftoneRectangleDrawActive && meteor$currentRenderPass != null) {
            HalftoneRectangleRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$zippyDrawActive && meteor$currentRenderPass != null) {
            ZippyRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$arcDrawActive && meteor$currentRenderPass != null) {
            ArcRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$arcOutlineDrawActive && meteor$currentRenderPass != null) {
            ArcOutlineRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$outlineDrawActive && meteor$currentRenderPass != null) {
            DefaultOutlineRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$outline360DrawActive && meteor$currentRenderPass != null) {
            Outline360Renderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$msdfDrawActive && meteor$currentRenderPass != null) {
            MsdfRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$imageDrawActive && meteor$currentRenderPass != null) {
            ImageRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$rippleDrawActive && meteor$currentRenderPass != null) {
            RippleRenderer.getInstance().bindParams(meteor$currentRenderPass);
        }
        if (meteor$itemDrawActive && meteor$currentRenderPass != null) {
            RenderItem.bindParams(meteor$currentRenderPass);
        }
    }
    
    @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At("RETURN"))
    private void meteor$clearTrackedPipeline(@Coerce Object draw, RenderPass renderPass, GpuBuffer gpuBuffer, VertexFormat.IndexType indexType, CallbackInfo ci) {
        meteor$currentRenderPass = null;
        meteor$blurDrawActive = false;
        meteor$glassDrawActive = false;
        meteor$glassOutlineDrawActive = false;
        meteor$glowDrawActive = false;
        meteor$rectangleDrawActive = false;
        meteor$gradientRectangleDrawActive = false;
        meteor$rotatingGradientRectangleDrawActive = false;
        meteor$halftoneRectangleDrawActive = false;
        meteor$zippyDrawActive = false;
        meteor$arcDrawActive = false;
        meteor$arcOutlineDrawActive = false;
        meteor$outlineDrawActive = false;
        meteor$outline360DrawActive = false;
        meteor$msdfDrawActive = false;
        meteor$imageDrawActive = false;
        meteor$itemDrawActive = false;
        meteor$rippleDrawActive = false;
    }
    
}