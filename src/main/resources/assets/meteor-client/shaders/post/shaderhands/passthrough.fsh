#version 150

uniform sampler2D HandTex;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 c = texture(HandTex, texCoord);

    if (c.a <= 0.001) {
        discard;
    }

    fragColor = c;
}
