#version 150

uniform sampler2D BaseMaskTex;
uniform sampler2D OutlineMaskTex;
uniform sampler2D GlowTex;
uniform sampler2D BoundsTexL;
uniform sampler2D BoundsTexR;

layout(std140) uniform OutlineConfig {
    vec2 TexelSize;
    float OutlineWidth;
    float GlowStrength;
    vec2 GradientCenter;
    float GradientBlend;
    float GlowRadius;
    vec4 OutlineGradColor1;
    vec4 OutlineGradColor2;
    vec4 OutlineGradColor3;
    vec4 OutlineGradColor4;
    vec4 ThemeMix;
    vec4 ThemeFlags;
};

in vec2 texCoord;
out vec4 fragColor;

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

vec4 gradientColor(vec2 pos) {
    float blend = max(GradientBlend, 0.05);
    float x = smoothstep(GradientCenter.x - blend, GradientCenter.x + blend, pos.x);
    float y = smoothstep(GradientCenter.y - blend, GradientCenter.y + blend, pos.y);
    vec4 top = mix(OutlineGradColor1, OutlineGradColor2, x);
    vec4 bottom = mix(OutlineGradColor4, OutlineGradColor3, x);
    return mix(top, bottom, y);
}

float innerEdge(float centerMask) {
    if (centerMask < 0.5) {
        return 0.0;
    }

    float n1 = step(0.05, texture(BaseMaskTex, texCoord + vec2(TexelSize.x, 0.0)).a);
    float n2 = step(0.05, texture(BaseMaskTex, texCoord - vec2(TexelSize.x, 0.0)).a);
    float n3 = step(0.05, texture(BaseMaskTex, texCoord + vec2(0.0, TexelSize.y)).a);
    float n4 = step(0.05, texture(BaseMaskTex, texCoord - vec2(0.0, TexelSize.y)).a);
    float crossMin = min(min(n1, n2), min(n3, n4));
    return clamp(centerMask - crossMin, 0.0, 1.0);
}

float outlineBand() {
    if (OutlineWidth <= 0.0) {
        return 0.0;
    }

    if (OutlineWidth < 1.0) {
        float c = step(0.05, texture(OutlineMaskTex, texCoord).a);
        float n1 = step(0.05, texture(OutlineMaskTex, texCoord + vec2(TexelSize.x, 0.0)).a);
        float n2 = step(0.05, texture(OutlineMaskTex, texCoord - vec2(TexelSize.x, 0.0)).a);
        float n3 = step(0.05, texture(OutlineMaskTex, texCoord + vec2(0.0, TexelSize.y)).a);
        float n4 = step(0.05, texture(OutlineMaskTex, texCoord - vec2(0.0, TexelSize.y)).a);
        float mx = max(max(c, n1), max(max(n2, n3), n4));
        float mn = min(min(c, n1), min(min(n2, n3), n4));
        return clamp((mx - mn) * OutlineWidth, 0.0, 1.0);
    }

    float radius = OutlineWidth;
    int iRadius = int(ceil(radius));
    float maxA = 0.0;
    float minA = 1.0;

    for (int x = -iRadius; x <= iRadius; x++) {
        for (int y = -iRadius; y <= iRadius; y++) {
            vec2 o = vec2(float(x), float(y));
            if (length(o) > radius + 0.25) continue;
            float a = step(0.05, texture(OutlineMaskTex, texCoord + o * TexelSize).a);
            maxA = max(maxA, a);
            minA = min(minA, a);
        }
    }

    return clamp(maxA - minA, 0.0, 1.0);
}

void main() {
    float maskAlpha = clamp(texture(BaseMaskTex, texCoord).a, 0.0, 1.0);
    vec4 glowSample = texture(GlowTex, texCoord);
    float blurAlpha = clamp(glowSample.a, 0.0, 1.0);

    float glowA = 0.0;

    if (GlowStrength > 0.0) {
        float outside = 1.0 - smoothstep(0.0, 1.0, maskAlpha);
        float exposure = 1.0 - exp(-blurAlpha * GlowStrength * 1.35);
        glowA = clamp(exposure * outside, 0.0, 1.0);
    }

    float outlineA = 0.0;
    vec3 outlineRgb = vec3(0.0);
    if (OutlineWidth > 0.0) {
        float oa = outlineBand();
        vec4 oc = gradientColor(handGradUV(texCoord));
        outlineA = clamp(oa * oc.a, 0.0, 1.0);
        outlineRgb = oc.rgb;
    }

    float outA = outlineA + glowA * (1.0 - outlineA);

    if (outA <= 0.0001) {
        discard;
    }

    vec3 rgb = (outlineRgb * outlineA + glowSample.rgb * glowA * (1.0 - outlineA)) / outA;
    fragColor = vec4(rgb, clamp(outA, 0.0, 1.0));
}
