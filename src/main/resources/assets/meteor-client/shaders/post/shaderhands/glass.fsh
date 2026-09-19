#version 150

uniform sampler2D SceneTex;
uniform sampler2D HandTex;
uniform sampler2D BoundsTexL;
uniform sampler2D BoundsTexR;

layout(std140) uniform GlassConfig {
    vec4 Multiplier;
    vec4 GlassParams;
    vec4 GlassExtra;
};

vec2 handGradUV(vec2 p) {
    vec4 l = texture(BoundsTexL, vec2(0.5));
    vec4 r = texture(BoundsTexR, vec2(0.5));
    bool lok = l.z > l.x + 0.001 && l.w > l.y + 0.001;
    bool rok = r.z > r.x + 0.001 && r.w > r.y + 0.001;
    vec4 b;
    if (lok && rok) {
        if (r.x - l.z > 0.04) {
            float split = 0.5 * (l.z + r.x);
            b = (p.x < split) ? l : r;
        } else {
            b = vec4(min(l.xy, r.xy), max(l.zw, r.zw));
        }
    } else if (lok) {
        b = l;
    } else if (rok) {
        b = r;
    } else {
        return p;
    }
    return clamp((p - b.xy) / (b.zw - b.xy), vec2(0.0), vec2(1.0));
}

layout(std140) uniform PaletteParams {
    vec4 paletteMeta;
    vec4 paletteColors[6];
    vec4 paletteMeta2;
    vec4 paletteLayerA[6];
    vec4 paletteWaveA;
    vec4 paletteWave2;
    vec4 paletteLayerB[6];
    vec4 paletteWaveB;
    vec4 palettePadB;
    vec4 paletteLayerC[6];
    vec4 paletteWaveC;
    vec4 palettePadC;
    vec4 paletteLayerD[6];
    vec4 paletteWaveD;
    vec4 palettePadD;
    vec4 paletteLayerE[6];
    vec4 paletteWaveE;
    vec4 palettePadE;
    vec4 paletteLayerF[6];
    vec4 paletteWaveF;
    vec4 palettePadF;
};

in vec2 texCoord;
out vec4 fragColor;

float kLayerA = 0.0;
float kLayerB = 0.0;
float kLayerC = 0.0;
float kLayerD = 0.0;
float kLayerE = 0.0;
float kLayerF = 0.0;

float themeLayerCoverage(vec4 wave, vec2 fragXY) {
    if (wave.w < 0.5) {
        return 0.0;
    }
    if (wave.w > 1.5) {
        return clamp(wave.z, 0.0, 1.0);
    }
    vec2 res = max(paletteWave2.xy, vec2(1.0));
    float aspect = res.x / res.y;
    vec2 d = (fragXY / res - wave.xy) * vec2(aspect, 1.0);
    float wf = max(paletteWave2.z, 0.0005);
    return 1.0 - smoothstep(wave.z - wf, wave.z + wf, length(d));
}

void themeWaveMix(vec2 fragXY) {
    kLayerA = themeLayerCoverage(paletteWaveA, fragXY);
    kLayerB = themeLayerCoverage(paletteWaveB, fragXY);
    kLayerC = themeLayerCoverage(paletteWaveC, fragXY);
    kLayerD = themeLayerCoverage(paletteWaveD, fragXY);
    kLayerE = themeLayerCoverage(paletteWaveE, fragXY);
    kLayerF = themeLayerCoverage(paletteWaveF, fragXY);
}

vec3 themeBlend(vec3 base, vec3 a, vec3 b, vec3 c, vec3 d, vec3 e, vec3 f) {
    vec3 r = mix(base, a, kLayerA);
    r = mix(r, b, kLayerB);
    r = mix(r, c, kLayerC);
    r = mix(r, d, kLayerD);
    r = mix(r, e, kLayerE);
    return mix(r, f, kLayerF);
}

