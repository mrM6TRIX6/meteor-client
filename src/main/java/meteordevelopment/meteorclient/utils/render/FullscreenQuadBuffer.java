package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.ByteBuffer;

public final class FullscreenQuadBuffer {
    
    private static GpuBuffer buffer;
    
    private FullscreenQuadBuffer() {}
    
    public static GpuBuffer getOrCreate() {
        var device = RenderSystem.tryGetDevice();
        if (device == null) {
            return null;
        }
        if (buffer != null && !buffer.isClosed()) {
            return buffer;
        }
        
        ByteBuffer data = ByteBuffer.allocateDirect(4);
        data.putInt(0);
        data.flip();
        buffer = device.createBuffer(
            () -> "meteor-client:fullscreen_quad_dummy",
            GpuBuffer.USAGE_VERTEX,
            data
        );
        return buffer;
    }
    
    public static void close() {
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
    }
    
}
