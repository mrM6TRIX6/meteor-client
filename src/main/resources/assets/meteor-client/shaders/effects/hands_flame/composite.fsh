#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D BeforeSampler;
uniform sampler2D TrailSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform HandsFlameData {
    vec4 flameColor;
    vec4 params0;
    vec4 params1;
    vec4 screen;
};

void main() {
    vec2 px = screen.zw;
    float brightness = params1.x;

    vec2 blurPx = px * 2.65;
    vec2 widePx = px * 5.85;
    vec4 trail = texture(TrailSampler, texCoord) * 0.36;
    trail += texture(TrailSampler, clamp(texCoord + vec2(blurPx.x, 0.0), vec2(0.0), vec2(1.0))) * 0.105;
    trail += texture(TrailSampler, clamp(texCoord - vec2(blurPx.x, 0.0), vec2(0.0), vec2(1.0))) * 0.105;
    trail += texture(TrailSampler, clamp(texCoord + vec2(0.0, blurPx.y), vec2(0.0), vec2(1.0))) * 0.105;
    trail += texture(TrailSampler, clamp(texCoord - vec2(0.0, blurPx.y), vec2(0.0), vec2(1.0))) * 0.105;
    trail += texture(TrailSampler, clamp(texCoord + blurPx, vec2(0.0), vec2(1.0))) * 0.0475;
    trail += texture(TrailSampler, clamp(texCoord - blurPx, vec2(0.0), vec2(1.0))) * 0.0475;
    trail += texture(TrailSampler, clamp(texCoord + vec2(blurPx.x, -blurPx.y), vec2(0.0), vec2(1.0))) * 0.0475;
    trail += texture(TrailSampler, clamp(texCoord + vec2(-blurPx.x, blurPx.y), vec2(0.0), vec2(1.0))) * 0.0475;
    trail += texture(TrailSampler, clamp(texCoord + vec2(widePx.x, 0.0), vec2(0.0), vec2(1.0))) * 0.0325;
    trail += texture(TrailSampler, clamp(texCoord - vec2(widePx.x, 0.0), vec2(0.0), vec2(1.0))) * 0.0325;
    trail += texture(TrailSampler, clamp(texCoord + vec2(0.0, widePx.y), vec2(0.0), vec2(1.0))) * 0.0325;
    trail += texture(TrailSampler, clamp(texCoord - vec2(0.0, widePx.y), vec2(0.0), vec2(1.0))) * 0.0325;

    // Read both scene states
    vec4 scene = texture(SceneSampler, texCoord);
    vec4 sceneBefore = texture(BeforeSampler, texCoord);

    // Detect item pixels via depth (primary) and before/after difference (fallback/edges)
    float depth = texture(DepthSampler, texCoord).r;
    float depthMask = depth < 0.9999 ? 1.0 : 0.0;

    vec3 delta = abs(scene.rgb - sceneBefore.rgb);
    float peak = max(max(delta.r, delta.g), delta.b);
    float luma = dot(delta, vec3(0.299, 0.587, 0.114));
    float maskValue = peak * 0.78 + luma * 0.88 + abs(scene.a - sceneBefore.a);
    float colorMask = smoothstep(0.004, 0.060, maskValue);

    float mask = max(depthMask, colorMask);
    float itemCover = smoothstep(0.04, 0.40, mask);

    // Early out: no trail and no item; pass through scene untouched
    if (trail.a < 0.003 && itemCover < 0.01) {
        fragColor = vec4(scene.rgb, 1.0);
        return;
    }

    float behindItem = 1.0 - itemCover * 0.98;

    // Soft flame field: only in non-item areas
    float field = smoothstep(0.006, 0.40, trail.a) * behindItem;

    float sceneLuma = dot(scene.rgb, vec3(0.299, 0.587, 0.114));
    float darkCover = 1.0 - smoothstep(0.16, 0.72, sceneLuma);
    float brightnessCurve = 0.42 + (1.0 - exp(-clamp(brightness, 0.0, 2.0) * 0.8)) * 0.64;

    // Glow veil: how much flame covers the pixel
    float glowIntensity = pow(trail.a, 0.48) * field;
    float veil = clamp(glowIntensity * (0.20 + brightnessCurve * 0.22) * (1.0 + darkCover * 0.38), 0.0, 0.66);
    // Slight boost on item pixels to soften item texture behind glow
    veil += itemCover * smoothstep(0.05, 0.32, trail.a) * 0.10;

    // Flame fill color
    vec3 flameFill = min(trail.rgb * (0.78 + brightnessCurve * 0.42) + vec3(0.025), vec3(1.0));

    // Where flame covers background (non-item) pixels, blur the underlying scene
    // to prevent sharp block textures from showing through the flame.
    // On item pixels (itemCover ~1), use the crisp scene directly.
    float blurAmount = field * smoothstep(0.10, 0.52, trail.a);
    vec3 sceneBase;
    if (blurAmount > 0.01) {
        vec2 bgPx = px * (4.0 + blurAmount * 4.5);
        vec3 blurred = sceneBefore.rgb * 0.28;
        blurred += texture(BeforeSampler, clamp(texCoord + vec2(bgPx.x, 0.0), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(BeforeSampler, clamp(texCoord - vec2(bgPx.x, 0.0), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(BeforeSampler, clamp(texCoord + vec2(0.0, bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(BeforeSampler, clamp(texCoord - vec2(0.0, bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(BeforeSampler, clamp(texCoord + bgPx, vec2(0.0), vec2(1.0))).rgb * 0.07;
        blurred += texture(BeforeSampler, clamp(texCoord - bgPx, vec2(0.0), vec2(1.0))).rgb * 0.07;
        blurred += texture(BeforeSampler, clamp(texCoord + vec2(bgPx.x, -bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.05;
        blurred += texture(BeforeSampler, clamp(texCoord + vec2(-bgPx.x, bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.05;
        // Blend between crisp scene and blurred background based on flame coverage
        sceneBase = mix(scene.rgb, blurred, blurAmount);
    } else {
        sceneBase = scene.rgb;
    }

    // Darken scene slightly under the glow for contrast
    sceneBase *= (1.0 - veil * darkCover * 0.18);

    // Blend flame over scene
    vec3 covered = mix(sceneBase, flameFill, veil);

    // Additive soft glow
    float glowPulse = 0.94 + sin((texCoord.y + texCoord.x * 0.35) * 16.0 + params1.y * 3.6) * 0.035;
    vec3 glow = trail.rgb * pow(trail.a, 0.48) * field * glowPulse;

    // Hot core
    float coreIntensity = pow(trail.a * field, 2.15);
    vec3 core = min(trail.rgb * 1.22 + vec3(0.04), vec3(1.0)) * coreIntensity;

    vec3 color = covered
        + glow * (0.78 + brightnessCurve * 0.54)
        + core * (0.10 + brightnessCurve * 0.10);

    // Always fully opaque: fullscreen post-process replacement
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
