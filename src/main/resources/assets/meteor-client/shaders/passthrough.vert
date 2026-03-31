#version 330 core

precision lowp float;

layout (location = 0) in vec2 a_Pos;

out vec2 v_Uv;

void main() {
    gl_Position = vec4(a_Pos, 0, 1);
    v_Uv =  a_Pos * 0.5 + 0.5;
}
