float rdist(vec2 pos, vec2 size, vec4 radius) {
    float cornerRadius;
    if (pos.x > 0.0) {
        cornerRadius = (pos.y > 0.0) ? radius.x : radius.w;
    } else {
        cornerRadius = (pos.y > 0.0) ? radius.y : radius.z;
    }

    vec2 v = abs(pos) - size + cornerRadius;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - cornerRadius;
}

float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) {
    vec2 center = size * 0.5;
    float feather = max(smoothness, 0.001);
    float dist = rdist(center - (coord * size), max(center - 1.0, vec2(0.0)), max(radius, vec4(0.0)));
    return 1.0 - smoothstep(1.0 - feather, 1.0, dist);
}

const vec2[4] RECT_VERTICES_COORDS = vec2[] (
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

vec2 rvertexcoord(int id) {
    return RECT_VERTICES_COORDS[id % 4];
}
