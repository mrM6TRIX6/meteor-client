#version 330 core

layout (location = 0) in vec2 a_Pos;
layout (location = 1) in vec2 a_Uv;
layout (location = 2) in vec4 a_Color;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

out vec2 v_Uv;
out vec4 v_Color;

void main() {
    gl_Position = u_Proj * u_ModelView * vec4(a_Pos, 0.0, 1.0);

    v_Uv = a_Uv;
    v_Color = a_Color;
}