#version 330 core

layout(std140) uniform BlurData {
    vec2 u_TexelSize;
    float u_Offset;
};

uniform sampler2D u_Texture;

in vec2 v_TexCoord;
out vec4 FragColor;

void main() {
    vec2 offset = u_TexelSize * u_Offset;
    vec4 center = texture(u_Texture, v_TexCoord);
    vec4 sum = center * 0.5;

    sum += texture(u_Texture, v_TexCoord + vec2(-offset.x, 0.0)) * 0.125;
    sum += texture(u_Texture, v_TexCoord + vec2( offset.x, 0.0)) * 0.125;
    sum += texture(u_Texture, v_TexCoord + vec2(0.0, -offset.y)) * 0.125;
    sum += texture(u_Texture, v_TexCoord + vec2(0.0,  offset.y)) * 0.125;

    FragColor = sum;
}