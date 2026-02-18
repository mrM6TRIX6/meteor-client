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

    vec4 sum = texture(u_Texture, v_TexCoord + vec2(-offset.x, -offset.y));
    sum += texture(u_Texture, v_TexCoord + vec2( offset.x, -offset.y));
    sum += texture(u_Texture, v_TexCoord + vec2(-offset.x,  offset.y));
    sum += texture(u_Texture, v_TexCoord + vec2( offset.x,  offset.y));

    FragColor = sum * 0.25;
}