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

public final class RhombTargetEspRenderer {
    
    private static final Identifier RHOMB_TEXTURE = MeteorClient.identifier("textures/world/cube.png");
    
    private RhombTargetEspRenderer() {
    }
    
    public static void render(MatrixStack stack, VertexConsumerProvider.Immediate provider, TargetEspRenderContext context) {
        VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(RHOMB_TEXTURE));
        Quaternionf camRot = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
        
        stack.push();
        stack.translate(0.0f, context.target().getHeight() / 2.0f, 0.0f);
        stack.multiply(camRot);
        
        float timeRotation = (context.frameTimeMs() % 6283L) / 1000.0f;
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(timeRotation) * 360.0f));
        stack.scale(0.35f, 0.35f, 1.0f);
        
        int c1 = ColorUtil.withAlpha(context.primaryColor(), (int) (255.0f * context.alpha()));
        int c2 = ColorUtil.withAlpha(context.secondaryColor(), (int) (255.0f * context.alpha()));
        
        MatrixStack.Entry entry = stack.peek();
        consumer.vertex(entry, -1.0f, -1.0f, 0.0f).texture(0.0f, 0.0f).color(c2);
        consumer.vertex(entry, -1.0f, 1.0f, 0.0f).texture(0.0f, 1.0f).color(c1);
        consumer.vertex(entry, 1.0f, 1.0f, 0.0f).texture(1.0f, 1.0f).color(c2);
        consumer.vertex(entry, 1.0f, -1.0f, 0.0f).texture(1.0f, 0.0f).color(c1);
        stack.pop();
    }
    
    public static void endBatch(VertexConsumerProvider.Immediate provider) {
        provider.draw(ClientPipelines.ROMB_ESP.apply(RHOMB_TEXTURE));
    }
    
}
