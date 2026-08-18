#version 150

in vec2 FragCoord;
flat in int QuadIndex;

layout(std140) uniform ArcParamsArray {
    vec4 params[2560];
};

out vec4 OutColor;

vec4 sampleColor(vec2 uv, int base) {
    uv = clamp(uv, 0.0, 1.0);

    float u = uv.x;
    float v = uv.y;

    float u0 = (1.0 - u) * (1.0 - u);
    float u1 = 2.0 * u * (1.0 - u);
    float u2 = u * u;

    float v0 = (1.0 - v) * (1.0 - v);
    float v1 = 2.0 * v * (1.0 - v);
    float v2 = v * v;

    vec4 result = vec4(0.0);
    result += params[base + 1] * u0 * v0;
    result += params[base + 2] * u1 * v0;
    result += params[base + 3] * u2 * v0;
    result += params[base + 4] * u0 * v1;
    result += params[base + 5] * u1 * v1;
    result += params[base + 6] * u2 * v1;
    result += params[base + 7] * u0 * v2;
    result += params[base + 8] * u1 * v2;
    result += params[base + 9] * u2 * v2;

    return result;
}

void main() {
    int base = QuadIndex * 10;
    vec4 arc = params[base];
    vec2 coord = clamp(FragCoord, vec2(0.0), vec2(1.0));
    vec2 size = vec2(max(arc.x, 1.0));
    vec2 p = (coord - vec2(0.5)) * size;

    float rotation = radians(arc.w);
    float c = cos(rotation);
    float s = sin(rotation);
    vec2 q = vec2(
        p.x * c + p.y * s,
        -p.x * s + p.y * c
    );
    q.y = abs(q.y);

    float halfArc = radians(arc.z) * 0.5;
    float outerRadius = arc.x * 0.5;
    float thickness = arc.y;
    float halfThickness = thickness * 0.5;
    float midRadius = outerRadius - halfThickness;
    vec2 endPoint = vec2(cos(halfArc), sin(halfArc)) * midRadius;
    vec2 tangent = vec2(-sin(halfArc), cos(halfArc));

    float radialDist = abs(length(q) - midRadius) - halfThickness;
    float capDist = dot(q - endPoint, tangent);
    float dist = max(radialDist, capDist);

    float aaWidth = max(0.625, fwidth(length(q)));
    float alpha = 1.0 - smoothstep(-aaWidth, aaWidth, dist);

    if (alpha <= 0.001) {
        discard;
    }

    vec4 color = sampleColor(coord, base);
    OutColor = vec4(color.rgb, color.a * alpha);
}
