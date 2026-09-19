package meteordevelopment.meteorclient.mixin.iris;

import meteordevelopment.meteorclient.utils.render.post.handsflame.IrisShaderCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
public abstract class IrisRenderingPipelineHandsMixin {

    @Inject(method = "finalizeLevelRendering", at = @At("TAIL"), remap = false, require = 0)
    private void meteor$renderHandsFlameAfterIrisFinalPass(CallbackInfo ci) {
        IrisShaderCompat.renderHandsFlameAfterIrisFinalPass();
    }
    
}
