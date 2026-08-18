#version 150

#moj_import <meteor-client:common.glsl>

in vec2 FragCoord;
flat in int QuadIndex;

layout(std140) uniform RotatingGradientRectangleParamsArray {
    vec4 params[3584];
};

out vec4 OutColor;

void main() {
    int base = QuadIndex * 7;

    vec4 radius = params[base + 0];
    vec4 sizeTime = params[base + 1];
    vec4 smoothSpeed = params[base + 2];
    vec4 color1 = params[base + 3];
    vec4 color2 = params[base + 4];
    vec4 color3 = params[base + 5];
    vec4 color4 = params[base + 6];

    vec2 size = sizeTime.xy;
    float time = sizeTime.z;

    float smoothness = smoothSpeed.x;
    float speed = smoothSpeed.y;

    vec2 coord = clamp(FragCoord, vec2(0.0), vec2(1.0));

    float alpha = ralpha(size, coord, max(radius, vec4(0.0)), smoothness);

    float s = sin(time * speed);
    float c = cos(time * speed);
    mat2 mRot = mat2(c, -s, s, c);
    vec2 gradientPos = (coord - 0.5) * mRot + 0.5;

    vec4 top = mix(color1, color2, gradientPos.x);
    vec4 bottom = mix(color3, color4, gradientPos.x);

    vec4 color = mix(top, bottom, gradientPos.y);

    color.a *= alpha;

    if (color.a <= 0.001) {
        discard;
    }

    OutColor = color;
}