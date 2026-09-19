#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D BeforeSampler;
uniform sampler2D AfterSampler;
uniform sampler2D TrailSampler;
uniform sampler2D DepthSampler;
uniform sampler2D NoHandDepthSampler;

layout(std140) uniform HandsFlameData {
    vec4 flameColor;
    vec4 params0;
    vec4 params1;
    vec4 screen;
    vec4 gradColor1;
    vec4 gradColor2;
    vec4 gradColor3;
    vec4 gradColor4;
    vec4 flameExtra;
};

float quickTrailAlpha(vec2 uv, vec2 blurPx) {
    return texture(TrailSampler, uv).a;
}

vec4 blurredTrailColor(vec2 uv, vec2 blurPx) {
    vec4 trail = texture(TrailSampler, uv) * 0.36;
    trail += texture(TrailSampler, clamp(uv + vec2(blurPx.x, 0.0), vec2(0.0), vec2(1.0))) * 0.11;
    trail += texture(TrailSampler, clamp(uv - vec2(blurPx.x, 0.0), vec2(0.0), vec2(1.0))) * 0.11;
    trail += texture(TrailSampler, clamp(uv + vec2(0.0, blurPx.y), vec2(0.0), vec2(1.0))) * 0.11;
    trail += texture(TrailSampler, clamp(uv - vec2(0.0, blurPx.y), vec2(0.0), vec2(1.0))) * 0.11;
    trail += texture(TrailSampler, clamp(uv + blurPx, vec2(0.0), vec2(1.0))) * 0.05;
    trail += texture(TrailSampler, clamp(uv - blurPx, vec2(0.0), vec2(1.0))) * 0.05;
    trail += texture(TrailSampler, clamp(uv + vec2(blurPx.x, -blurPx.y), vec2(0.0), vec2(1.0))) * 0.05;
    trail += texture(TrailSampler, clamp(uv + vec2(-blurPx.x, blurPx.y), vec2(0.0), vec2(1.0))) * 0.05;
    return trail;
}

float handDepthMaskAt(vec2 uv, float irisDepthMode) {
    float depth = texture(DepthSampler, uv).r;
    if (irisDepthMode > 0.5) {
        float noHandDepth = texture(NoHandDepthSampler, uv).r;
        if (depth < 0.9999 && (noHandDepth >= 0.9999 || depth < noHandDepth - 0.00002)) {
            return 1.0;
        }
    } else {
        if (depth < 0.9999) {
            return 1.0;
        }
    }
    vec4 beforeColor = texture(BeforeSampler, uv);
    vec4 afterColor = texture(AfterSampler, uv);
    vec3 delta = abs(afterColor.rgb - beforeColor.rgb);
    float peak = max(max(delta.r, delta.g), delta.b);
    float luma = dot(delta, vec3(0.299, 0.587, 0.114));
    float value = peak * 0.78 + luma * 0.88 + abs(afterColor.a - beforeColor.a);
    return smoothstep(0.004, 0.060, value);
}

