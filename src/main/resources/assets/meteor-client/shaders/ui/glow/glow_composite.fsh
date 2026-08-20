#version 150

uniform sampler2D Sampler0;

in vec2 FragCoord;
flat in int QuadIndex;

layout(std140) uniform GlowParamsArray {
    vec4 params[1120];
};

out vec4 OutColor;

void main() {
    int base = QuadIndex * 5;

    vec4 colorVec = params[base + 2];
    vec4 reg      = params[base + 3];

    vec2 halfTexel = 0.5 / vec2(textureSize(Sampler0, 0));
    vec2 lo = reg.xy + halfTexel;
    vec2 hi = max(reg.xy + reg.zw - halfTexel, lo);
    vec4 blurred = texture(Sampler0, clamp(reg.xy + FragCoord * reg.zw, lo, hi));

    float a = clamp(blurred.a * colorVec.a, 0.0, 1.0);
    if (a <= 0.0) discard;

    OutColor = vec4(blurred.rgb * a, a);
}
