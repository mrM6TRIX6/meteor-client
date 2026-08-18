#version 150

#moj_import <meteor-client:common.glsl>

in vec2 FragCoord;
flat in int QuadIndex;

layout(std140) uniform OutlineParamsArray {
    vec4 params[3072];
};

out vec4 OutColor;

vec4 outlineColor(vec2 coord, vec4 topLeft, vec4 topRight, vec4 bottomRight, vec4 bottomLeft) {
    vec4 top = mix(topLeft, topRight, coord.x);
    vec4 bottom = mix(bottomLeft, bottomRight, coord.x);
    return mix(top, bottom, coord.y);
}

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
    vec4 radius = params[base];
    vec4 sizeThicknessSmooth = params[base + 1];
    vec4 topLeft = params[base + 2];
    vec4 topRight = params[base + 3];
    vec4 bottomRight = params[base + 4];
    vec4 bottomLeft = params[base + 5];

    vec2 coord = clamp(FragCoord, vec2(0.0), vec2(1.0));
    vec2 size = max(sizeThicknessSmooth.xy, vec2(1.0));
    float thickness = max(sizeThicknessSmooth.z, 0.0);
    float alpha = outlineAlpha(coord, size, radius, thickness, sizeThicknessSmooth.w);

    vec4 color = outlineColor(coord, topLeft, topRight, bottomRight, bottomLeft);
    color.a *= alpha;

    if (color.a <= 0.001) {
        discard;
    }

    OutColor = color;
}
