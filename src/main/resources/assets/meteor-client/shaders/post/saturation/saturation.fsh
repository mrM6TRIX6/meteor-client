#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

layout(std140) uniform SaturationData {
    float saturation;
};

vec3 adjustSaturation(vec3 color, float saturationValue) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    return mix(vec3(gray), color, saturationValue);
}

void main() {
    vec4 scene = texture(Sampler0, texCoord);
    fragColor = vec4(adjustSaturation(scene.rgb, saturation), scene.a);
}
