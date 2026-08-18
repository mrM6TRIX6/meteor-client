package meteordevelopment.meteorclient.utils.render.world.targetesp;

import net.minecraft.entity.LivingEntity;

public record TargetEspRenderContext(
    LivingEntity target,
    float alpha,
    float partialTicks,
    long frameTimeMs,
    int primaryColor,
    int secondaryColor,
    float hurtProgress,
    float chainImpactProgress
) {}
