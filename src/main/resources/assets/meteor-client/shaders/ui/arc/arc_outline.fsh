#version 150

in vec2 FragCoord;
flat in int QuadIndex;

layout(std140) uniform ArcOutlineParamsArray {
    vec4 params[1024];
};

out vec4 OutColor;

void main() {
    int base = QuadIndex * 4;
    vec4 arc = params[base];
    vec4 outline = params[base + 1];
    vec4 fillColor = params[base + 2];
    vec4 outlineColor = params[base + 3];

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
    float outlineThickness = outline.x;
    vec2 endPoint = vec2(cos(halfArc), sin(halfArc)) * midRadius;
    vec2 tangent = vec2(-sin(halfArc), cos(halfArc));

    float radialDist = abs(length(q) - midRadius) - halfThickness;
    float capDist = dot(q - endPoint, tangent);
    float dist = max(radialDist, capDist);
    float edge = max(fwidth(length(q)), 0.75);

    float arcAlpha = 1.0 - smoothstep(-edge, edge, dist);

    if (arcAlpha <= 0.0) {
        discard;
    }

    float fillHalfThickness = max(halfThickness - outlineThickness, 0.0);
    vec2 fillEndPoint = endPoint - tangent * outlineThickness;
    float fillRadialDist = abs(length(q) - midRadius) - fillHalfThickness;
    float fillCapDist = dot(q - fillEndPoint, tangent);
    float fillDist = max(fillRadialDist, fillCapDist);
    float fillAlpha = 1.0 - smoothstep(-edge, edge, fillDist);

    vec4 fill = fillColor * fillAlpha;
    vec4 outlineResult = outlineColor * (1.0 - fillAlpha);
    vec4 finalColor = fill + outlineResult;

    OutColor = vec4(finalColor.rgb, finalColor.a * arcAlpha);
}
