#version 330 core

layout (location = 0) in vec2 a_Pos;
layout (location = 1) in vec4 a_Color;
layout (location = 2) in vec2 a_Uv;
layout (location = 3) in vec2 a_Size;
layout (location = 4) in vec4 a_Radius;
layout (location = 5) in float a_Smoothness;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

out vec4 v_Color;
out vec2 v_Uv;
out vec2 v_Size;
out vec4 v_Radius;
out float v_Smoothness;

void main() {
    gl_Position = u_Proj * u_ModelView * vec4(a_Pos, 0.0, 1.0);

    v_Color = a_Color;
    v_Uv = a_Uv;
    v_Size = a_Size;
    v_Radius = a_Radius;
    v_Smoothness = a_Smoothness;
}