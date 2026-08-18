#version 150

#moj_import <meteor-client:common.glsl>

in vec2 FragCoord;
flat in int QuadIndex;

layout(std140) uniform GradientRectangleParamsArray {
    vec4 params[2560];
};

out vec4 OutColor;

void main() {
    int base = QuadIndex * 5;

    vec4 radius = params[base + 0];
    vec4 sizeTime = params[base + 1];
    vec4 fx = params[base + 2];
    vec4 firstColor = params[base + 3];
    vec4 secondColor = params[base + 4];

    vec2 size = sizeTime.xy;
    float time = sizeTime.z;

    float smoothness = fx.x;
    float speed = fx.y;
    float frequency = fx.z;
    float angle = fx.w;

    vec2 coord = clamp(FragCoord, vec2(0.0), vec2(1.0));

    float alpha = ralpha(size, coord, max(radius, vec4(0.0)), smoothness);

    float rad = radians(angle);
    vec2 dir = vec2(cos(rad), sin(rad));
    float gradientPos = dot(coord - 0.5, dir) + 0.5;
    float t = sin(gradientPos * frequency - time * speed) * 0.5 + 0.5;

    vec4 color = mix(firstColor, secondColor, t);

    color.a *= alpha;

    if (color.a <= 0.001) {
        discard;
    }

    OutColor = color;
}