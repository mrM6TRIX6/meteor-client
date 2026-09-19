package meteordevelopment.meteorclient.mixin.iris;

import meteordevelopment.meteorclient.utils.render.post.handsflame.IrisShaderCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.irisshaders.iris.compat.sodium.impl.shader_hand.ShaderHandRenderer", remap = false)
public abstract class IrisHandRendererHandsMixin {

    @Inject(
        method = {"renderSolid", "renderTranslucent"},
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;backupProjectionMatrix()V"),
        remap = false,
        require = 0
    )
    private void meteor$beginHandsFlameDepthCapture(CallbackInfo ci) {
        IrisShaderCompat.beginHandDepthCapture();
    }

    @Inject(method = {"renderSolid", "renderTranslucent"}, at = @At("TAIL"), remap = false, require = 0)
    private void meteor$endHandsFlameDepthCapture(CallbackInfo ci) {
        IrisShaderCompat.endHandDepthCapture();
    }
    
}
