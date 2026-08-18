#version 150

#moj_import <meteor-client:common.glsl>

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 TexCoord;
in vec2 FragCoord;
in vec4 FragColor;
flat in int QuadIndex;

layout(std140) uniform ImageParamsArray {
    vec4 params[3072];
};

out vec4 OutColor;

void main() {
    int base = QuadIndex * 3;
    vec4 radius = params[base];
    vec4 sizeSmooth = params[base + 1];
    vec4 uv = params[base + 2];

    vec2 coord = clamp(FragCoord, vec2(0.0), vec2(1.0));
    float shapeAlpha = ralpha(max(sizeSmooth.xy, vec2(1.0)), coord, radius, sizeSmooth.z);
    vec2 sampleCoord = mix(uv.xy, uv.zw, TexCoord);
    vec4 textureColor = texture(Sampler0, sampleCoord);
    vec4 color = textureColor * FragColor;
    color.a *= shapeAlpha;

    if (color.a <= 0.001) {
        discard;
    }

    OutColor = color * ColorModulator;
}
