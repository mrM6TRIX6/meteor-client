#version 150

uniform sampler2D Sampler0;

layout(std140) uniform FogBlurData {
    vec4 BlurData;
    vec4 FogData;
    vec4 Reserved;
};

in vec2 texCoord;
out vec4 fragColor;

vec3 sampleScene(vec2 uv, vec2 axisStep) {
    vec3 color = texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).rgb * 0.2270270270;
    color += texture(Sampler0, clamp(uv + axisStep * 1.3846153846, vec2(0.0), vec2(1.0))).rgb * 0.3162162162;
    color += texture(Sampler0, clamp(uv - axisStep * 1.3846153846, vec2(0.0), vec2(1.0))).rgb * 0.3162162162;
    color += texture(Sampler0, clamp(uv + axisStep * 3.2307692308, vec2(0.0), vec2(1.0))).rgb * 0.0702702703;
    color += texture(Sampler0, clamp(uv - axisStep * 3.2307692308, vec2(0.0), vec2(1.0))).rgb * 0.0702702703;
    return color;
}

void main() {
    vec2 axisStep = BlurData.xy * BlurData.z;
    fragColor = vec4(sampleScene(texCoord, axisStep), 1.0);
}