void main() {
    vec2 px = screen.zw;
    float brightness = params1.x;
    float packedColorMode = params1.z;
    float irisDepthMode = step(20.0, packedColorMode);

    vec2 blurPx = px * 2.65;
    vec2 widePx = px * 5.85;

    float trailHint = quickTrailAlpha(texCoord, blurPx);
    if (trailHint < 0.0010) {
        fragColor = texture(SceneSampler, texCoord);
        return;
    }

    vec4 scene = texture(SceneSampler, texCoord);
    vec4 sceneBefore = texture(BeforeSampler, texCoord);
    vec4 afterColor = texture(AfterSampler, texCoord);

    float mask = 0.0;
    if (irisDepthMode > 0.5) {
        float depthMask = handDepthMaskAt(texCoord, irisDepthMode);
        if (depthMask > 0.5) {
            mask = 1.0;
        } else {
            float depth = texture(DepthSampler, texCoord).r;
            float noHandDepth = texture(NoHandDepthSampler, texCoord).r;
            if (depth >= 0.9999 || noHandDepth < 0.0001 || depth >= noHandDepth - 0.00002) {
                mask = 0.0;
            } else {
                vec3 delta = abs(afterColor.rgb - sceneBefore.rgb);
                float peak = max(max(delta.r, delta.g), delta.b);
                float luma = dot(delta, vec3(0.299, 0.587, 0.114));
                float maskValue = peak * 0.78 + luma * 0.88 + abs(afterColor.a - sceneBefore.a);
                mask = smoothstep(0.004, 0.060, maskValue);
            }
        }
    } else {
        float depth = texture(DepthSampler, texCoord).r;
        if (depth < 0.9999) {
            mask = 1.0;
        } else {
            vec3 delta = abs(afterColor.rgb - sceneBefore.rgb);
            float peak = max(max(delta.r, delta.g), delta.b);
            float luma = dot(delta, vec3(0.299, 0.587, 0.114));
            float maskValue = peak * 0.78 + luma * 0.88 + abs(afterColor.a - sceneBefore.a);
            mask = smoothstep(0.004, 0.060, maskValue);
        }
    }
    float itemCover = smoothstep(0.04, 0.40, mask);
    vec4 trail = blurredTrailColor(texCoord, blurPx);

    float behindItem = 1.0 - itemCover;

    float field = smoothstep(0.006, 0.40, trail.a) * behindItem;

    float sceneLuma = dot(scene.rgb, vec3(0.299, 0.587, 0.114));
    float darkCover = 1.0 - smoothstep(0.16, 0.72, sceneLuma);
    float brightnessCurve = 0.42 + (1.0 - exp(-clamp(brightness, 0.0, 2.0) * 0.8)) * 0.64;

    float glowIntensity = pow(trail.a, 0.48) * field;
    float veil = clamp(glowIntensity * (0.20 + brightnessCurve * 0.22) * (1.0 + darkCover * 0.38), 0.0, 0.66);

    vec3 flameFill = min(trail.rgb * (0.78 + brightnessCurve * 0.42) + vec3(0.025), vec3(1.0));

    float blurAmount = field * smoothstep(0.10, 0.52, trail.a);
    vec3 sceneBase;
    if (blurAmount > 0.01) {
        vec2 bgPx = px * (4.0 + blurAmount * 4.5);
        vec3 blurred = scene.rgb * 0.28;
        blurred += texture(SceneSampler, clamp(texCoord + vec2(bgPx.x, 0.0), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(SceneSampler, clamp(texCoord - vec2(bgPx.x, 0.0), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(SceneSampler, clamp(texCoord + vec2(0.0, bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(SceneSampler, clamp(texCoord - vec2(0.0, bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(SceneSampler, clamp(texCoord + bgPx, vec2(0.0), vec2(1.0))).rgb * 0.07;
        blurred += texture(SceneSampler, clamp(texCoord - bgPx, vec2(0.0), vec2(1.0))).rgb * 0.07;
        blurred += texture(SceneSampler, clamp(texCoord + vec2(bgPx.x, -bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.05;
        blurred += texture(SceneSampler, clamp(texCoord + vec2(-bgPx.x, bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.05;

        sceneBase = mix(scene.rgb, blurred, blurAmount);
    } else {
        sceneBase = scene.rgb;
    }

    sceneBase *= (1.0 - veil * darkCover * 0.18);

    vec3 covered = mix(sceneBase, flameFill, veil);

    float glowPulse = 0.94 + sin((texCoord.y + texCoord.x * 0.35) * 16.0 + params1.y * 3.6) * 0.035;
    vec3 glow = trail.rgb * pow(trail.a, 0.48) * field * glowPulse;

    float coreIntensity = pow(trail.a * field, 2.15);
    vec3 core = min(trail.rgb * 1.22 + vec3(0.04), vec3(1.0)) * coreIntensity;

    vec3 color = covered
        + glow * (0.78 + brightnessCurve * 0.54)
        + core * (0.10 + brightnessCurve * 0.10);

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
