#version 150

#moj_import <meteor-client:common.glsl>

in vec2 FragCoord;
flat in int QuadIndex;

layout(std140) uniform RectangleParamsArray {
    vec4 params[3072];
};

out vec4 OutColor;

vec4 rectangleColor(vec2 coord, vec4 topLeft, vec4 topRight, vec4 bottomRight, vec4 bottomLeft) {
    vec4 top = mix(topLeft, topRight, coord.x);
    vec4 bottom = mix(bottomLeft, bottomRight, coord.x);
    return mix(top, bottom, coord.y);
}

void main() {
    int base = QuadIndex * 6;

    vec4 radius = params[base];
    vec4 sizeSmooth = params[base + 1];
    vec4 colorTopLeft = params[base + 2];
    vec4 colorTopRight = params[base + 3];
    vec4 colorBottomRight = params[base + 4];
    vec4 colorBottomLeft = params[base + 5];

    vec2 coord = clamp(FragCoord, vec2(0.0), vec2(1.0));
    vec2 size = max(sizeSmooth.xy, vec2(1.0));
    float alpha = ralpha(size, coord, max(radius, vec4(0.0)), sizeSmooth.z);
    vec4 color = rectangleColor(coord, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
    color.a *= alpha;

    if (color.a <= 0.001) {
        discard;
    }

    OutColor = color;
}