package meteordevelopment.meteorclient.utils.render.world.targetesp;

import meteordevelopment.meteorclient.utils.render.WorldVertex;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

public final class TargetEspQuadRenderer {
    
    private TargetEspQuadRenderer() {}
    
    public static void billboard(MatrixStack stack, VertexConsumerProvider.Immediate provider, Identifier texture, float y, float scale, float rotation, int c1, int c2, int c3, int c4) {
        VertexConsumer consumer = provider.getBuffer(RenderLayers.entityTranslucent(texture));
        Quaternionf cameraRotation = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
        stack.push();
        stack.translate(0.0f, y, 0.0f);
        stack.multiply(cameraRotation);
        if (rotation != 0.0f) {
            stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        }
        stack.scale(scale, scale, 1.0f);
        
        MatrixStack.Entry pose = stack.peek();
        WorldVertex.textured(consumer, pose, -1.0f, -1.0f, 0.0f, 0.0f, 0.0f, c1);
        WorldVertex.textured(consumer, pose, -1.0f, 1.0f, 0.0f, 0.0f, 1.0f, c2);
        WorldVertex.textured(consumer, pose, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, c3);
        WorldVertex.textured(consumer, pose, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, c4);
        stack.pop();
    }
    
    public static void end(VertexConsumerProvider.Immediate provider, Identifier texture) {
        provider.draw(RenderLayers.entityTranslucent(texture));
    }
    
}
