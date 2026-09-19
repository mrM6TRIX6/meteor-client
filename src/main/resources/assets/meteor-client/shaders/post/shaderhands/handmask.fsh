#version 150

uniform sampler2D HandTex;
uniform sampler2D HandDepth;

in vec2 texCoord;
out vec4 fragColor;

const float NEAR = 0.05;
const float FAR = 100.0;

const float DMIN = 0.2;
const float DMAX = 2.0;

void main() {
    float coverage = texture(HandTex, texCoord).a;
    float d = texture(HandDepth, texCoord).r;
    float zndc = d * 2.0 - 1.0;
    float eye = (2.0 * NEAR * FAR) / (FAR + NEAR - zndc * (FAR - NEAR));
    float enc = clamp((eye - DMIN) / (DMAX - DMIN), 0.0, 1.0);
    fragColor = vec4(enc, 0.0, 0.0, coverage);
}
