#version 150

#moj_import <meteor-client:common.glsl>

in vec2 FragCoord;
flat in int QuadIndex;

uniform sampler2D Sampler0;

layout(std140) uniform GlassOutlineParamsArray {
    vec4 params[3072];
};

layout(std140) uniform BlurRegion {
    vec4 Region;
};

out vec4 OutColor;

float outlineAlpha(vec2 coord, vec2 size, vec4 radius, float thickness, float smoothness) {
    float outer = ralpha(size, coord, radius, smoothness);
    vec2 innerSize = size - vec2(thickness * 2.0);
    if (innerSize.x <= 0.0 || innerSize.y <= 0.0) {
        return outer;
    }

    vec2 local = coord * size;
    vec2 innerCoord = (local - vec2(thickness)) / innerSize;
    vec4 innerRadius = max(radius - vec4(thickness), vec4(0.0));
    float inner = ralpha(innerSize, innerCoord, innerRadius, smoothness);
    return clamp(outer - inner, 0.0, 1.0);
}

void main() {
    int base = QuadIndex * 6;
    vec4 radius = max(params[base], vec4(0.0));
    vec4 sizeThicknessSmooth = params[base + 1];
    vec4 alphaPowerMix = params[base + 2];
    vec4 fresnelColor = params[base + 3];
    vec4 flagsDistortZSquirt = params[base + 4];
    vec4 tintColor = params[base + 5];

    vec2 coord = clamp(FragCoord, vec2(0.0), vec2(1.0));
    vec2 size = max(sizeThicknessSmooth.xy, vec2(1.0));
    float thickness = max(sizeThicknessSmooth.z, 0.0);
    float smoothness = max(sizeThicknessSmooth.w, 0.001);
    float globalAlpha = clamp(alphaPowerMix.x, 0.0, 1.0);
    float fresnelPower = max(alphaPowerMix.y, 0.001);
    float baseAlpha = clamp(alphaPowerMix.z, 0.0, 1.0);
    float fresnelMix = clamp(alphaPowerMix.w, 0.0, 1.0);
    float fresnelInvert = flagsDistortZSquirt.x;
    float distortStrength = flagsDistortZSquirt.y;
    float shinePhase = flagsDistortZSquirt.w;

    float alpha = outlineAlpha(coord, size, radius, thickness, smoothness);

    vec2 center = size * 0.5;
    vec2 halfSize = max(center - 1.0, vec2(0.0));
    vec2 pos = center - coord * size;
    float distToEdge = abs(rdist(pos, halfSize, radius));
    float maxDistNorm = max(min(halfSize.x, halfSize.y), 0.001);
    float edgeGradient = 1.0 - clamp(distToEdge / maxDistNorm, 0.0, 1.0);
    float fresnelBase = (fresnelInvert > 0.5) ? edgeGradient : (1.0 - edgeGradient);

    float fresnel;
    if (fresnelPower > 20.0) {
        fresnel = exp(fresnelPower * log(clamp(fresnelBase, 0.001, 1.0)));
    } else {
        fresnel = pow(clamp(fresnelBase, 0.0, 1.0), fresnelPower);
    }
    fresnel = clamp(fresnel, 0.0, 1.0);

    vec2 dir = (length(pos) > 0.001) ? normalize(-pos) : vec2(0.0);
    vec2 texCoord = clamp((gl_FragCoord.xy - Region.xy) / max(Region.zw, vec2(1.0)), vec2(0.0), vec2(1.0));
    vec4 texColor = texture(Sampler0, texCoord + dir * fresnel * distortStrength);

    vec3 tintedColor = mix(texColor.rgb, tintColor.rgb, clamp(tintColor.a, 0.0, 1.0) * 0.65);
    vec3 finalColor = mix(tintedColor, fresnelColor.rgb, fresnel * fresnelMix);
    float finalAlpha = mix(baseAlpha, max(fresnelColor.a, tintColor.a), fresnel) * alpha * globalAlpha;

    if (shinePhase < 0.999) {
        float distTop = coord.y;
        float distRight = 1.0 - coord.x;
        float distBottom = 1.0 - coord.y;
        float distLeft = coord.x;

        float topMask = step(distTop, distRight) * step(distTop, distBottom) * step(distTop, distLeft);
        float rightMask = step(distRight, distTop) * step(distRight, distBottom) * step(distRight, distLeft);
        float bottomMask = step(distBottom, distTop) * step(distBottom, distRight) * step(distBottom, distLeft);
        float leftMask = step(distLeft, distTop) * step(distLeft, distRight) * step(distLeft, distBottom);

        float pathA = topMask * (coord.x * 0.5) + rightMask * (0.5 + coord.y * 0.5);
        float maskA = clamp(topMask + rightMask, 0.0, 1.0);
        float pathB = leftMask * (coord.y * 0.5) + bottomMask * (0.5 + coord.x * 0.5);
        float maskB = clamp(leftMask + bottomMask, 0.0, 1.0);

        float center = shinePhase * 1.34 - 0.17;
        float cycleFade = smoothstep(0.0, 0.14, shinePhase) * (1.0 - smoothstep(0.86, 1.0, shinePhase));
        float widthA = 0.18;
        float widthB = 0.16;
        float shineA = smoothstep(widthA, 0.0, abs(pathA - center)) * maskA;
        float shineB = smoothstep(widthB, 0.0, abs(pathB - center)) * maskB;
        float outlineFade = clamp(max(max(tintColor.a, fresnelColor.a), baseAlpha) * globalAlpha, 0.0, 1.0);
        float shine = max(shineA, shineB * 0.9) * cycleFade * alpha * outlineFade;
        vec3 shineColor = vec3(1.0);
        finalColor = mix(finalColor, shineColor, shine * 0.78);
        finalAlpha = max(finalAlpha, shine * 0.66);
    }

    if (finalAlpha < 0.001) {
        discard;
    }

    OutColor = vec4(finalColor, finalAlpha);
}
