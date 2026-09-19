#version 150

uniform sampler2D Image;

layout(std140) uniform KawaseParams {
    vec2 Resolution;
    float Offset;
    float Pad0;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 suv = texCoord * 2.0;
    vec2 halfpixel = Resolution * 2.0 * Offset;
    vec3 sum = texture(Image, suv).rgb * 4.0;
    sum += texture(Image, suv - halfpixel).rgb;
    sum += texture(Image, suv + halfpixel).rgb;
    sum += texture(Image, suv + vec2(halfpixel.x, -halfpixel.y)).rgb;
    sum += texture(Image, suv - vec2(halfpixel.x, -halfpixel.y)).rgb;
    fragColor = vec4(sum / 8.0, 1.0);
}
