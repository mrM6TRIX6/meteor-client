#version 150

#moj_import <meteor-client:common.glsl>

in vec2 FragCoord;
flat in int QuadIndex;

layout(std140) uniform HalftoneRectangleParamsArray {
    vec4 params[3072];
};

out vec4 OutColor;

vec4 rectangleColor(vec2 coord, vec4 topLeft, vec4 topRight, vec4 bottomRight, vec4 bottomLeft) {
    vec4 top = mix(topLeft, topRight, coord.x);
    vec4 bottom = mix(bottomLeft, bottomRight, coord.x);
    return mix(top, bottom, coord.y);
}

float halftoneAlpha(vec2 local, float dotSize, float dotSpacing) {
    if (dotSize <= 0.0) {
        return 0.0;
    }

    float pitch = max(dotSize + max(dotSpacing, 0.0), dotSize + 0.001);
    vec2 dotCenter = (floor(local / pitch) + vec2(0.5)) * pitch;
    float dist = length(local - dotCenter);
    float radius = dotSize * 0.5;
    float feather = max(fwidth(dist), 0.55);
    return 1.0 - smoothstep(radius - feather, radius + feather, dist);
}

vec4 over(vec4 top, vec4 bottom) {
    float alpha = top.a + bottom.a * (1.0 - top.a);
    vec3 color = (top.rgb * top.a + bottom.rgb * bottom.a * (1.0 - top.a)) / max(alpha, 0.001);
    return vec4(color, alpha);
}

void main() {
    int base = QuadIndex * 8;
    vec4 radius = params[base];
    vec4 sizeSmoothDot = params[base + 1];
    vec4 topLeft = params[base + 2];
    vec4 topRight = params[base + 3];
    vec4 bottomRight = params[base + 4];
    vec4 bottomLeft = params[base + 5];
    vec4 dotColor = params[base + 6];
    vec4 dotInfo = params[base + 7];

    vec2 coord = clamp(FragCoord, vec2(0.0), vec2(1.0));
    vec2 size = max(sizeSmoothDot.xy, vec2(1.0));
    float shapeAlpha = ralpha(size, coord, radius, sizeSmoothDot.z);

    vec4 baseColor = rectangleColor(coord, topLeft, topRight, bottomRight, bottomLeft);
    baseColor.a *= shapeAlpha;

    float dots = halftoneAlpha(coord * size, sizeSmoothDot.w, dotInfo.x) * shapeAlpha;
    vec4 dotsColor = dotColor;
    dotsColor.a *= dots;

    vec4 color = over(dotsColor, baseColor);
    if (color.a <= 0.001) {
        discard;
    }

    OutColor = color;
}
