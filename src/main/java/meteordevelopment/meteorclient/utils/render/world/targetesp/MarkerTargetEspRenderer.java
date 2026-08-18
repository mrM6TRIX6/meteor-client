package meteordevelopment.meteorclient.utils.render.world.targetesp;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import meteordevelopment.meteorclient.utils.render.pipeline.ClientPipelines;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

public final class MarkerTargetEspRenderer {
    
    private static final Identifier MARKER_TEXTURE = MeteorClient.identifier("textures/features/targetesp/marker.png");
    
    private MarkerTargetEspRenderer() {}
    
    public static void render(MatrixStack stack, VertexConsumerProvider.Immediate provider, TargetEspRenderContext context) {
        VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(MARKER_TEXTURE));
        Quaternionf camRot = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
        
        stack.push();
        stack.translate(0.0f, context.target().getHeight() / 2.0f, 0.0f);
        stack.multiply(camRot);
        
        float rotation = (context.frameTimeMs() % 3600L) / 3600.0f * 360.0f;
        float pulse = 1.0f + MathHelper.sin((context.frameTimeMs() % 1400L) / 1400.0f * MathHelper.TAU) * 0.08f;
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        stack.scale(0.65f * pulse, 0.65f * pulse, 1.0f);
        
        int c1 = ColorUtil.withAlpha(context.primaryColor(), (int) (255.0f * context.alpha()));
        int c2 = ColorUtil.withAlpha(context.secondaryColor(), (int) (255.0f * context.alpha()));
        int c3 = ColorUtil.withAlpha(context.primaryColor(), (int) (215.0f * context.alpha()));
        int c4 = ColorUtil.withAlpha(context.secondaryColor(), (int) (215.0f * context.alpha()));
        
        MatrixStack.Entry entry = stack.peek();
        consumer.vertex(entry, -1.0f, -1.0f, 0.0f).texture(0.0f, 0.0f).color(c1);
        consumer.vertex(entry, -1.0f, 1.0f, 0.0f).texture(0.0f, 1.0f).color(c2);
        consumer.vertex(entry, 1.0f, 1.0f, 0.0f).texture(1.0f, 1.0f).color(c3);
        consumer.vertex(entry, 1.0f, -1.0f, 0.0f).texture(1.0f, 0.0f).color(c4);
        stack.pop();
    }
    
    public static void endBatch(VertexConsumerProvider.Immediate provider) {
        provider.draw(ClientPipelines.ROMB_ESP.apply(MARKER_TEXTURE));
    }
    
}
