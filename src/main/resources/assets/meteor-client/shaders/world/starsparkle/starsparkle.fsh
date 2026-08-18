#version 410 core

in vec2 vUV;
out vec4 fragColor;

layout(std140) uniform SparkleData {
    vec4 iBackgroundColor;
    vec4 iStarColor;
    vec4 iParams;
};

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float star(vec2 uv, float flare) {
    float d = length(uv);
    float m = 0.03 / max(d, 0.0001);
    float rays = max(0.0, 1.0 - abs(uv.x * uv.y * 1500.0));
    m += rays * flare;
    m *= smoothstep(1.1, 0.0, d);
    return m;
}

float sparkleLayer(vec2 uv, float t, vec2 offset) {
    vec2 gv = fract(uv) - 0.5;
    vec2 id = floor(uv) + offset;
    float col = 0.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offs = vec2(float(x), float(y));
            vec2 cell = id + offs;
            float rnd = hash21(cell);
            vec2 p = offs + vec2(rnd, hash21(cell + 34.2)) - 0.5;
            float blink = sin(t * 2.5 + rnd * 6.2831) * 0.5 + 0.5;
            blink = mix(0.1, 1.2, blink) * smoothstep(0.2, 0.8, rnd);
            float flareSize = mix(0.35, 2.2, fract(rnd * 345.32));
            float s = star(gv - p, flareSize);
            col += s * blink;
        }
    }

    return col;
}

void main() {
    float time = iParams.x;
    float scale = iParams.y;

    vec2 uv = (vUV - 0.5) * scale * 2.0;
    float t = time * 0.7;
    vec2 drift = vec2(-t * 0.28, t * 0.28);
    vec2 flowUv = uv + drift;

    float layer1 = sparkleLayer(flowUv * 4.0, t, vec2(0.0));
    float layer2 = sparkleLayer(flowUv * 8.0, t * 1.2, vec2(12.3));
    float layer3 = sparkleLayer(flowUv * 14.0, t * 1.5, vec2(45.6));

    float sparkles = layer1 * 1.45 + layer2 * 0.8 + layer3 * 0.4;
    vec3 starCore = vec3(1.0, 0.85, 0.95);
    vec3 starGlow = iStarColor.rgb;

    vec3 col = iBackgroundColor.rgb;
    col += mix(starGlow, starCore, clamp(sparkles, 0.0, 1.0)) * sparkles;
    col = 1.0 - exp(-col * 1.3);

    float sparkleAlpha = clamp(sparkles * iStarColor.a, 0.0, 1.0);
    float alpha = max(iBackgroundColor.a, sparkleAlpha);
    fragColor = vec4(col, alpha);
}
