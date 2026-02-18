#version 330 core

layout (location = 0) in vec2 a_Pos;

out vec2 v_TexCoord;

void main() {
    gl_Position = vec4(a_Pos, 0.0, 1.0);
    v_TexCoord = a_Pos * 0.5 + 0.5;
}