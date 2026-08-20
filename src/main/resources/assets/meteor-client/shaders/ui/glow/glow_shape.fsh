#version 150

in vec2 FragCoord;

layout(std140) uniform GlowParamsArray {
    vec4 params[1120];
};

out vec4 OutColor;

float rdist(vec2 pos, vec2 halfSize, vec4 radius) {
    float cornerRadius;
    if (pos.x > 0.0) {
        cornerRadius = (pos.y > 0.0) ? radius.x : radius.w;
    } else {
        cornerRadius = (pos.y > 0.0) ? radius.y : radius.z;
    }
    float r = max(cornerRadius, 0.0);
    vec2 q = abs(pos) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

void main() {
    vec4 radius   = max(params[0], vec4(0.0));
    vec4 sizeData = params[1];
    vec4 colorVec = params[2];

    vec2 rectSize = max(sizeData.xy, vec2(1.0));
    float pad = max(sizeData.z, 0.0);
    float smoothness = max(sizeData.w, 0.5);

    vec2 targetSize = rectSize + vec2(pad * 2.0);
    vec2 pixel = FragCoord * targetSize;

    float spanCount = params[5].x;
    float innerRadius = params[5].y;
    float leftAligned = params[5].z;
    float bottomAnchored = params[5].w;

    float dist;
    if (spanCount >= 0.5) {
        float minDist = 1e9;
        int count = int(spanCount + 0.5);
        for (int i = 0; i < 64; i++) {
            if (i >= count) break;
            vec4 span = params[6 + i];
            vec4 seg = vec4(span.x, span.z, span.y - span.x, span.w - span.z);

            vec2 fullHalf = max(seg.zw * 0.5, vec2(0.001));
            vec2 segCenter = seg.xy + fullHalf;
            vec2 halfSize = max(fullHalf - 1.0, vec2(0.001));
            float rad = min(innerRadius, min(halfSize.x, halfSize.y));
            vec2 center = pixel - segCenter;
            center.y = -center.y;
            float d = rdist(center, halfSize, vec4(rad));
            if (d < minDist) minDist = d;
        }
        dist = minDist;
    } else {
        vec2 pos = pixel - targetSize * 0.5;
        vec2 halfSize = max(rectSize * 0.5 - 1.0, vec2(0.0));
        dist = rdist(pos, halfSize, radius);
    }

    float a = 1.0 - smoothstep(-smoothness, 0.0, dist);
    if (a < 0.001) discard;

    vec4 colorTL = params[78];
    vec4 colorTR = params[79];
    vec4 colorBR = params[80];
    vec4 colorBL = params[81];

    vec2 t = clamp((pixel - vec2(pad)) / rectSize, 0.0, 1.0);
    vec3 rgb = mix(mix(colorBR.rgb, colorBL.rgb, t.x), mix(colorTR.rgb, colorTL.rgb, t.x), t.y);
    float cornerAlpha = mix(mix(colorBR.a, colorBL.a, t.x), mix(colorTR.a, colorTL.a, t.x), t.y);

    OutColor = vec4(rgb, a * cornerAlpha);
}
