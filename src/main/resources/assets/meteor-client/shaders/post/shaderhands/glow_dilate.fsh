#version 150

uniform sampler2D MaskTex;
uniform sampler2D BoundsTexL;
uniform sampler2D BoundsTexR;

layout(std140) uniform GlowConfig {
    vec2 Direction;
    vec2 TexelSize;
    float Radius;
    float GradientBlend;
    vec2 GradientCenter;
    vec4 GradColor1;
    vec4 GradColor2;
    vec4 GradColor3;
    vec4 GradColor4;
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
    float alpha = clamp(texture(MaskTex, texCoord).a, 0.0, 1.0);
    if (alpha <= 0.0001) {
        discard;
    }

    vec4 gradient = gradientColor(handGradUV(texCoord));
    fragColor = vec4(gradient.rgb, alpha * gradient.a);
}
