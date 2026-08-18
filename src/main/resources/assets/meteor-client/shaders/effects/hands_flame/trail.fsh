#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D BeforeSampler;
uniform sampler2D AfterSampler;
uniform sampler2D PrevTrailSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform HandsFlameData {
    vec4 flameColor;
    vec4 params0;
    vec4 params1;
    vec4 screen;
};

const vec2 dirs[8] = vec2[](
    vec2( 1.000,  0.000), vec2( 0.707,  0.707),
    vec2( 0.000,  1.000), vec2(-0.707,  0.707),
    vec2(-1.000,  0.000), vec2(-0.707, -0.707),
    vec2( 0.000, -1.000), vec2( 0.707, -0.707)
);

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x),
        mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amplitude;
        p = p * 2.03 + vec2(17.13, 9.27);
        amplitude *= 0.5;
    }
    return value;
}

float rawHandMaskColor(vec2 uv, float itemOnly, out vec3 outColor) {
    vec4 beforeColor = texture(BeforeSampler, uv);
    vec4 afterColor = texture(AfterSampler, uv);
    outColor = afterColor.rgb;

    // The depth buffer is cleared before the hand renders, so depth < 1.0 is the hand.
    float depth = texture(DepthSampler, uv).r;
    float depthMask = depth < 0.9999 ? 1.0 : 0.0;

    vec3 delta = abs(afterColor.rgb - beforeColor.rgb);
    float peak = max(max(delta.r, delta.g), delta.b);
    float luma = dot(delta, vec3(0.299, 0.587, 0.114));
    float value = peak * 0.78 + luma * 0.88 + abs(afterColor.a - beforeColor.a);
    float colorMask = smoothstep(0.004, 0.060, value);
    float itemMask = smoothstep(0.035, 0.135, value);

    return mix(max(depthMask, colorMask), itemMask, itemOnly);
}

float depthMaskAt(vec2 uv) {
    return texture(DepthSampler, uv).r < 0.9999 ? 1.0 : 0.0;
}

float nearbyDepthMask(vec2 uv, vec2 px, float radiusPx) {
    float mask = depthMaskAt(uv);
    for (int i = 0; i < 8; i++) {
        vec2 probeUv = clamp(uv + dirs[i] * radiusPx * 0.72 * px, vec2(0.0), vec2(1.0));
        mask = max(mask, depthMaskAt(probeUv));
    }
    return mask;
}

void computeFlameField(vec2 uv, vec2 px, float radiusPx, float prevAlpha, vec3 prevColor, float itemOnly,
                       out float envelope, out float currentMask, out vec3 itemColor) {
    float maskSum = 0.0;
    float maskWSum = 0.0;
    vec3 colorSum = vec3(0.0);
    float colorWSum = 0.0;

    // Center
    vec3 cc;
    float cm = rawHandMaskColor(uv, itemOnly, cc);
    maskSum += cm * 1.70;
    maskWSum += 1.70;
    float sat = max(max(cc.r, cc.g), cc.b) - min(min(cc.r, cc.g), cc.b);
    float lum = dot(cc, vec3(0.299, 0.587, 0.114));
    float cw = cm * (0.3 + sat * 1.5 + lum * 0.3) * 1.70;
    colorSum += cc * cw;
    colorWSum += cw;

    for (int i = 0; i < 8; i++) {
        vec2 dir = dirs[i];

        vec2 nearUv = clamp(uv + dir * radiusPx * 0.45 * px, vec2(0.0), vec2(1.0));
        vec3 nearColor;
        float nearMask = rawHandMaskColor(nearUv, itemOnly, nearColor);
        maskSum += nearMask * 0.70;
        maskWSum += 0.70;
        float nearSat = max(max(nearColor.r, nearColor.g), nearColor.b) - min(min(nearColor.r, nearColor.g), nearColor.b);
        float nearLum = dot(nearColor, vec3(0.299, 0.587, 0.114));
        float nearWeight = nearMask * 0.70 * (0.3 + nearSat * 1.5 + nearLum * 0.3);
        colorSum += nearColor * nearWeight;
        colorWSum += nearWeight;

        vec2 farUv = clamp(uv + dir * radiusPx * px, vec2(0.0), vec2(1.0));
        vec3 farColor;
        float farMask = rawHandMaskColor(farUv, itemOnly, farColor);
        maskSum += farMask * 0.30;
        maskWSum += 0.30;
        float farSat = max(max(farColor.r, farColor.g), farColor.b) - min(min(farColor.r, farColor.g), farColor.b);
        float farLum = dot(farColor, vec3(0.299, 0.587, 0.114));
        float farWeight = farMask * 0.30 * (0.3 + farSat * 1.5 + farLum * 0.3);
        colorSum += farColor * farWeight;
        colorWSum += farWeight;
    }

    float blurredMask = smoothstep(0.020, 0.68, maskSum / max(maskWSum, 0.001));
    currentMask = blurredMask;
    envelope = max(blurredMask, prevAlpha * 0.62);
    itemColor = colorWSum > 0.001 ? colorSum / colorWSum : (prevAlpha > 0.01 ? prevColor : flameColor.rgb);
}

