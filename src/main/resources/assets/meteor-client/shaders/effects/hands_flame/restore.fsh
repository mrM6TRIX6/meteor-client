#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D SceneSampler;

void main() {
    fragColor = texture(SceneSampler, texCoord);
}