vec3 paletteColorAt(int idx) {
    if (idx <= 0) return themeBlend(paletteColors[0].rgb, paletteLayerA[0].rgb, paletteLayerB[0].rgb, paletteLayerC[0].rgb, paletteLayerD[0].rgb, paletteLayerE[0].rgb, paletteLayerF[0].rgb);
    if (idx == 1) return themeBlend(paletteColors[1].rgb, paletteLayerA[1].rgb, paletteLayerB[1].rgb, paletteLayerC[1].rgb, paletteLayerD[1].rgb, paletteLayerE[1].rgb, paletteLayerF[1].rgb);
    if (idx == 2) return themeBlend(paletteColors[2].rgb, paletteLayerA[2].rgb, paletteLayerB[2].rgb, paletteLayerC[2].rgb, paletteLayerD[2].rgb, paletteLayerE[2].rgb, paletteLayerF[2].rgb);
    if (idx == 3) return themeBlend(paletteColors[3].rgb, paletteLayerA[3].rgb, paletteLayerB[3].rgb, paletteLayerC[3].rgb, paletteLayerD[3].rgb, paletteLayerE[3].rgb, paletteLayerF[3].rgb);
    if (idx == 4) return themeBlend(paletteColors[4].rgb, paletteLayerA[4].rgb, paletteLayerB[4].rgb, paletteLayerC[4].rgb, paletteLayerD[4].rgb, paletteLayerE[4].rgb, paletteLayerF[4].rgb);
    return themeBlend(paletteColors[5].rgb, paletteLayerA[5].rgb, paletteLayerB[5].rgb, paletteLayerC[5].rgb, paletteLayerD[5].rgb, paletteLayerE[5].rgb, paletteLayerF[5].rgb);
}

vec3 paletteRamp(float t) {
    int count = int(paletteMeta.x + 0.5);
    if (count <= 1) return paletteColorAt(0);
    float f = clamp(t, 0.0, 1.0) * float(count - 1);
    int i = clamp(int(floor(f)), 0, count - 1);
    int j = min(i + 1, count - 1);
    float frac = clamp(f - float(i), 0.0, 1.0);
    frac = frac * frac * (3.0 - 2.0 * frac);
    return mix(paletteColorAt(i), paletteColorAt(j), frac);
}

vec3 paletteLoop(float t) {
    int count = int(paletteMeta.x + 0.5);
    if (count <= 1) return paletteColorAt(0);
    float f = fract(t) * float(count);
    int i1 = clamp(int(floor(f)), 0, count - 1);
    float u = clamp(f - float(i1), 0.0, 1.0);
    int i0 = i1 - 1; if (i0 < 0) i0 += count;
    int i2 = i1 + 1; if (i2 >= count) i2 -= count;
    int i3 = i1 + 2; if (i3 >= count) i3 -= count;
    vec3 c0 = paletteColorAt(i0);
    vec3 c1 = paletteColorAt(i1);
    vec3 c2 = paletteColorAt(i2);
    vec3 c3 = paletteColorAt(i3);
    float u2 = u * u;
    float u3 = u2 * u;
    vec3 cyclicCol = 0.5 * ((2.0 * c1)
                      + (-c0 + c2) * u
                      + (2.0 * c0 - 5.0 * c1 + 4.0 * c2 - c3) * u2
                      + (-c0 + 3.0 * c1 - 3.0 * c2 + c3) * u3);
    float tri = 0.5 - 0.5 * cos(6.2831853 * fract(t));
    vec3 mirrorCol = paletteRamp(tri);
    float closed = clamp(paletteMeta2.y, 0.0, 1.0);
    return clamp(mix(mirrorCol, cyclicCol, closed), 0.0, 1.0);
}

vec3 boxGradient(vec2 uv, float phase) {
    vec3 tl = paletteLoop(phase);
    vec3 tr = paletteLoop(phase + 0.25);
    vec3 br = paletteLoop(phase + 0.5);
    vec3 bl = paletteLoop(phase + 0.75);
    vec3 top = mix(tl, tr, uv.x);
    vec3 bot = mix(bl, br, uv.x);
    return mix(top, bot, uv.y);
}

