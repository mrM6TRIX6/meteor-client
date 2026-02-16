#version 330 core

layout(std140) uniform BlurData {
    vec2 u_TexelSize; // resoulution
    float u_Offset;
};

uniform sampler2D u_Texture;

in vec2 v_TexCoord;
out vec4 FragColor;

void main() {
    vec2 offset = u_TexelSize * u_Offset;

    vec4 c0 = texture(u_Texture, v_TexCoord + vec2(-offset.x, -offset.y));
    vec4 c1 = texture(u_Texture, v_TexCoord + vec2( offset.x, -offset.y));
    vec4 c2 = texture(u_Texture, v_TexCoord + vec2(-offset.x,  offset.y));
    vec4 c3 = texture(u_Texture, v_TexCoord + vec2( offset.x,  offset.y));

    FragColor = (c0 + c1 + c2 + c3) * 0.25;
}