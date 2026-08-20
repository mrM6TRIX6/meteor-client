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
    vec4 c0 = sampleSource(uv);
    vec4 c1 = sampleSource(uv - HalfPixel.xy);
    vec4 c2 = sampleSource(uv + HalfPixel.xy);
    vec4 c3 = sampleSource(uv + vec2(HalfPixel.x, -HalfPixel.y));
    vec4 c4 = sampleSource(uv - vec2(HalfPixel.x, -HalfPixel.y));

    vec4 sum;
    sum.rgb = c0.rgb * c0.a * 4.0 + c1.rgb * c1.a + c2.rgb * c2.a + c3.rgb * c3.a + c4.rgb * c4.a;
    sum.a = c0.a * 4.0 + c1.a + c2.a + c3.a + c4.a;

    float a = sum.a / 8.0;
    if (a <= 0.0001) { OutColor = vec4(0.0); return; }
    OutColor = vec4(sum.rgb / max(sum.a, 1e-4), a);
}
