#version 150

uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform FogBlurData {
    vec4 BlurData;
    vec4 FogData;
    vec4 Reserved;
};

in vec2 texCoord;
out vec4 fragColor;

float linearizeDepth(float depth, float nearPlane, float farPlane) {
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - depth * (farPlane - nearPlane));
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    float linearDistance = linearizeDepth(depth, FogData.x, FogData.y) / FogData.y;
    float fogMask = smoothstep(FogData.z, FogData.w, linearDistance);
    vec3 scene = texture(SceneSampler, texCoord).rgb;
    vec3 blurred = texture(BlurSampler, texCoord).rgb;
    fragColor = vec4(mix(scene, blurred, clamp(fogMask * BlurData.w, 0.0, 1.0)), 1.0);
}
