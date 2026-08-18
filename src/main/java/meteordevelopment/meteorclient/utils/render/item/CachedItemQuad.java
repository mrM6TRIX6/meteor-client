package meteordevelopment.meteorclient.utils.render.item;

import net.minecraft.util.Identifier;

record CachedItemQuad(
    Identifier atlas,
    float x0,
    float y0,
    float u0,
    float v0,
    float x1,
    float y1,
    float u1,
    float v1,
    float x2,
    float y2,
    float u2,
    float v2,
    float x3,
    float y3,
    float u3,
    float v3,
    int tint,
    boolean foil
) {}
