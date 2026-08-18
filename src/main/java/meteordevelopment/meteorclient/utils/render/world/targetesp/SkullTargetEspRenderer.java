package meteordevelopment.meteorclient.utils.render.world.targetesp;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import meteordevelopment.meteorclient.utils.render.pipeline.ClientPipelines;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

public final class SkullTargetEspRenderer {
    
    private static final Identifier SKULL_TEXTURE = MeteorClient.identifier("textures/features/targetesp/skull.png");
    
    private SkullTargetEspRenderer() {}
    
    public static void render(MatrixStack stack, VertexConsumerProvider.Immediate provider, TargetEspRenderContext context) {
        VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(SKULL_TEXTURE));
        Quaternionf camRot = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
        
        float impact = TargetEspMath.easeOutCubic(context.chainImpactProgress());
        float scale = 0.48f * (1.0f - 0.14f * impact);
        float verticalOffset = context.target().getHeight() * 0.50f;
        float alpha = Math.min(1.0f, context.alpha() * 1.85f);
        
        int c1 = ColorUtil.withAlpha(context.primaryColor(), (int) (255.0f * alpha));
        int c2 = ColorUtil.withAlpha(context.secondaryColor(), (int) (255.0f * alpha));
        int c3 = ColorUtil.withAlpha(context.primaryColor(), (int) (250.0f * alpha));
        int c4 = ColorUtil.withAlpha(context.secondaryColor(), (int) (250.0f * alpha));
        
        stack.push();
        stack.translate(0.0f, verticalOffset, 0.0f);
        stack.multiply(camRot);
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        stack.scale(scale, scale, 1.0f);
        
        MatrixStack.Entry entry = stack.peek();
        consumer.vertex(entry, -0.8f, -0.8f, 0.0f).texture(0.0f, 0.0f).color(c1);
        consumer.vertex(entry, -0.8f, 0.8f, 0.0f).texture(0.0f, 1.0f).color(c2);
        consumer.vertex(entry, 0.8f, 0.8f, 0.0f).texture(1.0f, 1.0f).color(c3);
        consumer.vertex(entry, 0.8f, -0.8f, 0.0f).texture(1.0f, 0.0f).color(c4);
        stack.pop();
    }
    
    public static void endBatch(VertexConsumerProvider.Immediate provider) {
        provider.draw(ClientPipelines.ROMB_ESP.apply(SKULL_TEXTURE));
    }
    
}
