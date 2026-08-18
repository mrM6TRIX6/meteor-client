#version 150

#moj_import <meteor-client:common.glsl>

in vec2 FragCoord;
flat in int QuadIndex;

layout(std140) uniform ZippyParamsArray {
    vec4 params[768];
};

out vec4 OutColor;

vec2 stanh(vec2 value) {
    return tanh(clamp(value, vec2(-40.0), vec2(40.0)));
}

void main() {
    int base = QuadIndex * 3;
    vec4 radius = params[base];
    vec4 sizeSmoothTime = params[base + 1];
    vec4 tint = params[base + 2];

    vec2 coord = clamp(FragCoord, vec2(0.0), vec2(1.0));
    vec2 size = max(sizeSmoothTime.xy, vec2(1.0));
    float shapeAlpha = ralpha(size, coord, max(radius, vec4(0.0)), sizeSmoothTime.z);

    if (shapeAlpha <= 0.001) {
        discard;
    }

    vec2 pixelPos = coord * size;
    vec2 v = size;
    vec2 u = 0.12 * (2.0 * pixelPos - v) / max(v.y, 1.0);
    vec4 o = vec4(1.0, 2.0, 3.0, 0.0);
    vec4 z = o;
    float t = sizeSmoothTime.w;

    for (float a = 0.5, i = 0.0; i < 18.0; i += 1.0) {
        a += 0.03;
        t += 1.0;
        v = cos(t - 7.0 * u * pow(a, i + 1.0)) - 5.0 * u;

        float angle = i + 1.0 + 0.02 * t;
        float s = sin(angle);
        float c = cos(angle);
        mat2 rotate = mat2(c, -s, s, c);

        u *= rotate;
        u += stanh(40.0 * dot(u, u) * cos(100.0 * u.yx + t)) / 200.0
           + 0.2 * a * u
           + cos(4.0 / exp(dot(o, o) / 100.0) + t) / 300.0;

        float wave = length((1.0 + (i + 1.0) * dot(v, v)) * sin(1.5 * u / (0.5 - dot(u, u)) - 9.0 * u.yx + t));
        o += (1.0 + cos(z + t)) / max(wave, 0.001);
    }

    o = 25.6 / (min(o, vec4(13.0)) + 164.0 / max(o, vec4(0.001))) - dot(u, u) / 250.0;

    vec3 color = clamp(o.rgb * tint.rgb, vec3(0.0), vec3(1.0));
    float alpha = tint.a * shapeAlpha;

    if (alpha <= 0.001) {
        discard;
    }

    OutColor = vec4(color, alpha);
}
