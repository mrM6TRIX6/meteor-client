#version 330 core

in vec4 v_Color;
in vec2 v_Uv;
in vec2 v_Size;
in vec4 v_Radius;
in float v_Smoothness;

out vec4 color;

float rdist(vec2 pos, vec2 size, vec4 radius) {
    radius.xy = (pos.x > 0.0) ? radius.xy : radius.wz;
    radius.x  = (pos.y > 0.0) ? radius.x : radius.y;

    vec2 v = abs(pos) - size + radius.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius.x;
}

float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) {
    vec2 center = size * 0.5;
    float dist = rdist(center - (coord * size), center - 1.0, radius);
    return 1.0 - smoothstep(1.0 - smoothness, 1.0, dist);
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

void main() {
    float alpha = ralpha(v_Size, v_Uv, v_Radius, v_Smoothness);
    vec4 color1 = vec4(v_Color.rgb, v_Color.a * alpha);

    if (color1.a == 0.0) { // alpha test
        discard;
    }

    color = color1;
}
