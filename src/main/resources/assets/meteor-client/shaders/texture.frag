#version 330 core

in vec2 v_Uv;
in vec4 v_Color;

out vec4 FragColor;

uniform sampler2D u_Texture;

layout(std140) uniform TextureData {
    vec2 u_Size;
    vec4 u_Radius;
    float u_Smoothness;
};

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

void main() {
    float alpha = ralpha(u_Size, v_Uv, u_Radius, u_Smoothness);
    vec4 color = vec4(1.0, 1.0, 1.0, alpha) * texture(u_Texture, v_Uv) * v_Color;

    if (color.a == 0.0) { // alpha test
        discard;
    }

    FragColor = color;
}