vec3 meshGradient(vec2 uv, float phase, float aspect) {
    float a = 6.2831853 * phase;
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float kx = mod(fi, 2.0) < 0.5 ? 1.0 : 2.0;
        float ky = mod(fi, 2.0) < 0.5 ? 2.0 : 1.0;
        vec2 pos = vec2(0.5) + 0.34 * vec2(sin(a * kx + fi * 2.3999), cos(a * ky - fi * 1.618));
        vec2 d = (uv - pos) * vec2(aspect, 1.0);
        float w = 1.0 / (dot(d, d) * 5.0 + 0.06);
        sum += paletteRamp(fi / 5.0) * w;
        wsum += w;
    }
    return sum / wsum;
}

vec3 selectStyle(float id, vec3 ramp, vec3 mesh, vec3 box) {
    int s = int(id + 0.5);
    if (s == 1) return mesh;
    if (s == 2) return box;
    return ramp;
}

const float REFLECT_SCALE = 22.0;

vec4 sn_permute(vec4 x) { return mod(((x * 34.0) + 1.0) * x, 289.0); }
vec4 sn_taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
    vec3 i  = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);
    vec3 x1 = x0 - i1 + 1.0 * C.xxx;
    vec3 x2 = x0 - i2 + 2.0 * C.xxx;
    vec3 x3 = x0 - 1.0 + 3.0 * C.xxx;
    i = mod(i, 289.0);
    vec4 p = sn_permute(sn_permute(sn_permute(
        i.z + vec4(0.0, i1.z, i2.z, 1.0))
        + i.y + vec4(0.0, i1.y, i2.y, 1.0))
        + i.x + vec4(0.0, i1.x, i2.x, 1.0));
    float n_ = 1.0 / 7.0;
    vec3 ns = n_ * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);
    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);
    vec4 norm = sn_taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

void main() {
    themeWaveMix(gl_FragCoord.xy);

    float alpha = texture(HandTex, texCoord).a;
    if (alpha <= 0.001) {
        discard;
    }

    vec3 tint;
    if (int(paletteMeta.x + 0.5) >= 2) {

        vec2 g = handGradUV(texCoord);
        vec2 uv = vec2(g.x, 1.0 - g.y);
        float tri = abs(fract(uv.x + fract(paletteMeta.y)) * 2.0 - 1.0);
        vec3 rampCol = paletteRamp(smoothstep(0.0, 1.0, tri)) * 0.86;
        vec3 meshCol = meshGradient(uv, paletteMeta.y, 1.0);
        vec3 boxCol = boxGradient(uv, paletteMeta.y * 20.0);
        vec3 targetCol = selectStyle(paletteMeta.z, rampCol, meshCol, boxCol);

        float sweep = paletteMeta.w;
        if (sweep >= 0.0) {
            vec3 prevCol = selectStyle(paletteMeta2.x, rampCol, meshCol, boxCol);
            float axis = uv.x + (uv.y - 0.5) * 0.30;
            float head = sweep * 1.5 - 0.25;
            float wipe = 1.0 - smoothstep(head - 0.04, head + 0.04, axis);
            tint = mix(prevCol, targetCol, wipe);
        } else {
            tint = targetCol;
        }
    } else {
        tint = Multiplier.rgb;
    }

    vec2 ruv = vec2(texCoord.x, mix(texCoord.y, 1.0 - texCoord.y, GlassExtra.x));
    float noise = snoise(vec3(ruv * REFLECT_SCALE, 1.0));
    vec2 noisyUV = clamp(ruv + vec2(noise * GlassParams.z), 0.0, 1.0);
    vec3 refl = texture(SceneTex, noisyUV).rgb;

    float luma = dot(refl, vec3(0.299, 0.587, 0.114));
    refl = max(mix(vec3(luma), refl, GlassParams.x), 0.0);

    vec3 lit = mix(refl, vec3(1.0), clamp(luma, 0.0, 1.0) * GlassParams.y);
    vec3 col = mix(lit, lit * tint, GlassParams.w);

    fragColor = vec4(col, Multiplier.a) * alpha;
}
