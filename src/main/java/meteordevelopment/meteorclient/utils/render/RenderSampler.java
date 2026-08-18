package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.GpuSampler;

import java.util.OptionalDouble;

public final class RenderSampler {
    
    private static GpuSampler linear;
    private static GpuSampler nearest;
    
    private RenderSampler() {}
    
    public static GpuSampler linear() {
        if (linear == null) {
            linear = RenderSystem.getDevice().createSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR,
                FilterMode.LINEAR,
                1,
                OptionalDouble.empty()
            );
        }
        return linear;
    }
    
    public static GpuSampler nearest() {
        if (nearest == null) {
            nearest = RenderSystem.getDevice().createSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.NEAREST,
                FilterMode.NEAREST,
                1,
                OptionalDouble.empty()
            );
        }
        return nearest;
    }
    
}
