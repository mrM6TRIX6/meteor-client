#version 150

uniform sampler2D TextureIn;
uniform sampler2D BoundsTexL;
uniform sampler2D BoundsTexR;

layout(std140) uniform GaussConfig {
    vec2 Direction;
    vec2 TexelSize;
    vec4 GaussSupport;
    vec2 GradientCenter;
    float GradientBlend;
    float LinearSampling;
    vec4 GradColor1;
    vec4 GradColor2;
    vec4 GradColor3;
    vec4 GradColor4;
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
    vec4 top = mix(GradColor1, GradColor2, x);
    vec4 bottom = mix(GradColor4, GradColor3, x);
    return mix(top, bottom, y);
}

void main() {
    vec2 dir = Direction * TexelSize;
    int support = int(GaussSupport.w + 0.5);
    bool linearSampling = LinearSampling > 0.5;

    vec3 gaussian = GaussSupport.xyz;
    vec4 result = texture(TextureIn, texCoord) * gaussian.x;
    float sum = gaussian.x;

    if (linearSampling) {
        for (int i = 1; i <= 128; i += 2) {
            if (i > support) break;
            gaussian.xy *= gaussian.yz;
            float w1 = gaussian.x;
            gaussian.xy *= gaussian.yz;
            float w2 = gaussian.x;
            float w = w1 + w2;
            vec2 offset = dir * ((float(i) * w1 + float(i + 1) * w2) / max(w, 1e-4));
            result += texture(TextureIn, texCoord + offset) * w;
            result += texture(TextureIn, texCoord - offset) * w;
            sum += w * 2.0;
        }
    } else {
        for (int i = 1; i <= 128; i++) {
            if (i > support) break;
            gaussian.xy *= gaussian.yz;
            vec2 offset = dir * float(i);
            result += texture(TextureIn, texCoord + offset) * gaussian.x;
            result += texture(TextureIn, texCoord - offset) * gaussian.x;
            sum += gaussian.x * 2.0;
        }
    }

    result /= max(sum, 1e-4);

    vec4 gradient = gradientColor(handGradUV(texCoord));
    fragColor = vec4(gradient.rgb, clamp(result.a * gradient.a, 0.0, 1.0));
}
