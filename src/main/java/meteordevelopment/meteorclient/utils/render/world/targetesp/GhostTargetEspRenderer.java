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
import net.minecraft.util.math.Vec3d;

public final class GhostTargetEspRenderer {
    
    private static final Identifier GHOST_TEXTURE = MeteorClient.identifier("textures/particles/ghost-glow.png");
    
    
    private GhostTargetEspRenderer() {}
    
    public static void render(MatrixStack stack, VertexConsumerProvider.Immediate provider, TargetEspRenderContext context) {
        VertexConsumer consumer = provider.getBuffer(ClientPipelines.GHOSTS_ESP.apply(GHOST_TEXTURE));
        
        stack.push();
        stack.translate(0.0f, context.target().getHeight() * 0.60f, 0.0f);
        particle(stack, consumer, context, TransformationType.FIRST);
        particle(stack, consumer, context, TransformationType.SECOND);
        particle(stack, consumer, context, TransformationType.THIRD);
        stack.pop();
    }
    
    private static void particle(MatrixStack stack, VertexConsumer consumer, TargetEspRenderContext context, TransformationType transformation) {
        double radius = 0.8f;
        double distance = 20.0;
        float particleSize = 1;
        int alphaFactor = 1;
        
        int particleCount = Math.max(4, Math.round(10 * context.alpha()));
        for (int i = 0; i < particleCount; i++) {
            stack.push();
            
            double angle = 0.2 * ((context.frameTimeMs() * 0.55) - (i * distance)) / 40.0;
            double sin = Math.sin(angle) * radius;
            double cos = Math.cos(angle) * radius;
            
            Vec3d trans = transformation.make(sin, cos);
            stack.translate(trans.x, trans.y, trans.z);
            stack.multiply(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());
            
            float spinRotation = (context.frameTimeMs() * 0.1f) - (i * 10.0f);
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spinRotation));
            stack.translate(particleSize / 2.0f, particleSize / 2.0f, 0.0f);
            
            int currentAlpha = (int) ((255 - i * alphaFactor) * context.alpha());
            int startColor = ColorUtil.withAlpha(context.primaryColor(), currentAlpha);
            int endColor = ColorUtil.withAlpha(context.secondaryColor(), currentAlpha);
            
            MatrixStack.Entry entry = stack.peek();
            consumer.vertex(entry, 0.0f, -particleSize, 0.0f).texture(0.0f, 0.0f).color(endColor);
            consumer.vertex(entry, -particleSize, -particleSize, 0.0f).texture(0.0f, 1.0f).color(endColor);
            consumer.vertex(entry, -particleSize, 0.0f, 0.0f).texture(1.0f, 1.0f).color(startColor);
            consumer.vertex(entry, 0.0f, 0.0f, 0.0f).texture(1.0f, 0.0f).color(startColor);
            
            stack.pop();
        }
    }
    
    public static void endBatch(VertexConsumerProvider.Immediate provider) {
        provider.draw(ClientPipelines.GHOSTS_ESP.apply(GHOST_TEXTURE));
    }
    
    private enum TransformationType {
        FIRST {
            @Override
            Vec3d make(double sin, double cos) {
                return new Vec3d(sin, cos, -cos);
            }
        },
        SECOND {
            @Override
            Vec3d make(double sin, double cos) {
                return new Vec3d(-sin, sin, -cos);
            }
        },
        THIRD {
            @Override
            Vec3d make(double sin, double cos) {
                return new Vec3d(-sin, -sin, cos);
            }
        };
        
        abstract Vec3d make(double sin, double cos);
    }
    
}
