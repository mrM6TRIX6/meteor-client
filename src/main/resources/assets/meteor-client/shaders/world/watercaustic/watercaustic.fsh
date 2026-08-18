#version 410 core

in vec2 vUV;
out vec4 fragColor;

layout(std140) uniform WaterData {
    vec4 iColor;
    vec4 iPatternColor;
    float iTime;
};

#define TAU 6.28318530718
#define MAX_ITER 5

void main() {
    float time = iTime * .5 + 23.0;
    vec2 uv = vUV * iPatternColor.w;
    vec2 p = mod(uv * TAU, TAU) - 250.0;
    vec2 i = vec2(p);
    float c = 1.0;
    float inten = .005;

    for (int n = 0; n < MAX_ITER; n++) {
        float t = time * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
        c += 1.0 / length(vec2(p.x / (sin(i.x + t) / inten), p.y / (cos(i.y + t) / inten)));
    }

    c /= float(MAX_ITER);
    c = 1.17 - pow(c, 1.4);
    float pattern = clamp(pow(abs(c), 8.0), 0.0, 1.0);
    vec3 finalRGB = mix(iColor.rgb, iPatternColor.rgb, pattern);
    fragColor = vec4(finalRGB, iColor.w);
}
