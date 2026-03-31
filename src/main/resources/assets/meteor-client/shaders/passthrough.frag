#version 330 core

precision lowp float;

in vec2 v_Uv;

out vec4 FragColor;

uniform sampler2D u_Texture;

void main() {
    FragColor = texture(u_Texture, v_Uv);
}
