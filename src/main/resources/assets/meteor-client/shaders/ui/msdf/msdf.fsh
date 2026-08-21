#version 150
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(std140) uniform MsdfParamsArray {
    vec4 params[2048];
};

in vec2 TexCoord;
in vec4 VertexColor;
flat in int MsdfIndex;

out vec4 OutColor;

float median(vec3 c) {
    return max(min(c.r, c.g), min(max(c.r, c.g), c.b));
}

void main() {
    int base = MsdfIndex * 2;
    vec4 a = params[base];
    vec4 b = params[base + 1];

    float range = a.x;
    float thickness = a.y;
    float smoothness = a.z;
    float outlineThickness = a.w;
    bool outline = outlineThickness > 0.001;
    vec4 outlineColor = b;

    float dist = median(texture(Sampler0, TexCoord).rgb) - 0.5 + thickness;

    vec2 h = vec2(dFdx(TexCoord.x), dFdy(TexCoord.y)) * vec2(textureSize(Sampler0, 0));
    float pixels = range * inversesqrt(max(dot(h, h), 1e-8));
    pixels = max(pixels, 4.0);

    float alpha = smoothstep(-smoothness, smoothness, dist * pixels);
    vec4 color = vec4(VertexColor.rgb, VertexColor.a * alpha);

    if (outline) {
        color = mix(outlineColor, VertexColor, alpha);
        color.a = VertexColor.a * smoothstep(-smoothness, smoothness, (dist + outlineThickness) * pixels);
    }

    if (color.a < 0.002) discard;
    OutColor = color * ColorModulator;
}