void main() {
    float strength = params0.x;
    float riseSpeed = params0.y;
    float wobble = params0.z;
    float flameLength = params0.w;
    float brightness = params1.x;
    float time = params1.y;
    float packedColorMode = params1.z;
    float itemOnly = step(10.0, packedColorMode);
    float colorMode = packedColorMode - itemOnly * 10.0;
    float colorAlpha = params1.w;
    vec2 px = screen.zw;
    float lengthCurve = clamp(flameLength / 2.5, 0.0, 1.0);
    float radiusPx = mix(22.0, 62.0, lengthCurve);

    // Advect previous trail first; needed for envelope extension and early out
    float rise = 0.05 + smoothstep(0.0, 2.0, riseSpeed) * 0.80;
    float curl = noise(texCoord * vec2(8.0, 6.0) + vec2(time * 0.20, time * 0.11)) - 0.5;
    vec2 drift = vec2(
        sin(texCoord.y * 18.0 + time * 2.35) * px.x * wobble * 2.6 + curl * px.x * (2.0 + wobble * 3.2),
        -px.y * rise
    );
    vec2 histUv = clamp(texCoord + drift, vec2(0.0), vec2(1.0));

    vec2 softPx = px * (2.1 + wobble * 0.65);
    vec4 previous = texture(PrevTrailSampler, histUv) * 0.34;
    previous += texture(PrevTrailSampler, clamp(histUv + vec2(softPx.x, 0.0), vec2(0.0), vec2(1.0))) * 0.105;
    previous += texture(PrevTrailSampler, clamp(histUv - vec2(softPx.x, 0.0), vec2(0.0), vec2(1.0))) * 0.105;
    previous += texture(PrevTrailSampler, clamp(histUv + vec2(0.0, softPx.y), vec2(0.0), vec2(1.0))) * 0.105;
    previous += texture(PrevTrailSampler, clamp(histUv - vec2(0.0, softPx.y), vec2(0.0), vec2(1.0))) * 0.105;
    previous += texture(PrevTrailSampler, clamp(histUv + softPx, vec2(0.0), vec2(1.0))) * 0.045;
    previous += texture(PrevTrailSampler, clamp(histUv - softPx, vec2(0.0), vec2(1.0))) * 0.045;
    previous += texture(PrevTrailSampler, clamp(histUv + vec2(softPx.x, -softPx.y), vec2(0.0), vec2(1.0))) * 0.045;
    previous += texture(PrevTrailSampler, clamp(histUv + vec2(-softPx.x, softPx.y), vec2(0.0), vec2(1.0))) * 0.045;

    vec3 quickColor;
    float quickMask = rawHandMaskColor(texCoord, itemOnly, quickColor);
    if (quickMask < 0.01 && previous.a < 0.0015 && nearbyDepthMask(texCoord, px, radiusPx) < 0.5) {
        fragColor = vec4(0.0);
        return;
    }

    // Smooth envelope + color from a compact radial mask.
    float envelope;
    float currentMask;
    vec3 itemColor;
    computeFlameField(texCoord, px, radiusPx, previous.a, previous.rgb, itemOnly, envelope, currentMask, itemColor);

    // Fire turbulence
    float flow = time * (0.08 + riseSpeed * 0.50);
    float lateralWobble = sin(texCoord.y * 28.0 + time * (1.5 + wobble * 2.5)) * wobble * 0.22;
    vec2 flameUv = vec2(texCoord.x + lateralWobble, texCoord.y - flow);

    float n1 = fbm(vec2(flameUv.x * 4.4, flameUv.y * (5.2 + flameLength * 2.7)));
    float n2 = fbm(vec2(flameUv.x * 8.2 + 3.7, flameUv.y * 9.5 - time * 0.35));
    float fireNoise = n1 * 0.72 + n2 * 0.28;

    float solidZone = smoothstep(0.15, 0.55, envelope);
    float flameShape = mix(fireNoise, 1.0, solidZone * 0.78);
    flameShape = smoothstep(0.18, 0.78, flameShape);

    float source = currentMask * mix(0.64, 1.35, flameShape);
    source = clamp(source, 0.0, 1.0);

    float strengthCurve = 1.0 - exp(-clamp(strength, 0.0, 2.0) * 0.95);
    float brightnessCurve = 0.45 + (1.0 - exp(-clamp(brightness, 0.0, 2.0) * 0.85)) * 0.70;
    source *= strengthCurve * brightnessCurve * 1.22 * colorAlpha;
    source = clamp(source, 0.0, 0.92);

    // Color
    vec3 baseColor;
    if (colorMode < 0.5) {
        baseColor = itemColor;
    } else {
        baseColor = flameColor.rgb;
        // Normalize luminance so client/custom colors don't glow brighter than item mode
        float luma = dot(baseColor, vec3(0.299, 0.587, 0.114));
        if (luma > 0.45) {
            baseColor *= 0.45 / luma;
        }
    }
    float distFromItem = 1.0 - clamp(envelope * 1.5, 0.0, 1.0);
    vec3 coreColor = min(baseColor * 1.4 + vec3(0.06), vec3(1.0));
    vec3 midColor = baseColor * 0.85;
    vec3 tipColor = baseColor * 0.35;
    vec3 fireColor = mix(coreColor, midColor, smoothstep(0.0, 0.35, distFromItem));
    fireColor = mix(fireColor, tipColor, smoothstep(0.25, 0.9, distFromItem));
    fireColor *= 0.92 + 0.08 * sin(time * 7.3 + texCoord.x * 20.0 + texCoord.y * 15.0);

    // Blend with history
    float historyFade = mix(0.930, 0.950, lengthCurve);
    float blendFactor = clamp(source * 0.56 + 0.10, 0.08, 0.66);
    float alpha = clamp(mix(previous.a * historyFade, source, blendFactor), 0.0, 0.95);
    alpha = max(alpha, previous.a * historyFade);

    float colorBlend = blendFactor * smoothstep(0.015, 0.22, currentMask);
    vec3 color = alpha > 0.001 ? mix(previous.rgb, fireColor, colorBlend) : vec3(0.0);
    fragColor = vec4(color, alpha);
}
