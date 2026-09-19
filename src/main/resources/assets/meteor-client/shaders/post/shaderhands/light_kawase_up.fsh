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
    vec2 suv = texCoord / 2.0;
    vec2 halfpixel = Resolution / 2.0 * Offset;
    vec3 sum = vec3(0.0);
    sum += texture(Image, suv + vec2(-halfpixel.x * 2.0, 0.0)).rgb;
    sum += texture(Image, suv + vec2(-halfpixel.x, halfpixel.y)).rgb * 2.0;
    sum += texture(Image, suv + vec2(0.0, halfpixel.y * 2.0)).rgb;
    sum += texture(Image, suv + vec2(halfpixel.x, halfpixel.y)).rgb * 2.0;
    sum += texture(Image, suv + vec2(halfpixel.x * 2.0, 0.0)).rgb;
    sum += texture(Image, suv + vec2(halfpixel.x, -halfpixel.y)).rgb * 2.0;
    sum += texture(Image, suv + vec2(0.0, -halfpixel.y * 2.0)).rgb;
    sum += texture(Image, suv + vec2(-halfpixel.x, -halfpixel.y)).rgb * 2.0;
    fragColor = vec4(sum / 12.0, 1.0);
}
