#version 150

uniform sampler2D MaskTex;
uniform sampler2D SplitTex;

layout(std140) uniform InitConfig {
    vec4 Range;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float a = texture(MaskTex, texCoord).a;
    bool inside = a > 0.02 && texCoord.x >= Range.x && texCoord.x < Range.y;

    int mode = int(Range.z + 0.5);
    if (inside && mode != 0) {
        vec4 u = texture(SplitTex, vec2(0.5));
        float center = (u.x + u.z) * 0.5;
        if (mode == 1) {
            inside = texCoord.x < center;
        } else {
            inside = texCoord.x >= center;
        }
    }

    if (inside) {
        fragColor = vec4(texCoord, texCoord);
    } else {
        fragColor = vec4(1.0, 1.0, 0.0, 0.0);
    }
}
