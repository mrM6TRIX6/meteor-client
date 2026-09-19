#version 150

layout(std140) uniform BoundsWrite {
    vec4 Bbox;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = Bbox;
}
