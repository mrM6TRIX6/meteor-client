package meteordevelopment.meteorclient.utils.render;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public final class WorldVertex {
    
    private static final int FULL_BRIGHT = 0x00F000F0;
    
    private WorldVertex() {}
    
    public static void textured(VertexConsumer consumer, MatrixStack.Entry pose, float x, float y, float z, float u, float v, int color) {
        consumer.vertex(pose, x, y, z)
            .texture(u, v)
            .color(color)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(FULL_BRIGHT)
            .normal(pose, 0.0F, 0.0F, 1.0F);
    }
    
}
