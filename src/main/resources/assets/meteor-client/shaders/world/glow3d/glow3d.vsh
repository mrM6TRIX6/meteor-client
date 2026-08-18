#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
};

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec4 inColor;
layout(location = 2) in vec2 inUV;

out vec4 vColor;
out vec2 vUV;

void main() {
    vColor = inColor;
    vUV = inUV;
    gl_Position = uProjection * vec4(inPosition, 1.0);
}
