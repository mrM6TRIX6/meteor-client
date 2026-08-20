#version 150

uniform sampler2D Sampler0;

layout(std140) uniform KawaseParams {
    vec4 SourceRect;
    vec4 HalfPixel;
    vec4 FallbackColor;
};

in vec2 TexCoord;

out vec4 OutColor;

vec4 sampleSource(vec2 c) {
    return texture(Sampler0, clamp(c, vec2(0.0), vec2(1.0)));
}

void main() {
    vec2 uv = SourceRect.xy + TexCoord * SourceRect.zw;

    vec4 c0 = sampleSource(uv + vec2(-HalfPixel.x * 2.0, 0.0));
    vec4 c1 = sampleSource(uv + vec2(-HalfPixel.x, HalfPixel.y));
    vec4 c2 = sampleSource(uv + vec2(0.0, HalfPixel.y * 2.0));
    vec4 c3 = sampleSource(uv + vec2(HalfPixel.x, HalfPixel.y));
    vec4 c4 = sampleSource(uv + vec2(HalfPixel.x * 2.0, 0.0));
    vec4 c5 = sampleSource(uv + vec2(HalfPixel.x, -HalfPixel.y));
    vec4 c6 = sampleSource(uv + vec2(0.0, -HalfPixel.y * 2.0));
    vec4 c7 = sampleSource(uv + vec2(-HalfPixel.x, -HalfPixel.y));

    vec4 sum;
    sum.rgb = c0.rgb * c0.a + c1.rgb * c1.a * 2.0 + c2.rgb * c2.a + c3.rgb * c3.a * 2.0
    + c4.rgb * c4.a + c5.rgb * c5.a * 2.0 + c6.rgb * c6.a + c7.rgb * c7.a * 2.0;
    sum.a = c0.a + c1.a * 2.0 + c2.a + c3.a * 2.0 + c4.a + c5.a * 2.0 + c6.a + c7.a * 2.0;

    float a = sum.a / 12.0;
    if (a <= 0.0001) { OutColor = vec4(0.0); return; }
    OutColor = vec4(sum.rgb / max(sum.a, 1e-4), a);
}
